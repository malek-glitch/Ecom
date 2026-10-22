package tech.astro.liftoff.catalog.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import tech.astro.liftoff.inventory.dto.StockStatus
import java.time.Instant
import java.util.UUID

const val SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$"

/** RELEVANCE only makes sense with a search query; without one the store falls back to NEWEST. */
enum class ProductSort { RELEVANCE, NEWEST, PRICE_ASC, PRICE_DESC, NAME }

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
    val seoTitle: String?,
    val seoDescription: String?,
    val updatedAt: Instant,
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
    /** Units left; null when stock isn't tracked (always available). */
    val stock: Int?,
    /** At or below the product's low-stock level ("only 3 left"). */
    val lowStock: Boolean,
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
    /** Units left; null when stock isn't tracked (always available). */
    val stock: Int?,
    val lowStock: Boolean,
    val category: CategoryRef?,
    val seoTitle: String?,
    val seoDescription: String?,
    val updatedAt: Instant,
    /** A few other products to suggest: same category first. */
    val related: List<ProductSummary>,
)

data class AdminProductResponse(
    val id: UUID,
    val slug: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val compareAtPriceCents: Long?,
    val sku: String?,
    /** The stored stock, even when it isn't tracked. Change it through the inventory endpoints. */
    val stock: Int,
    val trackInventory: Boolean,
    val stockStatus: StockStatus,
    val active: Boolean,
    val featured: Boolean,
    val category: CategoryRef?,
    val imageUrl: String?,
    val seoTitle: String?,
    val seoDescription: String?,
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
    @field:Size(max = 70)
    val seoTitle: String = "",
    @field:Size(max = 200)
    val seoDescription: String = "",
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
    /** Initial stock, on create only. Later changes go through the inventory (adjustments keep a history). */
    @field:PositiveOrZero @field:Max(1_000_000)
    val stock: Int = 0,
    val active: Boolean = true,
    val featured: Boolean = false,
    val categoryId: UUID? = null,
    @field:Size(max = 70)
    val seoTitle: String = "",
    @field:Size(max = 200)
    val seoDescription: String = "",
    /** Required on update (optimistic locking), ignored on create. */
    val version: Long? = null,
)

data class ShopHome(
    val featured: List<ProductSummary>,
    val newest: List<ProductSummary>,
    val categories: List<CategoryResponse>,
)

// ---------------------------------------------------------------------- search

/** What the store search was asked for. Blank or absent values mean "no filter". */
data class ProductSearchCriteria(
    val query: String? = null,
    val categorySlug: String? = null,
    val minPriceCents: Long? = null,
    val maxPriceCents: Long? = null,
    val inStockOnly: Boolean = false,
    val sort: ProductSort? = null,
    val page: Int = 0,
    val size: Int = 24,
)

data class CategoryFacet(val slug: String, val name: String, val count: Long)

data class PriceRange(val minCents: Long, val maxCents: Long)

data class ProductSearchResponse(
    val items: List<ProductSummary>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
    /** The sort actually applied (RELEVANCE when searching, unless another was asked for). */
    val sort: ProductSort,
    /** Matching products per category, ignoring the category filter, so shoppers can switch. */
    val categories: List<CategoryFacet>,
    /** Cheapest and dearest matching product, ignoring the price filter; null when nothing matches. */
    val priceRange: PriceRange?,
    /** A corrected query that finds products, offered when this one finds none ("did you mean"). */
    val didYouMean: String?,
)

data class SearchSuggestions(
    val products: List<ProductSummary>,
    val categories: List<CategoryRef>,
    val totalProducts: Long,
    val didYouMean: String?,
)

data class SearchInsight(val query: String, val searches: Long, val lastResultCount: Int, val lastSearchedAt: Instant)

data class SearchInsights(
    /** Most searched queries over the last 30 days. */
    val top: List<SearchInsight>,
    /** Queries that last found nothing: products people want and can't find. */
    val withoutResults: List<SearchInsight>,
)

// ---------------------------------------------------------------------- sitemap

data class SitemapProduct(val slug: String, val updatedAt: Instant, val imageUrl: String?)

data class SitemapCategory(val slug: String, val updatedAt: Instant)

data class SitemapEntries(val products: List<SitemapProduct>, val categories: List<SitemapCategory>)
