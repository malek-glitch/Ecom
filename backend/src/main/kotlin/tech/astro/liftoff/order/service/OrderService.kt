package tech.astro.liftoff.order.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.order.dto.AddressResponse
import tech.astro.liftoff.order.dto.OrderItemResponse
import tech.astro.liftoff.order.dto.OrderResponse
import tech.astro.liftoff.order.dto.OrderSummary
import tech.astro.liftoff.order.dto.PlaceOrderRequest
import tech.astro.liftoff.order.dto.ShopStats
import tech.astro.liftoff.order.model.OrderItem
import tech.astro.liftoff.order.model.OrderStatus
import tech.astro.liftoff.order.model.PaymentMethod
import tech.astro.liftoff.order.model.ShippingAddress
import tech.astro.liftoff.order.model.ShopOrder
import tech.astro.liftoff.order.repository.OrderRepository
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.BadRequestException
import tech.astro.liftoff.shared.error.ConflictException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.persistence.LIKE_ESCAPE
import tech.astro.liftoff.shared.persistence.containsPattern
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.repository.UserRepository
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Checkout and order handling. Customers only ever see their own orders (404 for anyone
 * else's); admins see all of them through the admin endpoints.
 */
@Service
class OrderService(
    private val orders: OrderRepository,
    private val products: ProductRepository,
    private val users: UserRepository,
    private val pricing: PricingService,
    private val mailer: OrderMailer,
    private val audit: AuditService,
    private val props: AppProperties,
    private val clock: Clock,
) {
    // ------------------------------------------------------------------ customer

    /**
     * Prices the cart from the database (never from the client), takes the items out of stock
     * atomically and records the order, all in one transaction: if any line is short, nothing
     * is taken and the customer gets 409 `order.out_of_stock`.
     */
    @Transactional
    fun place(customerId: UUID, request: PlaceOrderRequest): OrderResponse {
        val customer = loadUser(customerId)
        val method = request.paymentMethod!!
        if (method == PaymentMethod.BANK_TRANSFER && !pricing.bankTransferEnabled()) {
            throw BadRequestException("order.payment_method_unavailable", "This payment method is not available")
        }
        val wanted = CartService.merge(request.items)
        val found = products.findAllByIdIn(wanted.keys).filter { it.active }.associateBy { it.id }
        if (wanted.keys.any { it !in found }) {
            throw ConflictException("order.product_unavailable", "A product in the cart is no longer available")
        }

        var position = 0
        val items = wanted.map { (id, quantity) ->
            val product = found.getValue(id)
            OrderItem(id, product.name, product.slug, product.sku, product.priceCents, quantity, product.priceCents * quantity, position++)
        }
        val subtotal = items.sumOf { it.lineTotalCents }
        val shipping = pricing.shipping(subtotal)
        val total = subtotal + shipping
        if (request.expectedTotalCents != null && request.expectedTotalCents != total) {
            throw ConflictException("order.price_changed", "Prices changed since the cart was shown. Review it and try again.")
        }

        // Sorted by id so concurrent checkouts lock rows in the same order (no deadlocks).
        for ((id, quantity) in wanted.toSortedMap()) {
            if (products.takeStock(id, quantity) == 0) {
                throw ConflictException("order.out_of_stock", "Not enough stock for ${found.getValue(id).name}")
            }
        }

        val address = request.shippingAddress!!
        val order = ShopOrder(
            number = orders.nextNumber(),
            customerId = customer.id,
            email = customer.email,
            paymentMethod = method,
            currency = pricing.currency(),
            subtotalCents = subtotal,
            shippingCents = shipping,
            totalCents = total,
            shippingAddress = ShippingAddress(
                fullName = address.fullName.trim(),
                phone = address.phone.trim(),
                line1 = address.line1.trim(),
                line2 = address.line2.trim(),
                city = address.city.trim(),
                postalCode = address.postalCode.trim(),
                country = address.country.trim(),
            ),
            customerNote = request.note.trim(),
        )
        order.items.addAll(items)
        order.itemCount = items.sumOf { it.quantity }
        orders.saveAndFlush(order)
        mailer.placed(order, customer.fullName, customer.locale)
        return toResponse(order)
    }

    @Transactional(readOnly = true)
    fun listMine(customerId: UUID, pageable: Pageable): Page<OrderSummary> {
        val spec = Specification<ShopOrder> { root, _, cb -> cb.equal(root.get<UUID>("customerId"), customerId) }
        return orders.findAll(spec, pageable).map(::toSummary)
    }

    @Transactional(readOnly = true)
    fun getMine(customerId: UUID, id: UUID): OrderResponse = toResponse(loadMine(customerId, id))

    /** Customers may cancel until the order is paid or shipped. */
    @Transactional
    fun cancelMine(customerId: UUID, id: UUID): OrderResponse {
        val order = loadMine(customerId, id)
        if (order.status != OrderStatus.PENDING) {
            throw ConflictException("order.not_cancellable", "This order can no longer be cancelled")
        }
        cancel(order)
        val customer = loadUser(customerId)
        mailer.cancelled(order, customer.fullName, customer.locale)
        return toResponse(orders.saveAndFlush(order))
    }

    // ------------------------------------------------------------------ admin

    /** [query] matches an order number (with or without #), an email or the recipient's name. */
    @Transactional(readOnly = true)
    fun adminSearch(query: String?, status: OrderStatus?, pageable: Pageable): Page<OrderSummary> {
        var spec = Specification.unrestricted<ShopOrder>()
        if (status != null) spec = spec.and { root, _, cb -> cb.equal(root.get<OrderStatus>("status"), status) }
        val q = query?.trim()?.removePrefix("#")
        if (!q.isNullOrBlank()) {
            val number = q.toLongOrNull()
            spec = if (number != null) {
                spec.and { root, _, cb -> cb.equal(root.get<Long>("number"), number) }
            } else {
                val pattern = containsPattern(q)
                spec.and { root, _, cb ->
                    cb.or(
                        cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get<ShippingAddress>("shippingAddress").get("fullName")), pattern, LIKE_ESCAPE),
                    )
                }
            }
        }
        return orders.findAll(spec, pageable).map(::toSummary)
    }

    @Transactional(readOnly = true)
    fun adminGet(id: UUID): OrderResponse = toResponse(load(id))

    /** Moves an order along (see [OrderStatus]); the customer is emailed when it ships or is cancelled. */
    @Transactional
    fun updateStatus(actor: User, id: UUID, next: OrderStatus): OrderResponse {
        val order = load(id)
        val previous = order.status
        if (previous == next) return toResponse(order)
        if (!previous.canMoveTo(next)) {
            throw ConflictException("order.invalid_transition", "An order can't go from $previous to $next")
        }
        if (next == OrderStatus.CANCELLED) cancel(order) else order.status = next
        orders.saveAndFlush(order)
        audit.record(
            AuditActions.ADMIN_ORDER_STATUS_CHANGED,
            actor = actor,
            targetType = "order",
            targetId = order.id.toString(),
            targetLabel = "#${order.number}",
            details = mapOf("from" to previous.name, "to" to next.name),
        )
        val customer = order.customerId?.let { users.findById(it).orElse(null) }
        val name = customer?.fullName ?: order.shippingAddress.fullName
        val locale = customer?.locale ?: "en"
        when (next) {
            OrderStatus.SHIPPED -> mailer.shipped(order, name, locale)
            OrderStatus.CANCELLED -> mailer.cancelled(order, name, locale)
            else -> Unit
        }
        return toResponse(order)
    }

    @Transactional(readOnly = true)
    fun stats(): ShopStats {
        val currency = pricing.currency()
        val recent = orders.revenueSince(clock.instant().minus(Duration.ofDays(30)))
        return ShopStats(
            currency = currency,
            revenueLast30DaysCents = recent.firstOrNull { it.currency == currency }?.total ?: 0,
            ordersLast30Days = recent.sumOf { it.orders },
            pendingOrders = orders.countByStatus(OrderStatus.PENDING),
            paidOrders = orders.countByStatus(OrderStatus.PAID),
            activeProducts = products.countByActiveTrue(),
            lowStockProducts = products.countByActiveTrueAndStockLessThanEqual(props.shop.lowStockThreshold),
        )
    }

    // ------------------------------------------------------------------ helpers

    /** Puts the items back on the shelf. Lines whose product was deleted are skipped. */
    private fun cancel(order: ShopOrder) {
        order.items.forEach { item -> item.productId?.let { products.returnStock(it, item.quantity) } }
        order.status = OrderStatus.CANCELLED
    }

    private fun load(id: UUID): ShopOrder = orders.findWithItemsById(id) ?: throw notFound()

    private fun loadMine(customerId: UUID, id: UUID): ShopOrder = orders.findByIdAndCustomerId(id, customerId) ?: throw notFound()

    private fun loadUser(id: UUID): User = users.findById(id).orElseThrow { NotFoundException("user.not_found", "User not found") }

    private fun notFound() = NotFoundException("order.not_found", "Order not found")

    private fun toSummary(order: ShopOrder) = OrderSummary(
        order.id, order.number, order.status, order.email, order.shippingAddress.fullName, order.currency, order.totalCents,
        order.itemCount, order.paymentMethod, order.createdAt,
    )

    private fun toResponse(order: ShopOrder): OrderResponse {
        val a = order.shippingAddress
        return OrderResponse(
            id = order.id,
            number = order.number,
            status = order.status,
            email = order.email,
            customerId = order.customerId,
            paymentMethod = order.paymentMethod,
            currency = order.currency,
            subtotalCents = order.subtotalCents,
            shippingCents = order.shippingCents,
            totalCents = order.totalCents,
            shippingAddress = AddressResponse(a.fullName, a.phone, a.line1, a.line2, a.city, a.postalCode, a.country),
            note = order.customerNote,
            items = order.items.map {
                OrderItemResponse(it.productId, it.productSlug, it.productName, it.sku, it.unitPriceCents, it.quantity, it.lineTotalCents)
            },
            nextStatuses = OrderStatus.entries.filter { order.status.canMoveTo(it) },
            cancellable = order.status == OrderStatus.PENDING,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
        )
    }
}
