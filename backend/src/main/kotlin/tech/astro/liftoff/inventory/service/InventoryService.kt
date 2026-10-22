package tech.astro.liftoff.inventory.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.catalog.model.Category
import tech.astro.liftoff.catalog.model.Product
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.catalog.service.CatalogMapper
import tech.astro.liftoff.inventory.dto.AdjustStockRequest
import tech.astro.liftoff.inventory.dto.AdjustmentKind
import tech.astro.liftoff.inventory.dto.InventoryRow
import tech.astro.liftoff.inventory.dto.InventorySettingsRequest
import tech.astro.liftoff.inventory.dto.InventorySummary
import tech.astro.liftoff.inventory.dto.MovementResponse
import tech.astro.liftoff.inventory.dto.StockStatusFilter
import tech.astro.liftoff.inventory.model.MovementType
import tech.astro.liftoff.inventory.model.StockMovement
import tech.astro.liftoff.inventory.repository.StockMovementRepository
import tech.astro.liftoff.mail.MailKind
import tech.astro.liftoff.mail.OutgoingMail
import tech.astro.liftoff.order.model.ShopOrder
import tech.astro.liftoff.settings.Setting
import tech.astro.liftoff.settings.SettingsService
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.ConflictException
import tech.astro.liftoff.shared.error.FieldValidationException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.persistence.LIKE_ESCAPE
import tech.astro.liftoff.shared.persistence.containsPattern
import tech.astro.liftoff.user.model.Role
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.repository.UserRepository
import java.time.Clock
import java.util.UUID

/**
 * Stock levels and their history. Every change goes through here: an atomic SQL update of
 * `products.stock` (the entity never writes that column) plus a [StockMovement] in the same
 * transaction, so the history always adds up to the stock.
 */
