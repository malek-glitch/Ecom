package tech.astro.liftoff.admin.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.admin.dto.AdminSettingResponse
import tech.astro.liftoff.admin.dto.AdminStats
import tech.astro.liftoff.admin.dto.AuditEventResponse
import tech.astro.liftoff.admin.dto.UpdateSettingRequest
import tech.astro.liftoff.admin.service.AdminUserService
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.settings.SettingView
import tech.astro.liftoff.settings.SettingsService
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.web.PageResponse
import tech.astro.liftoff.shared.web.Paging
import tech.astro.liftoff.user.service.UserService

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
class AdminController(
    private val admin: AdminUserService,
    private val settings: SettingsService,
    private val audit: AuditService,
    private val users: UserService,
) {
    @GetMapping("/stats")
    fun stats(): AdminStats = admin.stats()

    @GetMapping("/settings")
    fun settings(): List<AdminSettingResponse> = settings.all().map(::toResponse)

    @PutMapping("/settings/{key}")
    fun updateSetting(user: CurrentUser, @PathVariable key: String, @Valid @RequestBody request: UpdateSettingRequest): AdminSettingResponse =
        toResponse(settings.set(key, request.value, users.load(user.id)))

    /** `action` filters by prefix (e.g. `admin.`), `q` searches actor and target emails. Newest first. */
    @GetMapping("/audit")
    fun audit(
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): PageResponse<AuditEventResponse> {
        val result = audit.search(action, q, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")))
        return PageResponse.of(result) {
            AuditEventResponse(it.id, it.occurredAt, it.action, it.actorId, it.actorEmail, it.targetType, it.targetId, it.targetLabel, it.ipAddress, it.details)
        }
    }

    private fun toResponse(view: SettingView) =
        AdminSettingResponse(view.key, view.type, view.value, view.default, view.clientVisible, view.description, view.options)
}
