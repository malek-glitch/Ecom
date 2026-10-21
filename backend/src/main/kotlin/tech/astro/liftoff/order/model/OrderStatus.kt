package tech.astro.liftoff.order.model

/**
 * An order's life. Admins move it forward; the customer can only cancel a PENDING one.
 * Cancelling puts the items back in stock.
 */
enum class OrderStatus {
    /** Placed, waiting for payment (bank transfer) or shipping (cash on delivery). */
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    ;

    fun canMoveTo(next: OrderStatus): Boolean = next in TRANSITIONS.getValue(this)

    companion object {
        private val TRANSITIONS: Map<OrderStatus, Set<OrderStatus>> = mapOf(
            PENDING to setOf(PAID, SHIPPED, CANCELLED),
            PAID to setOf(SHIPPED, CANCELLED),
            SHIPPED to setOf(DELIVERED),
            DELIVERED to emptySet(),
            CANCELLED to emptySet(),
        )
    }
}

/**
 * How the customer pays. Both are offline: the admin marks the order PAID by hand.
 * To take card payments, add a method here and a provider (Stripe, PayPal...): see docs/SHOP.md.
 */
enum class PaymentMethod { CASH_ON_DELIVERY, BANK_TRANSFER }
