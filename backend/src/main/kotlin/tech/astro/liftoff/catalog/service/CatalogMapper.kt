package tech.astro.liftoff.catalog.service

import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import tech.astro.liftoff.catalog.dto.AdminProductResponse
import tech.astro.liftoff.catalog.dto.CategoryRef
import tech.astro.liftoff.catalog.dto.CategoryResponse
import tech.astro.liftoff.catalog.dto.ProductDetail
import tech.astro.liftoff.catalog.dto.ProductSummary
import tech.astro.liftoff.catalog.model.Category
import tech.astro.liftoff.catalog.model.Product
import tech.astro.liftoff.storage.ObjectStorage

@Component
class CatalogMapper(private val storage: ObjectProvider<ObjectStorage>) {

    fun imageUrl(key: String?): String? = key?.let { storage.ifAvailable?.publicUrl(it) }

    fun ref(category: Category?): CategoryRef? = category?.let { CategoryRef(it.id, it.slug, it.name) }

    fun category(category: Category, productCount: Long) = CategoryResponse(
        category.id, category.slug, category.name, category.description, category.position, productCount,
        category.seoTitle, category.seoDescription, category.updatedAt, category.version ?: 0,
    )

    fun summary(p: Product) = ProductSummary(
        p.id, p.slug, p.name, p.priceCents, p.compareAtPriceCents, imageUrl(p.imageKey), p.stock, ref(p.category),
    )

    fun detail(p: Product, related: List<Product>) = ProductDetail(
        p.id, p.slug, p.name, p.description, p.priceCents, p.compareAtPriceCents, p.sku, imageUrl(p.imageKey), p.stock, ref(p.category),
        p.seoTitle, p.seoDescription, p.updatedAt, related.map(::summary),
    )

    fun admin(p: Product) = AdminProductResponse(
        p.id, p.slug, p.name, p.description, p.priceCents, p.compareAtPriceCents, p.sku, p.stock, p.active, p.featured,
        ref(p.category), imageUrl(p.imageKey), p.seoTitle, p.seoDescription, p.createdAt, p.updatedAt, p.version ?: 0,
    )
}
