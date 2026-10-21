package tech.astro.liftoff.order.controller

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.order.dto.CartQuote
import tech.astro.liftoff.order.dto.CartQuoteRequest
import tech.astro.liftoff.order.dto.OrderResponse
import tech.astro.liftoff.order.dto.OrderSummary
import tech.astro.liftoff.order.dto.PlaceOrderRequest
import tech.astro.liftoff.order.dto.ShopStats
import tech.astro.liftoff.order.dto.UpdateOrderStatusRequest
import tech.astro.liftoff.order.model.OrderStatus
import tech.astro.liftoff.order.service.CartService
import tech.astro.liftoff.order.service.OrderService
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.security.RateLimited
import tech.astro.liftoff.shared.web.PageResponse
import tech.astro.liftoff.shared.web.Paging
import tech.astro.liftoff.user.service.UserService
import java.util.UUID

private val NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt")

/** Prices the visitor's cart. Public: people can fill a cart before having an account. */
@RestController
@RequestMapping("/api/v1/public/shop/cart")
@Tag(name = "Store")
@SecurityRequirements
class CartController(private val cart: CartService) {
    @PostMapping("/quote")
    @RateLimited("cart")
    fun quote(@Valid @RequestBody request: CartQuoteRequest): CartQuote = cart.quote(request.items)
}

/** The signed-in customer's orders. */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
class OrderController(private val orders: OrderService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimited("checkout")
    fun place(user: CurrentUser, @Valid @RequestBody request: PlaceOrderRequest): OrderResponse = orders.place(user.id, request)

    @GetMapping
    fun list(
        user: CurrentUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<OrderSummary> = PageResponse.of(orders.listMine(user.id, Paging.of(page, size, NEWEST_FIRST))) { it }

    @GetMapping("/{id}")
    fun get(user: CurrentUser, @PathVariable id: UUID): OrderResponse = orders.getMine(user.id, id)

    /** Only while the order is PENDING; 409 `order.not_cancellable` afterwards. */
    @PostMapping("/{id}/cancel")
    fun cancel(user: CurrentUser, @PathVariable id: UUID): OrderResponse = orders.cancelMine(user.id, id)
}

@RestController
@RequestMapping("/api/v1/admin/shop")
@Tag(name = "Admin: orders")
class AdminOrderController(
    private val orders: OrderService,
    private val users: UserService,
) {
    @GetMapping("/stats")
    fun stats(): ShopStats = orders.stats()

    /** `q` matches an order number, an email or a recipient name. Newest first. */
    @GetMapping("/orders")
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<OrderSummary> = PageResponse.of(orders.adminSearch(q, status, Paging.of(page, size, NEWEST_FIRST))) { it }

    @GetMapping("/orders/{id}")
    fun get(@PathVariable id: UUID): OrderResponse = orders.adminGet(id)

    /** 409 `order.invalid_transition` for a move the order's current status doesn't allow. */
    @PutMapping("/orders/{id}/status")
    fun updateStatus(user: CurrentUser, @PathVariable id: UUID, @Valid @RequestBody request: UpdateOrderStatusRequest): OrderResponse =
        orders.updateStatus(users.load(user.id), id, request.status!!)
}
