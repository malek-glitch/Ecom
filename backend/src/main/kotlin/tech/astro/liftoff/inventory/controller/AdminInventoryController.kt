package tech.astro.liftoff.inventory.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.inventory.dto.AdjustStockRequest
import tech.astro.liftoff.inventory.dto.InventoryRow
import tech.astro.liftoff.inventory.dto.InventorySettingsRequest
import tech.astro.liftoff.inventory.dto.InventorySummary
import tech.astro.liftoff.inventory.dto.MovementResponse
import tech.astro.liftoff.inventory.dto.StockStatusFilter
import tech.astro.liftoff.inventory.model.MovementType
import tech.astro.liftoff.inventory.service.InventoryService
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.web.PageResponse
import tech.astro.liftoff.user.service.UserService
import java.time.LocalDate
import java.util.UUID

/** Stock levels, adjustments and history. ADMIN only, like every route under /api/v1/admin. */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@Tag(name = "Admin: inventory")
class AdminInventoryController(
    private val inventory: InventoryService,
    private val users: UserService,
) {
    @GetMapping("/summary")
    fun summary(): InventorySummary = inventory.summary()

    /** `q` searches names and SKUs. Out of stock first, then lowest stock. */
    @GetMapping("/products")
    fun products(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) status: StockStatusFilter?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
    ): PageResponse<InventoryRow> = PageResponse.of(inventory.overview(q, status, page, size)) { it }

    @GetMapping("/products/{id}")
    fun product(@PathVariable id: UUID): InventoryRow = inventory.row(id)

    /** Receive, remove or count stock. 422 `StockTooLow` when removing more than there is. */
    @PostMapping("/products/{id}/adjustments")
    fun adjust(user: CurrentUser, @PathVariable id: UUID, @Valid @RequestBody request: AdjustStockRequest): MovementResponse =
        inventory.adjust(users.load(user.id), id, request)

    /** Tracking on/off, low-stock threshold and unit cost of one product. */
    @PutMapping("/products/{id}/settings")
    fun settings(user: CurrentUser, @PathVariable id: UUID, @Valid @RequestBody request: InventorySettingsRequest): InventoryRow =
        inventory.updateSettings(users.load(user.id), id, request)

    /** Stock history, newest first; `productId` narrows it to one product. */
    @GetMapping("/movements")
    fun movements(
        @RequestParam(required = false) productId: UUID?,
        @RequestParam(required = false) type: MovementType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
    ): PageResponse<MovementResponse> = PageResponse.of(inventory.movements(productId, type, page, size)) { it }

    @GetMapping("/export", produces = ["text/csv"])
    fun export(): ResponseEntity<String> = ResponseEntity.ok()
        .contentType(MediaType("text", "csv", Charsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock-${LocalDate.now()}.csv\"")
        .body(inventory.exportCsv())
}
