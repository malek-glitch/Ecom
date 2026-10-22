package tech.astro.liftoff.inventory.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant
import java.util.UUID

enum class MovementType {
    /** Stock a product started with (creation, or the opening balance when the module arrived). */
    INITIAL,

    /** A delivery from a supplier. */
    RECEIVED,

    /** Taken by an order. */
    SOLD,

    /** Put back by a cancelled order. */
    RETURNED,

    /** Taken out by hand: damaged, lost, used as a sample... */
    REMOVED,

    /** Set to a counted figure (stock-take); the quantity is the difference found. */
    COUNTED,
}

/** One change of a product's stock. Append-only: corrections are new movements. */
@Entity
@Table(name = "stock_movements")
class StockMovement(
    @Column(name = "product_id", nullable = false, updatable = false)
    val productId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    val type: MovementType,

    /** Signed: positive in, negative out. */
    @Column(name = "quantity", nullable = false, updatable = false)
    val quantity: Int,

    @Column(name = "stock_after", nullable = false, updatable = false)
    val stockAfter: Int,

    @Column(name = "note", nullable = false, length = 300, updatable = false)
    val note: String = "",

    @Column(name = "order_id", updatable = false)
    val orderId: UUID? = null,

    @Column(name = "order_number", updatable = false)
    val orderNumber: Long? = null,

    @Column(name = "actor_id", updatable = false)
    val actorId: UUID? = null,

    @Column(name = "actor_name", length = 120, updatable = false)
    val actorName: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
) : BaseEntity()
