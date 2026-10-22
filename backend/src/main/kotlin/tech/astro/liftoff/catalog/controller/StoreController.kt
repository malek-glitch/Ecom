package tech.astro.liftoff.catalog.controller

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.catalog.dto.CategoryResponse
import tech.astro.liftoff.catalog.dto.ProductDetail
import tech.astro.liftoff.catalog.dto.ProductSort
import tech.astro.liftoff.catalog.dto.ProductSearchCriteria
import tech.astro.liftoff.catalog.dto.ProductSearchResponse
import tech.astro.liftoff.catalog.dto.SearchSuggestions
import tech.astro.liftoff.catalog.dto.SitemapEntries
import tech.astro.liftoff.catalog.dto.ShopHome
import tech.astro.liftoff.catalog.service.CategoryService
import tech.astro.liftoff.catalog.service.ProductSearchService
import tech.astro.liftoff.catalog.service.ProductService
import tech.astro.liftoff.shared.security.RateLimited

/** The public catalog: no account needed, server-rendered by the frontend for search engines. */
@RestController
@RequestMapping("/api/v1/public/shop")
@Tag(name = "Store")
@SecurityRequirements
class StoreController(
    private val products: ProductService,
    private val categories: CategoryService,
    private val search: ProductSearchService,
) {
    /** Featured and newest products plus the category menu, in one request for the home page. */
    @GetMapping("/home")
    fun home(): ShopHome = products.home()

    @GetMapping("/categories")
    fun categories(): List<CategoryResponse> = categories.list()

    /**
     * The catalog and the search, in one endpoint. With `q`, products are matched by words (as
     * prefixes), with typo tolerance and accent-insensitively, and sorted by relevance unless
     * `sort` says otherwise. `category` is a category slug; prices are in minor units.
     */
    @GetMapping("/products")
    fun products(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(defaultValue = "false") inStock: Boolean,
        @RequestParam(required = false) sort: ProductSort?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "24") size: Int,
    ): ProductSearchResponse = search.search(ProductSearchCriteria(q, category, minPrice, maxPrice, inStock, sort, page, size))

    /** As-you-type suggestions for the search box. */
    @GetMapping("/suggest")
    @RateLimited("search")
    fun suggest(@RequestParam(required = false) q: String?): SearchSuggestions = search.suggest(q)

    @GetMapping("/categories/{slug}")
    fun category(@PathVariable slug: String): CategoryResponse = categories.get(slug)

    /** Every visible product and category with its last change, for the frontend's sitemap.xml. */
    @GetMapping("/sitemap")
    fun sitemap(): SitemapEntries = products.sitemap()

    @GetMapping("/products/{slug}")
    fun product(@PathVariable slug: String): ProductDetail = products.get(slug)
}
