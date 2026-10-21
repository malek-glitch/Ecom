package tech.astro.liftoff.user.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.auth.service.SessionService
import tech.astro.liftoff.mail.AccountMailer
import tech.astro.liftoff.shared.error.ConflictException
import tech.astro.liftoff.shared.error.FieldValidationException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.security.UserSecurityStateCache
import tech.astro.liftoff.shared.web.ClientContext
import tech.astro.liftoff.storage.StorageObjectsOrphaned
import tech.astro.liftoff.user.dto.ChangePasswordRequest
import tech.astro.liftoff.user.dto.DeleteAccountRequest
import tech.astro.liftoff.user.dto.MeResponse
import tech.astro.liftoff.user.dto.UpdateProfileRequest
import tech.astro.liftoff.user.model.Role
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.repository.UserRepository
import java.util.UUID

/** What a signed-in user can do to their own account. */
@Service
class UserService(
    private val users: UserRepository,
    private val mapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
    private val sessions: SessionService,
    private val mailer: AccountMailer,
    private val audit: AuditService,
    private val stateCache: UserSecurityStateCache,
    private val events: ApplicationEventPublisher,
) {
    @Transactional(readOnly = true)
    fun me(current: CurrentUser): MeResponse = mapper.toMe(load(current.id))

    @Transactional
    fun updateProfile(current: CurrentUser, request: UpdateProfileRequest, ctx: ClientContext): MeResponse {
        val user = load(current.id)
        val changes = mutableMapOf<String, Any?>()
        request.fullName?.trim()?.let { if (it != user.fullName) { changes["fullName"] = it; user.fullName = it } }
        request.locale?.let { if (it != user.locale) { changes["locale"] = it; user.locale = it } }
        if (changes.isNotEmpty()) audit.record(AuditActions.USER_PROFILE_UPDATED, actor = user, target = user, ctx = ctx, details = changes)
        return mapper.toMe(user)
    }

    /**
     * Signs out every other device and invalidates this device's access token too (security
     * version bump): the client refreshes once and carries on.
     */
    @Transactional
    fun changePassword(current: CurrentUser, request: ChangePasswordRequest, ctx: ClientContext) {
        val user = load(current.id)
        val existing = user.passwordHash
        if (existing != null && (request.currentPassword == null || !passwordEncoder.matches(request.currentPassword, existing))) {
            throw FieldValidationException("currentPassword", "Incorrect", "is incorrect")
        }
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        user.bumpSecurityVersion()
        sessions.revokeAll(user.id, "password_changed", keep = current.sessionId)
        audit.record(AuditActions.USER_PASSWORD_CHANGED, actor = user, target = user, ctx = ctx)
        mailer.passwordChanged(user)
    }

    @Transactional
    fun revokeOtherSessions(current: CurrentUser, ctx: ClientContext): Int {
        val user = load(current.id)
        val revoked = sessions.revokeAll(user.id, "signed_out_elsewhere", keep = current.sessionId)
        audit.record(AuditActions.USER_SESSIONS_REVOKED, actor = user, target = user, ctx = ctx, details = mapOf("count" to revoked))
        return revoked
    }

    @Transactional
    fun deleteAccount(current: CurrentUser, request: DeleteAccountRequest, ctx: ClientContext) {
        val user = load(current.id)
        val hash = user.passwordHash
        if (hash != null) {
            if (request.password == null || !passwordEncoder.matches(request.password, hash)) {
                throw FieldValidationException("password", "Incorrect", "is incorrect")
            }
        } else if (!request.confirmEmail.equals(user.email, ignoreCase = true)) {
            throw FieldValidationException("confirmEmail", "Mismatch", "must match your email address")
        }
        if (user.role == Role.ADMIN && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ConflictException("admin.last_admin", "The last administrator can't delete their account")
        }
        audit.record(AuditActions.USER_ACCOUNT_DELETED, actor = user, target = user, ctx = ctx)
        user.avatarKey?.let { events.publishEvent(StorageObjectsOrphaned(listOf(it))) }
        users.delete(user) // sessions, tokens and notes cascade in the database
        stateCache.evict(user.id)
    }

    fun load(id: UUID): User = users.findById(id).orElseThrow { NotFoundException("user.not_found", "User not found") }
}
