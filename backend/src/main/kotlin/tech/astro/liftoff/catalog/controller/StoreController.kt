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
import tech.astro.liftoff.catalog.dto.ProductSummary
import tech.astro.liftoff.catalog.dto.ShopHome
import tech.astro.liftoff.catalog.service.CategoryService
import tech.astro.liftoff.catalog.service.ProductService
import tech.astro.liftoff.shared.web.PageResponse

/** The public catalog: no account needed, server-rendered by the frontend for search engines. */
@RestController
@RequestMapping("/api/v1/public/shop")
@Tag(name = "Store")
@SecurityRequirements
class StoreController(
    private val products: ProductService,
    private val categories: CategoryService,
) {
    /** Featured and newest products plus the category menu, in one request for the home page. */
    @GetMapping("/home")
    fun home(): ShopHome = products.home()

    @GetMapping("/categories")
    fun categories(): List<CategoryResponse> = categories.list()

    /** `category` is a category slug. */
    @GetMapping("/products")
    fun products(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "NEWEST") sort: ProductSort,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "24") size: Int,
    ): PageResponse<ProductSummary> = PageResponse.of(products.search(q, category, sort, page, size)) { it }

    @GetMapping("/products/{slug}")
    fun product(@PathVariable slug: String): ProductDetail = products.get(slug)
}
