package tech.astro.liftoff.catalog.service

import jakarta.persistence.criteria.JoinType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.catalog.dto.AdminProductResponse
import tech.astro.liftoff.catalog.dto.ProductDetail
import tech.astro.liftoff.catalog.dto.ProductSort
import tech.astro.liftoff.catalog.dto.ProductStatusFilter
import tech.astro.liftoff.catalog.dto.ProductSummary
import tech.astro.liftoff.catalog.dto.SaveProductRequest
import tech.astro.liftoff.catalog.dto.ShopHome
import tech.astro.liftoff.catalog.model.Category
import tech.astro.liftoff.catalog.model.Product
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.ConflictException
import tech.astro.liftoff.shared.error.FieldValidationException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.persistence.LIKE_ESCAPE
import tech.astro.liftoff.shared.persistence.containsPattern
import tech.astro.liftoff.shared.web.Paging
import tech.astro.liftoff.storage.StorageObjectsOrphaned
import tech.astro.liftoff.user.model.User
import java.util.UUID

@Service
class ProductService(
    private val products: ProductRepository,
    private val categories: CategoryService,
    private val mapper: CatalogMapper,
    private val audit: AuditService,
    private val events: ApplicationEventPublisher,
    private val props: AppProperties,
) {
    // ------------------------------------------------------------------ store (public)

    @Transactional(readOnly = true)
    fun home(): ShopHome = ShopHome(
        featured = products.findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc().map(mapper::summary),
        newest = products.findAll(isActive(), PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))).content.map(mapper::summary),
        categories = categories.list(),
    )

    /** Active products only. [categorySlug] narrows to one category; [query] searches names and descriptions. */
    @Transactional(readOnly = true)
    fun search(query: String?, categorySlug: String?, sort: ProductSort, page: Int, size: Int): Page<ProductSummary> {
        var spec = isActive()
        if (!categorySlug.isNullOrBlank()) {
            spec = spec.and { root, _, cb -> cb.equal(root.join<Product, Category>("category", JoinType.INNER).get<String>("slug"), categorySlug) }
        }
        if (!query.isNullOrBlank()) spec = spec.and(matches(query))
        return products.findAll(spec, Paging.of(page, size, sort.toSort())).map(mapper::summary)
    }

    @Transactional(readOnly = true)
    fun get(slug: String): ProductDetail =
        products.findBySlugAndActiveTrue(slug)?.let(mapper::detail) ?: throw notFound()

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    fun adminSearch(query: String?, categoryId: UUID?, status: ProductStatusFilter?, pageable: Pageable): Page<AdminProductResponse> {
        var spec = Specification.unrestricted<Product>()
        if (categoryId != null) {
            spec = spec.and { root, _, cb -> cb.equal(root.get<Category>("category").get<UUID>("id"), categoryId) }
        }
        val low = props.shop.lowStockThreshold
        when (status) {
            ProductStatusFilter.ACTIVE -> spec = spec.and { root, _, cb -> cb.isTrue(root.get("active")) }
            ProductStatusFilter.INACTIVE -> spec = spec.and { root, _, cb -> cb.isFalse(root.get("active")) }
            ProductStatusFilter.LOW_STOCK -> spec = spec.and { root, _, cb -> cb.and(cb.isTrue(root.get("active")), cb.between(root.get<Int>("stock"), 1, low)) }
            ProductStatusFilter.OUT_OF_STOCK -> spec = spec.and { root, _, cb -> cb.equal(root.get<Int>("stock"), 0) }
            null -> Unit
        }
        if (!query.isNullOrBlank()) {
            val pattern = containsPattern(query)
            spec = spec.and(matches(query).or { root, _, cb -> cb.like(cb.lower(root.get("sku")), pattern, LIKE_ESCAPE) })
        }
        return products.findAll(spec, pageable).map(mapper::admin)
    }

    @Transactional(readOnly = true)
    fun adminGet(id: UUID): AdminProductResponse = mapper.admin(load(id))

    @Transactional
    fun create(actor: User, request: SaveProductRequest): AdminProductResponse {
        val product = Product(
            slug = resolveSlug(request, current = null),
            name = request.name.trim(),
            priceCents = request.priceCents,
        )
        apply(product, request)
        products.saveAndFlush(product)
        audit.record(AuditActions.ADMIN_PRODUCT_CREATED, actor = actor, targetType = "product", targetId = product.id.toString(), targetLabel = product.name)
        return mapper.admin(product)
    }

    @Transactional
    fun update(actor: User, id: UUID, request: SaveProductRequest): AdminProductResponse {
        val product = load(id)
        val version = request.version ?: throw FieldValidationException("version", "NotNull", "must not be null")
        if (version != product.version) {
            throw ConflictException("conflict.stale", "This product was changed elsewhere. Reload and try again.")
        }
        product.slug = resolveSlug(request, current = product)
        apply(product, request)
        products.saveAndFlush(product)
        audit.record(AuditActions.ADMIN_PRODUCT_UPDATED, actor = actor, targetType = "product", targetId = product.id.toString(), targetLabel = product.name)
        return mapper.admin(product)
    }

    /** Past orders keep their copy of the name and price. To only hide a product, make it inactive. */
    @Transactional
    fun delete(actor: User, id: UUID) {
        val product = load(id)
        product.imageKey?.let { events.publishEvent(StorageObjectsOrphaned(listOf(it))) }
        products.delete(product)
        audit.record(AuditActions.ADMIN_PRODUCT_DELETED, actor = actor, targetType = "product", targetId = product.id.toString(), targetLabel = product.name)
    }

    fun load(id: UUID): Product = products.findWithCategoryById(id) ?: throw notFound()

    private fun apply(product: Product, request: SaveProductRequest) {
        val compareAt = request.compareAtPriceCents
        if (compareAt != null && compareAt <= request.priceCents) {
            throw FieldValidationException("compareAtPriceCents", "CompareAtPrice", "must be higher than the price")
        }
        val sku = request.sku.trim().ifEmpty { null }
        if (sku != null && sku != product.sku && products.existsBySku(sku)) {
            throw FieldValidationException("sku", "Taken", "is already used by another product")
        }
        product.name = request.name.trim()
        product.description = request.description.trim()
        product.priceCents = request.priceCents
        product.compareAtPriceCents = compareAt
        product.sku = sku
        product.stock = request.stock
        product.active = request.active
        product.featured = request.featured
        product.category = request.categoryId?.let(categories::load)
    }

    private fun resolveSlug(request: SaveProductRequest, current: Product?): String {
        val wanted = request.slug.trim()
        if (wanted.isEmpty()) {
            return current?.slug ?: Slugs.unique(Slugs.from(request.name)) { products.existsBySlug(it) }
        }
        if (wanted != current?.slug && products.existsBySlug(wanted)) {
            throw FieldValidationException("slug", "Taken", "is already used by another product")
        }
        return wanted
    }

    private fun isActive() = Specification<Product> { root, _, cb -> cb.isTrue(root.get("active")) }

    private fun matches(query: String): Specification<Product> {
        val pattern = containsPattern(query)
        return Specification { root, _, cb ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE),
            )
        }
    }

    private fun ProductSort.toSort(): Sort = when (this) {
        ProductSort.NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt")
        ProductSort.PRICE_ASC -> Sort.by(Sort.Order.asc("priceCents"), Sort.Order.asc("name"))
        ProductSort.PRICE_DESC -> Sort.by(Sort.Order.desc("priceCents"), Sort.Order.asc("name"))
        ProductSort.NAME -> Sort.by(Sort.Direction.ASC, "name")
    }

    private fun notFound() = NotFoundException("product.not_found", "Product not found")
}
