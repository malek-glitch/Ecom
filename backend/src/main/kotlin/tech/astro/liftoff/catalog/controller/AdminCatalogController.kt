package tech.astro.liftoff.catalog.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import tech.astro.liftoff.catalog.dto.AdminProductResponse
import tech.astro.liftoff.catalog.dto.CategoryResponse
import tech.astro.liftoff.catalog.dto.ProductStatusFilter
import tech.astro.liftoff.catalog.dto.SaveCategoryRequest
import tech.astro.liftoff.catalog.dto.SaveProductRequest
import tech.astro.liftoff.catalog.dto.SearchInsights
import tech.astro.liftoff.catalog.service.CategoryService
import tech.astro.liftoff.catalog.service.ProductImageService
import tech.astro.liftoff.catalog.service.ProductService
import tech.astro.liftoff.catalog.service.SearchInsightsService
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.security.RateLimited
import tech.astro.liftoff.shared.web.PageResponse
import tech.astro.liftoff.shared.web.Paging
import tech.astro.liftoff.user.service.UserService
import java.util.UUID

/** Catalog management, for admins only (like every route under /api/v1/admin). */
@RestController
@RequestMapping("/api/v1/admin/shop")
@Tag(name = "Admin: catalog")
class AdminCatalogController(
    private val products: ProductService,
    private val categories: CategoryService,
    private val images: ProductImageService,
    private val users: UserService,
    private val insights: SearchInsightsService,
) {
    /** Includes inactive products. `q` searches name, description and SKU. Newest first. */
    @GetMapping("/products")
    fun products(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) status: ProductStatusFilter?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<AdminProductResponse> =
        PageResponse.of(products.adminSearch(q, categoryId, status, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))) { it }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(user: CurrentUser, @Valid @RequestBody request: SaveProductRequest): AdminProductResponse =
        products.create(users.load(user.id), request)

    @GetMapping("/products/{id}")
    fun product(@PathVariable id: UUID): AdminProductResponse = products.adminGet(id)

    @PutMapping("/products/{id}")
    fun updateProduct(user: CurrentUser, @PathVariable id: UUID, @Valid @RequestBody request: SaveProductRequest): AdminProductResponse =
        products.update(users.load(user.id), id, request)

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(user: CurrentUser, @PathVariable id: UUID) = products.delete(users.load(user.id), id)

    /** 404 `storage.disabled` unless STORAGE_ENABLED=true. */
    @PostMapping("/products/{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RateLimited("upload")
    fun uploadImage(@PathVariable id: UUID, @RequestPart("file") file: MultipartFile): AdminProductResponse = images.upload(id, file)

    @DeleteMapping("/products/{id}/image")
    fun deleteImage(@PathVariable id: UUID): AdminProductResponse = images.remove(id)

    /** Popular searches and searches that found nothing, over the last 30 days. */
    @GetMapping("/search-insights")
    fun searchInsights(): SearchInsights = insights.insights()

    /** Every category, with its total number of products (active or not). */
    @GetMapping("/categories")
    fun categories(): List<CategoryResponse> = categories.list(includeInactiveProducts = true)

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(user: CurrentUser, @Valid @RequestBody request: SaveCategoryRequest): CategoryResponse =
        categories.create(users.load(user.id), request)

    @PutMapping("/categories/{id}")
    fun updateCategory(user: CurrentUser, @PathVariable id: UUID, @Valid @RequestBody request: SaveCategoryRequest): CategoryResponse =
        categories.update(users.load(user.id), id, request)

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(user: CurrentUser, @PathVariable id: UUID) = categories.delete(users.load(user.id), id)
}
