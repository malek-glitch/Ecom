package tech.astro.liftoff.order.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import tech.astro.liftoff.order.model.OrderStatus
import tech.astro.liftoff.order.model.PaymentMethod
import java.time.Instant
import java.util.UUID

const val MAX_CART_LINES = 50
const val MAX_LINE_QUANTITY = 99

data class CartItemRequest(
    @field:NotNull
    val productId: UUID? = null,
    @field:Min(1) @field:Max(MAX_LINE_QUANTITY.toLong())
    val quantity: Int = 1,
)

/** The cart lives in the browser; this prices it with current data. */
data class CartQuoteRequest(
    @field:Valid @field:Size(max = MAX_CART_LINES)
    val items: List<CartItemRequest> = emptyList(),
)

data class CartLine(
    val productId: UUID,
    val slug: String,
    val name: String,
    val imageUrl: String?,
    val unitPriceCents: Long,
    val quantity: Int,
    val lineTotalCents: Long,
    /** What's left (null: not tracked, always available). Lower than `quantity` means the line can't be ordered as is. */
    val stock: Int?,
)

data class CartQuote(
    val currency: String,
    val lines: List<CartLine>,
    /** Products that no longer exist or are no longer sold: drop them from the cart. */
    val unavailableProductIds: List<UUID>,
    val subtotalCents: Long,
    val shippingCents: Long,
    val totalCents: Long,
    /** Subtotal from which shipping is free; null when there is no such offer. */
    val freeShippingThresholdCents: Long?,
    /** True when every line can be ordered (all in stock, nothing unavailable, not empty). */
    val orderable: Boolean,
)

data class AddressRequest(
    @field:NotBlank @field:Size(max = 120)
    val fullName: String = "",
    @field:Size(max = 40)
    val phone: String = "",
    @field:NotBlank @field:Size(max = 200)
    val line1: String = "",
    @field:Size(max = 200)
    val line2: String = "",
    @field:NotBlank @field:Size(max = 120)
    val city: String = "",
    @field:NotBlank @field:Size(max = 20)
    val postalCode: String = "",
    @field:NotBlank @field:Size(max = 80)
    val country: String = "",
)

data class PlaceOrderRequest(
    @field:Valid @field:NotEmpty @field:Size(max = MAX_CART_LINES)
    val items: List<CartItemRequest> = emptyList(),
    @field:Valid @field:NotNull
    val shippingAddress: AddressRequest? = null,
    @field:NotNull
    val paymentMethod: PaymentMethod? = null,
    @field:Size(max = 1000)
    val note: String = "",
    /**
     * The total the customer saw. When prices changed meanwhile the order is refused with
     * 409 `order.price_changed`, instead of charging an amount they never agreed to.
     */
    val expectedTotalCents: Long? = null,
)

data class AddressResponse(
    val fullName: String,
    val phone: String,
    val line1: String,
    val line2: String,
    val city: String,
    val postalCode: String,
    val country: String,
)

data class OrderItemResponse(
    /** Null once the product has been deleted. */
    val productId: UUID?,
    val productSlug: String,
    val productName: String,
    val sku: String?,
    val unitPriceCents: Long,
    val quantity: Int,
    val lineTotalCents: Long,
)

data class OrderSummary(
    val id: UUID,
    val number: Long,
    val status: OrderStatus,
    val email: String,
    /** The recipient on the shipping address. */
    val customerName: String,
    val currency: String,
    val totalCents: Long,
    val itemCount: Int,
    val paymentMethod: PaymentMethod,
    val createdAt: Instant,
)

data class OrderResponse(
    val id: UUID,
    val number: Long,
    val status: OrderStatus,
    val email: String,
    val customerId: UUID?,
    val paymentMethod: PaymentMethod,
    val currency: String,
    val subtotalCents: Long,
    val shippingCents: Long,
    val totalCents: Long,
    val shippingAddress: AddressResponse,
    val note: String,
    val items: List<OrderItemResponse>,
    /** Statuses this order can move to next (for admins), so the UI only offers valid ones. */
    val nextStatuses: List<OrderStatus>,
    /** Whether the customer may still cancel it themselves. */
    val cancellable: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class UpdateOrderStatusRequest(
    @field:NotNull
    val status: OrderStatus? = null,
)

data class ShopStats(
    val currency: String,
    /** Revenue of non-cancelled orders in the store currency. */
    val revenueLast30DaysCents: Long,
    val ordersLast30Days: Long,
    val pendingOrders: Long,
    val paidOrders: Long,
    val activeProducts: Long,
    val lowStockProducts: Long,
)
