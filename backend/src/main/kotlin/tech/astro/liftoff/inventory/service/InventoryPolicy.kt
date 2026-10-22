package tech.astro.liftoff.inventory.service

import org.springframework.stereotype.Component
import tech.astro.liftoff.catalog.model.Product
import tech.astro.liftoff.inventory.dto.StockStatus
import tech.astro.liftoff.settings.Setting
import tech.astro.liftoff.settings.SettingsService

/**
 * The inventory rules, in one place. A product's stock counts only when the module is on
 * (setting inventory.enabled) and the product tracks it; otherwise it can always be ordered.
 */
@Component
class InventoryPolicy(private val settings: SettingsService) {

    fun enabled(): Boolean = settings.bool(Setting.INVENTORY_ENABLED)

    fun globalThreshold(): Int = settings.int(Setting.INVENTORY_LOW_STOCK_THRESHOLD).coerceAtLeast(0)

    fun tracked(product: Product): Boolean = enabled() && product.trackInventory

    fun threshold(product: Product): Int = product.lowStockThreshold ?: globalThreshold()

    /** What shoppers see: the stock of a tracked product, null for "always available". */
    fun publicStock(product: Product): Int? = if (tracked(product)) product.stock.coerceAtLeast(0) else null

    fun isLow(product: Product): Boolean = tracked(product) && product.stock > 0 && product.stock <= threshold(product)

    fun status(product: Product): StockStatus = when {
        !product.trackInventory -> StockStatus.UNTRACKED
        product.stock <= 0 -> StockStatus.OUT
        product.stock <= threshold(product) -> StockStatus.LOW
        else -> StockStatus.IN_STOCK
    }
}
