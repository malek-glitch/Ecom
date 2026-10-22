package tech.astro.liftoff.order.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.catalog.service.CatalogMapper
import tech.astro.liftoff.inventory.service.InventoryPolicy
import tech.astro.liftoff.order.dto.CartItemRequest
import tech.astro.liftoff.order.dto.CartLine
import tech.astro.liftoff.order.dto.CartQuote
import tech.astro.liftoff.order.dto.MAX_LINE_QUANTITY
import java.util.UUID

/** Prices a browser-side cart with the current catalog, so the customer always sees real numbers. */
@Service
class CartService(
    private val products: ProductRepository,
    private val mapper: CatalogMapper,
    private val pricing: PricingService,
    private val inventory: InventoryPolicy,
) {
    @Transactional(readOnly = true)
    fun quote(items: List<CartItemRequest>): CartQuote {
        val wanted = merge(items)
        val found = products.findAllByIdIn(wanted.keys).filter { it.active }.associateBy { it.id }
        val lines = wanted.mapNotNull { (id, quantity) ->
            val product = found[id] ?: return@mapNotNull null
            CartLine(
                productId = id,
                slug = product.slug,
                name = product.name,
                imageUrl = mapper.imageUrl(product.imageKey),
                unitPriceCents = product.priceCents,
                quantity = quantity,
                lineTotalCents = product.priceCents * quantity,
                stock = inventory.publicStock(product),
            )
        }
        val unavailable = wanted.keys.filter { it !in found }
        val subtotal = lines.sumOf { it.lineTotalCents }
        val shipping = pricing.shipping(subtotal)
        return CartQuote(
            currency = pricing.currency(),
            lines = lines,
            unavailableProductIds = unavailable,
            subtotalCents = subtotal,
            shippingCents = shipping,
            totalCents = subtotal + shipping,
            freeShippingThresholdCents = pricing.freeShippingThreshold(),
            orderable = lines.isNotEmpty() && unavailable.isEmpty() && lines.all { line -> line.stock == null || line.quantity <= line.stock },
        )
    }

    companion object {
        /** Adds up repeated products (keeping first-seen order) and caps each line. */
        fun merge(items: List<CartItemRequest>): LinkedHashMap<UUID, Int> {
            val merged = LinkedHashMap<UUID, Int>()
            for (item in items) {
                val id = item.productId ?: continue
                merged[id] = ((merged[id] ?: 0) + item.quantity).coerceAtMost(MAX_LINE_QUANTITY)
            }
            return merged
        }
    }
}
