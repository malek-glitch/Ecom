package tech.astro.liftoff.catalog.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

const val SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$"

enum class ProductSort { NEWEST, PRICE_ASC, PRICE_DESC, NAME }

/** Admin product list filter. */
enum class ProductStatusFilter { ACTIVE, INACTIVE, LOW_STOCK, OUT_OF_STOCK }

data class CategoryRef(val id: UUID, val slug: String, val name: String)

data class CategoryResponse(
    val id: UUID,
    val slug: String,
    val name: String,
    val description: String,
    val position: Int,
    /** Visible products for the public list; every product for admins. */
    val productCount: Long,
    val version: Long,
)

/** A product card in the store. */
data class ProductSummary(
    val id: UUID,
    val slug: String,
    val name: String,
    val priceCents: Long,
    val compareAtPriceCents: Long?,
    val imageUrl: String?,
    val stock: Int,
    val category: CategoryRef?,
)

data class ProductDetail(
    val id: UUID,
    val slug: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val compareAtPriceCents: Long?,
    val sku: String?,
    val imageUrl: String?,
    val stock: Int,
    val category: CategoryRef?,
)

data class AdminProductResponse(
    val id: UUID,
    val slug: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val compareAtPriceCents: Long?,
    val sku: String?,
    val stock: Int,
    val active: Boolean,
    val featured: Boolean,
    val category: CategoryRef?,
    val imageUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Send it back on update: a stale version gets 409 `conflict.stale`. */
    val version: Long,
)

data class SaveCategoryRequest(
    @field:NotBlank @field:Size(max = 120)
    val name: String = "",
    /** Empty: generated from the name. */
    @field:Size(max = 80) @field:Pattern(regexp = "^$|$SLUG_PATTERN")
    val slug: String = "",
    @field:Size(max = 2000)
    val description: String = "",
    @field:Min(0) @field:Max(10_000)
    val position: Int = 0,
)

data class SaveProductRequest(
    @field:NotBlank @field:Size(max = 200)
    val name: String = "",
    /** Empty: generated from the name. */
    @field:Size(max = 120) @field:Pattern(regexp = "^$|$SLUG_PATTERN")
    val slug: String = "",
    @field:Size(max = 20_000)
    val description: String = "",
    @field:PositiveOrZero @field:Max(100_000_000_00)
    val priceCents: Long = 0,
    @field:PositiveOrZero @field:Max(100_000_000_00)
    val compareAtPriceCents: Long? = null,
    @field:Size(max = 64)
    val sku: String = "",
    @field:PositiveOrZero @field:Max(1_000_000)
    val stock: Int = 0,
    val active: Boolean = true,
    val featured: Boolean = false,
    val categoryId: UUID? = null,
    /** Required on update (optimistic locking), ignored on create. */
    val version: Long? = null,
)

data class ShopHome(
    val featured: List<ProductSummary>,
    val newest: List<ProductSummary>,
    val categories: List<CategoryResponse>,
)
