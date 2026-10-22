package tech.astro.liftoff.catalog.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.catalog.dto.CategoryResponse
import tech.astro.liftoff.catalog.dto.SaveCategoryRequest
import tech.astro.liftoff.catalog.model.Category
import tech.astro.liftoff.catalog.repository.CategoryRepository
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.shared.error.FieldValidationException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.user.model.User
import java.util.UUID

@Service
class CategoryService(
    private val categories: CategoryRepository,
    private val products: ProductRepository,
    private val mapper: CatalogMapper,
    private val audit: AuditService,
) {
    /** Store menu: categories with their number of visible products. */
    @Transactional(readOnly = true)
    fun list(includeInactiveProducts: Boolean = false): List<CategoryResponse> {
        val counts = products.countByCategory(activeOnly = !includeInactiveProducts).associate { it.categoryId to it.count }
        return categories.findAllByOrderByPositionAscNameAsc().map { mapper.category(it, counts[it.id] ?: 0) }
    }

    @Transactional
    fun create(actor: User, request: SaveCategoryRequest): CategoryResponse {
        val slug = resolveSlug(request, current = null)
        val category = Category(slug, request.name.trim(), request.description.trim(), request.position)
        category.seoTitle = request.seoTitle.trim().ifEmpty { null }
        category.seoDescription = request.seoDescription.trim().ifEmpty { null }
        categories.saveAndFlush(category)
        audit.record(AuditActions.ADMIN_CATEGORY_CREATED, actor = actor, targetType = "category", targetId = category.id.toString(), targetLabel = category.name)
        return mapper.category(category, 0)
    }

    @Transactional
    fun update(actor: User, id: UUID, request: SaveCategoryRequest): CategoryResponse {
        val category = load(id)
        category.slug = resolveSlug(request, current = category)
        category.name = request.name.trim()
        category.description = request.description.trim()
        category.position = request.position
        category.seoTitle = request.seoTitle.trim().ifEmpty { null }
        category.seoDescription = request.seoDescription.trim().ifEmpty { null }
        categories.saveAndFlush(category)
        audit.record(AuditActions.ADMIN_CATEGORY_UPDATED, actor = actor, targetType = "category", targetId = category.id.toString(), targetLabel = category.name)
        val count = products.countByCategory(activeOnly = false).firstOrNull { it.categoryId == id }?.count ?: 0
        return mapper.category(category, count)
    }

    /** Its products stay, without a category. */
    @Transactional
    fun delete(actor: User, id: UUID) {
        val category = load(id)
        categories.delete(category)
        audit.record(AuditActions.ADMIN_CATEGORY_DELETED, actor = actor, targetType = "category", targetId = category.id.toString(), targetLabel = category.name)
    }

    /** A category page of the store. */
    @Transactional(readOnly = true)
    fun get(slug: String): CategoryResponse {
        val category = categories.findBySlug(slug) ?: throw NotFoundException("category.not_found", "Category not found")
        val count = products.countByCategory(activeOnly = true).firstOrNull { it.categoryId == category.id }?.count ?: 0
        return mapper.category(category, count)
    }

    fun load(id: UUID): Category = categories.findById(id).orElseThrow { NotFoundException("category.not_found", "Category not found") }

    private fun resolveSlug(request: SaveCategoryRequest, current: Category?): String {
        val wanted = request.slug.trim()
        if (wanted.isEmpty()) {
            return current?.slug ?: Slugs.unique(Slugs.from(request.name)) { categories.existsBySlug(it) }
        }
        if (wanted != current?.slug && categories.existsBySlug(wanted)) {
            throw FieldValidationException("slug", "Taken", "is already used by another category")
        }
        return wanted
    }
}
