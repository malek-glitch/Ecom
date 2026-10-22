package tech.astro.liftoff.order.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Formula
import org.hibernate.annotations.UpdateTimestamp
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant
import java.util.UUID

@Embeddable
class ShippingAddress(
    @Column(name = "ship_full_name", nullable = false, length = 120)
    var fullName: String,

    @Column(name = "ship_phone", nullable = false, length = 40)
    var phone: String = "",

    @Column(name = "ship_line1", nullable = false, length = 200)
    var line1: String,

    @Column(name = "ship_line2", nullable = false, length = 200)
    var line2: String = "",

    @Column(name = "ship_city", nullable = false, length = 120)
    var city: String,

    @Column(name = "ship_postal_code", nullable = false, length = 20)
    var postalCode: String,

    @Column(name = "ship_country", nullable = false, length = 80)
    var country: String,
)

/**
 * A placed order. Everything needed to fulfil it is copied in (prices, names, address,
 * currency), so editing or deleting a product or an account never rewrites history.
 * Named ShopOrder because ORDER is a reserved word in JPQL.
 */
@Entity(name = "ShopOrder")
@Table(name = "orders")
class ShopOrder(
    /** Shown to people as #1001; the uuid stays the API id. */
    @Column(name = "number", nullable = false, unique = true, updatable = false)
    val number: Long,

    @Column(name = "customer_id")
    var customerId: UUID?,

    @Column(name = "email", nullable = false, length = 254)
    val email: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30, updatable = false)
    val paymentMethod: PaymentMethod,

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    val currency: String,

    @Column(name = "subtotal_cents", nullable = false, updatable = false)
    val subtotalCents: Long,

    @Column(name = "shipping_cents", nullable = false, updatable = false)
    val shippingCents: Long,

    @Column(name = "total_cents", nullable = false, updatable = false)
    val totalCents: Long,

    @Embedded
    val shippingAddress: ShippingAddress,

    @Column(name = "customer_note", nullable = false, length = 1000, updatable = false)
    val customerNote: String = "",
) : BaseEntity() {

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    @OrderBy("position")
    val items: MutableList<OrderItem> = mutableListOf()

    /** Units across all lines, computed by the database so order lists don't load every line. */
    @Formula("(select coalesce(sum(i.quantity), 0) from order_items i where i.order_id = id)")
    var itemCount: Int = 0

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}

@Entity
@Table(name = "order_items")
class OrderItem(
    @Column(name = "product_id", updatable = false)
    val productId: UUID?,

    @Column(name = "product_name", nullable = false, length = 200, updatable = false)
    val productName: String,

    @Column(name = "product_slug", nullable = false, length = 120, updatable = false)
    val productSlug: String,

    @Column(name = "sku", length = 64, updatable = false)
    val sku: String?,

    @Column(name = "unit_price_cents", nullable = false, updatable = false)
    val unitPriceCents: Long,

    @Column(name = "quantity", nullable = false, updatable = false)
    val quantity: Int,

    @Column(name = "line_total_cents", nullable = false, updatable = false)
    val lineTotalCents: Long,

    @Column(name = "position", nullable = false, updatable = false)
    val position: Int,
) : BaseEntity()