@Service
class InventoryService(
    private val jdbc: JdbcClient,
    private val products: ProductRepository,
    private val movements: StockMovementRepository,
    private val policy: InventoryPolicy,
    private val mapper: CatalogMapper,
    private val users: UserRepository,
    private val settings: SettingsService,
    private val audit: AuditService,
    private val events: ApplicationEventPublisher,
    private val props: AppProperties,
    private val clock: Clock,
) {
    // ------------------------------------------------------------------ orders

    /**
     * Takes an order's tracked lines out of stock, in product-id order (so concurrent checkouts
     * lock rows in the same order). One short line throws `order.out_of_stock` and the whole
     * transaction, order included, rolls back. Does nothing while the module is off.
     */
    @Transactional
    fun takeForOrder(order: ShopOrder, quantities: Map<UUID, Int>, byId: Map<UUID, Product>) {
        if (!policy.enabled()) return
        for ((id, quantity) in quantities.toSortedMap()) {
            val product = byId.getValue(id)
            if (!product.trackInventory) continue
            val after = jdbc.sql("update products set stock = stock - :quantity where id = :id and stock >= :quantity returning stock")
                .param("id", id).param("quantity", quantity)
                .query(Int::class.java).optional().orElse(null)
                ?: throw ConflictException("order.out_of_stock", "Not enough stock for ${product.name}")
            record(product.id, MovementType.SOLD, -quantity, after, order = order)
            alertIfLow(product, before = after + quantity, after = after)
        }
    }

    /** Puts back what an order took (whatever the settings say now), going by its movements. */
    @Transactional
    fun returnForOrder(order: ShopOrder) {
        order.items.forEach { item ->
            val productId = item.productId ?: return@forEach
            val taken = -movements.netForOrder(order.id, productId)
            if (taken <= 0) return@forEach
            val after = jdbc.sql("update products set stock = stock + :quantity where id = :id returning stock")
                .param("id", productId).param("quantity", taken.toInt())
                .query(Int::class.java).optional().orElse(null) ?: return@forEach
            record(productId, MovementType.RETURNED, taken.toInt(), after, order = order)
        }
    }

    // ------------------------------------------------------------------ admin

    /** The first movement of a new product: the stock it was created with. */
    @Transactional
    fun recordInitial(product: Product, actor: User?) {
        record(product.id, MovementType.INITIAL, product.stock, product.stock, actor = actor)
    }

    /** Receive, remove or count stock. The row is locked, so a sale in progress can't be lost. */
    @Transactional
    fun adjust(actor: User, productId: UUID, request: AdjustStockRequest): MovementResponse {
        val product = load(productId)
        val before = jdbc.sql("select stock from products where id = :id for update").param("id", productId).query(Int::class.java).single()
        val quantity = request.quantity
        val (type, after) = when (request.kind!!) {
            AdjustmentKind.RECEIVE -> MovementType.RECEIVED to before + quantity
            AdjustmentKind.REMOVE -> MovementType.REMOVED to before - quantity
            AdjustmentKind.COUNT -> MovementType.COUNTED to quantity
        }
        if (request.kind != AdjustmentKind.COUNT && quantity == 0) {
            throw FieldValidationException("quantity", "Positive", "must be greater than 0")
        }
        if (after < 0) throw FieldValidationException("quantity", "StockTooLow", "is more than the $before in stock")
        if (after.toLong() > MAX_STOCK) throw FieldValidationException("quantity", "Max", "would take the stock above $MAX_STOCK")
        jdbc.sql("update products set stock = :after where id = :id").param("id", productId).param("after", after).update()
        val movement = record(productId, type, after - before, after, note = request.note.trim(), actor = actor)
        audit.record(
            AuditActions.ADMIN_STOCK_ADJUSTED,
            actor = actor,
            targetType = "product",
            targetId = productId.toString(),
            targetLabel = product.name,
            details = mapOf("type" to type.name, "from" to before, "to" to after, "note" to request.note.trim().ifEmpty { null }),
        )
        if (policy.tracked(product)) alertIfLow(product, before, after)
        return toResponse(movement, product.name)
    }

    @Transactional
    fun updateSettings(actor: User, productId: UUID, request: InventorySettingsRequest): InventoryRow {
        val product = load(productId)
        product.trackInventory = request.trackInventory
        product.lowStockThreshold = request.lowStockThreshold
        product.costCents = request.costCents
        products.saveAndFlush(product)
        audit.record(
            AuditActions.ADMIN_INVENTORY_SETTINGS_CHANGED,
            actor = actor,
            targetType = "product",
            targetId = productId.toString(),
            targetLabel = product.name,
            details = mapOf("trackInventory" to request.trackInventory, "lowStockThreshold" to request.lowStockThreshold, "costCents" to request.costCents),
        )
        return row(product)
    }

    /** Products with their stock, most urgent first (out of stock, then lowest). */
    @Transactional(readOnly = true)
    fun overview(query: String?, status: StockStatusFilter?, page: Int, size: Int): Page<InventoryRow> {
        val threshold = policy.globalThreshold()
        var spec = Specification.unrestricted<Product>()
        if (!query.isNullOrBlank()) {
            val pattern = containsPattern(query)
            spec = spec.and { root, _, cb ->
                cb.or(cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE), cb.like(cb.lower(root.get("sku")), pattern, LIKE_ESCAPE))
            }
        }
        spec = when (status) {
            StockStatusFilter.OUT -> spec.and { root, _, cb -> cb.and(cb.isTrue(root.get("trackInventory")), cb.le(root.get<Int>("stock"), 0)) }
            StockStatusFilter.LOW -> spec.and { root, _, cb ->
                cb.and(
                    cb.isTrue(root.get("trackInventory")),
                    cb.gt(root.get<Int>("stock"), 0),
                    cb.le(root.get<Int>("stock"), cb.coalesce(root.get<Int>("lowStockThreshold"), threshold)),
                )
            }
            StockStatusFilter.IN_STOCK -> spec.and { root, _, cb ->
                cb.and(cb.isTrue(root.get("trackInventory")), cb.gt(root.get<Int>("stock"), cb.coalesce(root.get<Int>("lowStockThreshold"), threshold)))
            }
            StockStatusFilter.UNTRACKED -> spec.and { root, _, cb -> cb.isFalse(root.get("trackInventory")) }
            null -> spec
        }
        val sort = Sort.by(Sort.Order.desc("trackInventory"), Sort.Order.asc("stock"), Sort.Order.asc("name"))
        return products.findAll(spec, PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), sort)).map(::row)
    }

    @Transactional(readOnly = true)
    fun row(productId: UUID): InventoryRow = row(load(productId))

    @Transactional(readOnly = true)
    fun summary(): InventorySummary {
        val threshold = policy.globalThreshold()
        return jdbc.sql(
            """
            select count(*) filter (where track_inventory) as tracked,
                   coalesce(sum(stock) filter (where track_inventory and stock > 0), 0) as units,
                   count(*) filter (where track_inventory and stock > 0 and stock <= coalesce(low_stock_threshold, :threshold)) as low,
                   count(*) filter (where track_inventory and stock <= 0) as out_of_stock,
                   coalesce(sum(stock::bigint * cost_cents) filter (where track_inventory and stock > 0), 0) as value,
                   coalesce(sum(stock::bigint * price_cents) filter (where track_inventory and stock > 0), 0) as retail,
                   count(*) filter (where track_inventory and stock > 0 and cost_cents is null) as missing_cost
            from products
            """.trimIndent(),
        ).param("threshold", threshold).query { rs, _ ->
            InventorySummary(
                enabled = policy.enabled(),
                lowStockThreshold = threshold,
                lowStockEmails = settings.bool(Setting.INVENTORY_LOW_STOCK_EMAILS),
                currency = settings.string(Setting.SHOP_CURRENCY),
                trackedProducts = rs.getLong("tracked"),
                unitsInStock = rs.getLong("units"),
                lowStock = rs.getLong("low"),
                outOfStock = rs.getLong("out_of_stock"),
                stockValueCents = rs.getLong("value"),
                retailValueCents = rs.getLong("retail"),
                productsWithoutCost = rs.getLong("missing_cost"),
            )
        }.single()
    }

    /** Stock history, newest first: of one product, or of the whole store. */
    @Transactional(readOnly = true)
    fun movements(productId: UUID?, type: MovementType?, page: Int, size: Int): Page<MovementResponse> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = when {
            productId != null && type != null -> movements.findByProductIdAndType(productId, type, pageable)
            productId != null -> movements.findByProductId(productId, pageable)
            type != null -> movements.findByType(type, pageable)
            else -> movements.findAll(pageable)
        }
        val names = products.findAllById(result.content.map { it.productId }.toSet()).associate { it.id to it.name }
        return result.map { toResponse(it, names[it.productId]) }
    }

    /** Stock levels as CSV, for a stock-take or a spreadsheet. */
    @Transactional(readOnly = true)
    fun exportCsv(): String {
        val rows = products.findAll(Sort.by("name")).map(::row)
        return buildString {
            appendLine("sku,name,category,tracked,stock,low_stock_threshold,status,unit_cost,stock_value")
            rows.forEach { r ->
                appendLine(
                    listOf(
                        r.sku.orEmpty(), r.name, r.categoryName.orEmpty(), r.trackInventory.toString(), r.stock.toString(),
                        r.lowStockThreshold.toString(), r.status.name, r.costCents?.toString().orEmpty(), r.stockValueCents?.toString().orEmpty(),
                    ).joinToString(",", transform = ::csvCell),
                )
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun load(id: UUID): Product = products.findWithCategoryById(id) ?: throw NotFoundException("product.not_found", "Product not found")

    private fun record(
        productId: UUID,
        type: MovementType,
        quantity: Int,
        stockAfter: Int,
        note: String = "",
        order: ShopOrder? = null,
        actor: User? = null,
    ): StockMovement = movements.save(
        StockMovement(
            productId = productId,
            type = type,
            quantity = quantity,
            stockAfter = stockAfter,
            note = note,
            orderId = order?.id,
            orderNumber = order?.number,
            actorId = actor?.id,
            actorName = actor?.fullName,
            createdAt = clock.instant(),
        ),
    )

    /** Emails the administrators when [product] drops to its low-stock level or sells out. */
    private fun alertIfLow(product: Product, before: Int, after: Int) {
        val threshold = policy.threshold(product)
        val crossedLow = before > threshold && after <= threshold
        val soldOut = before > 0 && after <= 0
        if (!(crossedLow || soldOut) || !settings.bool(Setting.INVENTORY_LOW_STOCK_EMAILS)) return
        val link = UriComponentsBuilder.fromUriString(props.frontendUrl.trimEnd('/')).path("/admin/inventory/{id}").buildAndExpand(product.id).toUriString()
        users.findAllByRoleAndEnabledTrue(Role.ADMIN).forEach { admin ->
            events.publishEvent(OutgoingMail(admin.email, admin.fullName, admin.locale, MailKind.LOW_STOCK, link, listOf(product.name, after.coerceAtLeast(0).toString())))
        }
    }

    private fun row(product: Product): InventoryRow {
        val category: Category? = product.category
        return InventoryRow(
            productId = product.id,
            name = product.name,
            sku = product.sku,
            imageUrl = mapper.imageUrl(product.imageKey),
            categoryName = category?.name,
            active = product.active,
            trackInventory = product.trackInventory,
            stock = product.stock,
            lowStockThreshold = policy.threshold(product),
            lowStockThresholdOverride = product.lowStockThreshold,
            status = policy.status(product),
            priceCents = product.priceCents,
            costCents = product.costCents,
            stockValueCents = product.costCents?.let { it * product.stock.coerceAtLeast(0) },
            version = product.version ?: 0,
        )
    }

    private fun toResponse(m: StockMovement, productName: String?) = MovementResponse(
        m.id, m.productId, productName, m.type, m.quantity, m.stockAfter, m.note, m.orderId, m.orderNumber, m.actorName, m.createdAt,
    )

    companion object {
        const val MAX_STOCK = 1_000_000_000L

        /** Quotes a CSV cell, and defuses formulas (=, +, -, @) that spreadsheets would run. */
        fun csvCell(value: String): String {
            val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
            return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${safe.replace("\"", "\"\"")}\"" else safe
        }
    }
}
