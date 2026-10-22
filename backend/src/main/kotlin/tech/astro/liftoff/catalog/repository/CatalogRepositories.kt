package tech.astro.liftoff.catalog.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import tech.astro.liftoff.catalog.model.Category
import tech.astro.liftoff.catalog.model.Product
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findBySlug(slug: String): Category?

    fun existsBySlug(slug: String): Boolean

    fun findAllByOrderByPositionAscNameAsc(): List<Category>
}

/** Category id → number of products in it, for category listings. */
interface CategoryCount {
    val categoryId: UUID
    val count: Long
}

interface ProductRepository : JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    /** Loads the category with each product, so listings don't run one query per row. */
    @EntityGraph(attributePaths = ["category"])
    override fun findAll(spec: Specification<Product>, pageable: Pageable): Page<Product>

    @EntityGraph(attributePaths = ["category"])
    fun findBySlugAndActiveTrue(slug: String): Product?

    @EntityGraph(attributePaths = ["category"])
    fun findWithCategoryById(id: UUID): Product?

    @EntityGraph(attributePaths = ["category"])
    fun findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc(): List<Product>

    fun existsBySlug(slug: String): Boolean

    fun existsBySku(sku: String): Boolean

    fun findAllByIdIn(ids: Collection<UUID>): List<Product>

    @EntityGraph(attributePaths = ["category"])
    fun findWithCategoryByIdIn(ids: Collection<UUID>): List<Product>

    fun findAllByActiveTrueOrderByUpdatedAtDesc(): List<Product>

    @Query("select p.category.id as categoryId, count(p) as count from Product p where p.category is not null and (:activeOnly = false or p.active = true) group by p.category.id")
    fun countByCategory(activeOnly: Boolean): List<CategoryCount>

    fun countByActiveTrue(): Long

    fun countByActiveTrueAndStockLessThanEqual(threshold: Int): Long

    /**
     * Takes [quantity] items out of stock only if that many are left. Atomic in the database,
     * so concurrent checkouts can't oversell: 0 updated rows means "not enough stock".
     */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.active = true and p.stock >= :quantity")
    fun takeStock(id: UUID, quantity: Int): Int

    @Modifying
    @Query("update Product p set p.stock = p.stock + :quantity where p.id = :id")
    fun returnStock(id: UUID, quantity: Int): Int
}
