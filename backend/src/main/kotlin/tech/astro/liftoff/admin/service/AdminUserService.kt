package tech.astro.liftoff.admin.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.admin.dto.AdminStats
import tech.astro.liftoff.admin.dto.AdminUserDetail
import tech.astro.liftoff.admin.dto.AdminUserSummary
import tech.astro.liftoff.admin.dto.DailyCount
import tech.astro.liftoff.admin.dto.InviteUserRequest
import tech.astro.liftoff.admin.dto.UpdateUserRequest
import tech.astro.liftoff.admin.dto.UserStatusFilter
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.auth.model.TokenPurpose
import tech.astro.liftoff.auth.repository.UserSessionRepository
import tech.astro.liftoff.auth.service.AuthService
import tech.astro.liftoff.auth.service.SessionService
import tech.astro.liftoff.auth.service.UserTokenService
import tech.astro.liftoff.mail.AccountMailer
import tech.astro.liftoff.shared.error.ConflictException
import tech.astro.liftoff.shared.error.ForbiddenException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.i18n.SupportedLocales
import tech.astro.liftoff.shared.persistence.LIKE_ESCAPE
import tech.astro.liftoff.shared.persistence.containsPattern
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.security.UserSecurityStateCache
import tech.astro.liftoff.shared.web.ClientContext
import tech.astro.liftoff.storage.StorageObjectsOrphaned
import tech.astro.liftoff.user.model.Role
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.repository.UserRepository
import tech.astro.liftoff.user.service.UserMapper
import java.sql.Date
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * User management for administrators. Guard rails (from TuniMarché's admin): an admin can't
 * demote, disable or delete themselves, and the last enabled admin can never be removed.
 */
@Service
class AdminUserService(
    private val users: UserRepository,
    private val sessionRepository: UserSessionRepository,
    private val sessions: SessionService,
    private val userTokens: UserTokenService,
    private val mailer: AccountMailer,
    private val audit: AuditService,
    private val stateCache: UserSecurityStateCache,
    private val mapper: UserMapper,
    private val events: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun search(query: String?, role: Role?, status: UserStatusFilter?, pageable: Pageable): Page<AdminUserSummary> {
        var spec = Specification.unrestricted<User>()
        if (!query.isNullOrBlank()) {
            val pattern = containsPattern(query)
            spec = spec.and { root, _, cb ->
                cb.or(
                    cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("fullName")), pattern, LIKE_ESCAPE),
                )
            }
        }
        if (role != null) spec = spec.and { root, _, cb -> cb.equal(root.get<Role>("role"), role) }
        when (status) {
            UserStatusFilter.ACTIVE -> spec = spec.and { root, _, cb -> cb.isTrue(root.get("enabled")) }
            UserStatusFilter.DISABLED -> spec = spec.and { root, _, cb -> cb.isFalse(root.get("enabled")) }
            UserStatusFilter.UNVERIFIED -> spec = spec.and { root, _, cb -> cb.isNull(root.get<Any>("emailVerifiedAt")) }
            null -> Unit
        }
        return users.findAll(spec, pageable).map(::summary)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): AdminUserDetail = detail(load(id))

    @Transactional
    fun invite(actor: CurrentUser, request: InviteUserRequest, ctx: ClientContext): AdminUserDetail {
        val admin = load(actor.id)
        val email = AuthService.normalizeEmail(request.email)
        if (users.existsByEmail(email)) throw ConflictException("auth.email_taken", "An account with this email already exists")
        val user = users.save(
            User(
                email = email,
                fullName = request.fullName.trim(),
                role = request.role,
                locale = SupportedLocales.normalize(request.locale ?: admin.locale),
            ),
        )
        mailer.invitation(user, userTokens.issue(user.id, TokenPurpose.INVITATION), admin.fullName)
        audit.record(AuditActions.ADMIN_USER_INVITED, actor = admin, target = user, ctx = ctx, details = mapOf("role" to user.role.name))
        return detail(user)
    }

    @Transactional
    fun update(actor: CurrentUser, id: UUID, request: UpdateUserRequest, ctx: ClientContext): AdminUserDetail {
        val admin = load(actor.id)
        val user = load(id)
        val self = admin.id == user.id
        val changes = linkedMapOf<String, Any?>()

        request.role?.takeIf { it != user.role }?.let { newRole ->
            if (self) throw ConflictException("admin.cannot_change_own_role", "You can't change your own role")
            if (user.role == Role.ADMIN && user.enabled) requireAnotherEnabledAdmin()
            changes["role"] = "${user.role} → $newRole"
            user.role = newRole
            user.bumpSecurityVersion() // the new role reaches the token on its next refresh
        }
        request.enabled?.takeIf { it != user.enabled }?.let { enabled ->
            if (!enabled) {
                if (self) throw ConflictException("admin.cannot_disable_self", "You can't disable your own account")
                if (user.role == Role.ADMIN) requireAnotherEnabledAdmin()
                user.bumpSecurityVersion()
                sessions.revokeAll(user.id, "disabled_by_admin")
            }
            changes["enabled"] = enabled
            user.enabled = enabled
        }
        request.fullName?.trim()?.takeIf { it != user.fullName }?.let {
            changes["fullName"] = it
            user.fullName = it
        }
        if (changes.isNotEmpty()) {
            stateCache.evict(user.id)
            audit.record(AuditActions.ADMIN_USER_UPDATED, actor = admin, target = user, ctx = ctx, details = changes)
        }
        return detail(user)
    }

    @Transactional
    fun revokeSessions(actor: CurrentUser, id: UUID, ctx: ClientContext) {
        val admin = load(actor.id)
        val user = load(id)
        user.bumpSecurityVersion()
        val revoked = sessions.revokeAll(user.id, "revoked_by_admin")
        audit.record(AuditActions.ADMIN_SESSIONS_REVOKED, actor = admin, target = user, ctx = ctx, details = mapOf("count" to revoked))
    }

    /** Sends a reset link, or a fresh invitation when the user never chose a password. */
    @Transactional
    fun sendPasswordEmail(actor: CurrentUser, id: UUID, ctx: ClientContext) {
        val admin = load(actor.id)
        val user = load(id)
        if (!user.enabled) throw ForbiddenException("admin.user_disabled", "Enable the account first")
        if (!user.hasPassword && user.googleSub == null) {
            mailer.invitation(user, userTokens.issue(user.id, TokenPurpose.INVITATION), admin.fullName)
        } else {
            mailer.passwordReset(user, userTokens.issue(user.id, TokenPurpose.PASSWORD_RESET))
        }
        audit.record(AuditActions.ADMIN_PASSWORD_RESET_SENT, actor = admin, target = user, ctx = ctx)
    }

    @Transactional
    fun delete(actor: CurrentUser, id: UUID, ctx: ClientContext) {
        val admin = load(actor.id)
        val user = load(id)
        if (admin.id == user.id) throw ConflictException("admin.cannot_delete_self", "Delete your own account from your settings instead")
        if (user.role == Role.ADMIN && user.enabled) requireAnotherEnabledAdmin()
        audit.record(AuditActions.ADMIN_USER_DELETED, actor = admin, target = user, ctx = ctx)
        user.avatarKey?.let { events.publishEvent(StorageObjectsOrphaned(listOf(it))) }
        users.delete(user)
        stateCache.evict(user.id)
    }

    @Transactional(readOnly = true)
    fun stats(): AdminStats {
        val now = clock.instant()
        val since = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(29)
        val counts = users.countSignupsPerDay(since.atStartOfDay().toInstant(ZoneOffset.UTC))
            .associate { row -> toLocalDate(row[0]) to (row[1] as Number).toLong() }
        return AdminStats(
            totalUsers = users.count(),
            admins = users.countByRole(Role.ADMIN),
            disabledUsers = users.countByEnabledFalse(),
            unverifiedUsers = users.countByEmailVerifiedAtIsNull(),
            newUsersLast7Days = users.countByCreatedAtAfter(now.minus(Duration.ofDays(7))),
            newUsersLast30Days = users.countByCreatedAtAfter(now.minus(Duration.ofDays(30))),
            activeSessions = sessionRepository.countByRevokedAtIsNullAndExpiresAtAfter(now),
            signupsPerDay = (0L until 30L).map { offset -> since.plusDays(offset).let { DailyCount(it, counts[it] ?: 0) } },
        )
    }

    private fun toLocalDate(value: Any): LocalDate = when (value) {
        is LocalDate -> value
        is Date -> value.toLocalDate()
        else -> LocalDate.parse(value.toString())
    }

    private fun requireAnotherEnabledAdmin() {
        if (users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ConflictException("admin.last_admin", "There must always be at least one active administrator")
        }
    }

    private fun load(id: UUID): User = users.findById(id).orElseThrow { NotFoundException("user.not_found", "User not found") }

    private fun summary(user: User) = AdminUserSummary(
        id = user.id,
        email = user.email,
        fullName = user.fullName,
        role = user.role,
        enabled = user.enabled,
        emailVerified = user.emailVerified,
        hasPassword = user.hasPassword,
        googleLinked = user.googleSub != null,
        avatarUrl = mapper.avatarUrl(user),
        lastLoginAt = user.lastLoginAt,
        createdAt = user.createdAt,
    )

    private fun detail(user: User) = AdminUserDetail(
        id = user.id,
        email = user.email,
        fullName = user.fullName,
        role = user.role,
        locale = user.locale,
        enabled = user.enabled,
        emailVerified = user.emailVerified,
        hasPassword = user.hasPassword,
        googleLinked = user.googleSub != null,
        avatarUrl = mapper.avatarUrl(user),
        activeSessions = sessions.countActive(user.id),
        lastLoginAt = user.lastLoginAt,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt,
    )
}
