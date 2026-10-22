package tech.astro.liftoff.inventory.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import tech.astro.liftoff.inventory.model.MovementType
import java.time.Instant
import java.util.UUID

enum class StockStatus { IN_STOCK, LOW, OUT, UNTRACKED }

enum class StockStatusFilter { LOW, OUT, IN_STOCK, UNTRACKED }

/** RECEIVE adds [AdjustStockRequest.quantity], REMOVE takes it away, COUNT sets the stock to it. */
enum class AdjustmentKind { RECEIVE, REMOVE, COUNT }

data class InventoryRow(
    val productId: UUID,
    val name: String,
    val sku: String?,
    val imageUrl: String?,
    val categoryName: String?,
    val active: Boolean,
    val trackInventory: Boolean,
    val stock: Int,
    /** The threshold in force: the product's own, or the store-wide one. */
    val lowStockThreshold: Int,
    /** The product's own threshold, null when it uses the store-wide one. */
    val lowStockThresholdOverride: Int?,
    val status: StockStatus,
    val priceCents: Long,
    val costCents: Long?,
    /** stock × cost, null without a cost. */
    val stockValueCents: Long?,
    val version: Long,
)

data class InventorySummary(
    /** The module switch (setting inventory.enabled). Off: nothing is tracked or enforced. */
    val enabled: Boolean,
    val lowStockThreshold: Int,
    val lowStockEmails: Boolean,
    val currency: String,
    val trackedProducts: Long,
    val unitsInStock: Long,
    val lowStock: Long,
    val outOfStock: Long,
    /** Stock valued at cost, for products that have a cost. */
    val stockValueCents: Long,
    /** Stock valued at selling price. */
    val retailValueCents: Long,
    /** Products in stock without a cost: the stock value leaves them out. */
    val productsWithoutCost: Long,
)

data class MovementResponse(
    val id: UUID,
    val productId: UUID,
    /** Current product name, or null if the product was deleted. */
    val productName: String?,
    val type: MovementType,
    val quantity: Int,
    val stockAfter: Int,
    val note: String,
    val orderId: UUID?,
    val orderNumber: Long?,
    val actorName: String?,
    val createdAt: Instant,
)

data class AdjustStockRequest(
    @field:NotNull
    val kind: AdjustmentKind? = null,
    @field:PositiveOrZero @field:Max(1_000_000)
    val quantity: Int = 0,
    @field:Size(max = 300)
    val note: String = "",
)

data class InventorySettingsRequest(
    val trackInventory: Boolean = true,
    /** Null: use the store-wide threshold. */
    @field:Min(0) @field:Max(1_000_000)
    val lowStockThreshold: Int? = null,
    @field:PositiveOrZero @field:Max(100_000_000_00)
    val costCents: Long? = null,
)
