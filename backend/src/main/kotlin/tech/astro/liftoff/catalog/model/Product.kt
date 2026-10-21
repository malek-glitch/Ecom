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
 * `stock` is only changed through [tech.astro.liftoff.catalog.repository.ProductRepository]'s
 * atomic updates when orders are placed or cancelled, so two buyers can't take the last item.
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

    @Column(name = "stock", nullable = false)
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
) : BaseEntity() {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}
