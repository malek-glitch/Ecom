package tech.astro.liftoff.admin.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.admin.dto.AdminUserDetail
import tech.astro.liftoff.admin.dto.AdminUserSummary
import tech.astro.liftoff.admin.dto.InviteUserRequest
import tech.astro.liftoff.admin.dto.UpdateUserRequest
import tech.astro.liftoff.admin.dto.UserStatusFilter
import tech.astro.liftoff.admin.service.AdminUserService
import tech.astro.liftoff.shared.error.BadRequestException
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.web.ClientContext
import tech.astro.liftoff.shared.web.PageResponse
import tech.astro.liftoff.shared.web.Paging
import tech.astro.liftoff.user.model.Role
import java.util.UUID

/** ADMIN only: SecurityConfig restricts every `/api/v1/admin/...` route. */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin")
class AdminUserController(private val admin: AdminUserService) {

    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) role: Role?,
        @RequestParam(required = false) status: UserStatusFilter?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt,desc") sort: String,
    ): PageResponse<AdminUserSummary> {
        val result = admin.search(q, role, status, Paging.of(page, size, parseSort(sort)))
        return PageResponse.of(result) { it }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): AdminUserDetail = admin.get(id)

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(user: CurrentUser, @Valid @RequestBody request: InviteUserRequest, ctx: ClientContext): AdminUserDetail =
        admin.invite(user, request, ctx)

    @PatchMapping("/{id}")
    fun update(user: CurrentUser, @PathVariable id: UUID, @Valid @RequestBody request: UpdateUserRequest, ctx: ClientContext): AdminUserDetail =
        admin.update(user, id, request, ctx)

    @PostMapping("/{id}/revoke-sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeSessions(user: CurrentUser, @PathVariable id: UUID, ctx: ClientContext) = admin.revokeSessions(user, id, ctx)

    @PostMapping("/{id}/password-email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun sendPasswordEmail(user: CurrentUser, @PathVariable id: UUID, ctx: ClientContext) = admin.sendPasswordEmail(user, id, ctx)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: CurrentUser, @PathVariable id: UUID, ctx: ClientContext) = admin.delete(user, id, ctx)

    private fun parseSort(sort: String): Sort {
        val (field, direction) = sort.split(",").let { it[0] to it.getOrElse(1) { "asc" } }
        if (field !in SORTABLE) throw BadRequestException("request.invalid_sort", "Cannot sort by '$field'")
        return Sort.by(if (direction.equals("desc", true)) Sort.Direction.DESC else Sort.Direction.ASC, field)
    }

    private companion object {
        val SORTABLE = setOf("createdAt", "email", "fullName", "lastLoginAt", "role")
    }
}
