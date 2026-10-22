package tech.astro.liftoff.catalog.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant

/**
 * Something for sale. Prices are integer minor units (cents) in the store currency.
 * `stock` is only changed by [tech.astro.liftoff.inventory.service.InventoryService], with atomic
 * updates that also write the stock history: two buyers can't take the last item, and a form
 * saved late can't overwrite a sale.
 */
@Entity
@Table(name = "products")
class Product(
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    var slug: String,

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "description", nullable = false, columnDefinition = "text")
    var description: String = "",

    @Column(name = "price_cents", nullable = false)
    var priceCents: Long,

    /** The "was" price, shown struck through. Null when not on sale. */
    @Column(name = "compare_at_price_cents")
    var compareAtPriceCents: Long? = null,

    @Column(name = "sku", unique = true, length = 64)
    var sku: String? = null,

    /** Read-only for JPA (`updatable = false`): only the inventory's atomic SQL updates change it. */
    @Column(name = "stock", nullable = false, updatable = false)
    var stock: Int = 0,

    /** Inactive products are hidden from the store but kept for order history. */
    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    /** Shown on the home page. */
    @Column(name = "featured", nullable = false)
    var featured: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    /** Object-storage key of the main picture (only when storage is enabled). */
    @Column(name = "image_key", length = 512)
    var imageKey: String? = null,

    /** False: always available, stock ignored (made to order, digital goods...). */
    @Column(name = "track_inventory", nullable = false)
    var trackInventory: Boolean = true,

    /** Low-stock level for this product; null means the store-wide setting. */
    @Column(name = "low_stock_threshold")
    var lowStockThreshold: Int? = null,

    /** What one unit costs the store, for the stock valuation. Admin only. */
    @Column(name = "cost_cents")
    var costCents: Long? = null,

    /** Title for search engines; null means the name. */
    @Column(name = "seo_title", length = 70)
    var seoTitle: String? = null,

    /** Description for search engines; null means the start of the description. */
    @Column(name = "seo_description", length = 200)
    var seoDescription: String? = null,
) : BaseEntity() {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}
