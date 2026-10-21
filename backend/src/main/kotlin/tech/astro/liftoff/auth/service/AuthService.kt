package tech.astro.liftoff.auth.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.auth.dto.GoogleLoginRequest
import tech.astro.liftoff.auth.dto.LoginRequest
import tech.astro.liftoff.auth.dto.RegisterRequest
import tech.astro.liftoff.auth.dto.RegisterResponse
import tech.astro.liftoff.auth.dto.ResetPasswordRequest
import tech.astro.liftoff.auth.dto.TokenResponse
import tech.astro.liftoff.auth.model.TokenPurpose
import tech.astro.liftoff.mail.AccountMailer
import tech.astro.liftoff.settings.Setting
import tech.astro.liftoff.settings.SettingsService
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.BadRequestException
import tech.astro.liftoff.shared.error.ConflictException
import tech.astro.liftoff.shared.error.ForbiddenException
import tech.astro.liftoff.shared.error.UnauthorizedException
import tech.astro.liftoff.shared.i18n.SupportedLocales
import tech.astro.liftoff.shared.security.UserSecurityStateCache
import tech.astro.liftoff.shared.web.ClientContext
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.repository.UserRepository
import java.time.Clock

@Service
class AuthService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sessions: SessionService,
    private val userTokens: UserTokenService,
    private val google: GoogleTokenVerifier,
    private val mailer: AccountMailer,
    private val settings: SettingsService,
    private val audit: AuditService,
    private val stateCache: UserSecurityStateCache,
    private val props: AppProperties,
    private val clock: Clock,
) {
    /** Compared against when the email is unknown, so "no such user" costs as much as "wrong password". */
    private val dummyHash by lazy { passwordEncoder.encode("liftoff-timing-equaliser")!! }

    @Transactional
    fun register(request: RegisterRequest, ctx: ClientContext): RegisterResponse {
        requireRegistrationOpen()
        val email = normalizeEmail(request.email)
        if (users.existsByEmail(email)) throw ConflictException("auth.email_taken", "An account with this email already exists")
        val user = users.save(
            User(
                email = email,
                fullName = request.fullName.trim(),
                passwordHash = passwordEncoder.encode(request.password),
                locale = SupportedLocales.normalize(request.locale),
            ),
        )
        audit.record(AuditActions.USER_REGISTERED, actor = user, target = user, ctx = ctx)
        mailer.verifyEmail(user, userTokens.issue(user.id, TokenPurpose.EMAIL_VERIFICATION))
        return if (props.auth.requireVerifiedEmail) {
            RegisterResponse(verificationRequired = true, session = null)
        } else {
            RegisterResponse(verificationRequired = false, session = sessions.startSession(user, ctx))
        }
    }

    @Transactional
    fun login(request: LoginRequest, ctx: ClientContext): TokenResponse {
        val user = users.findByEmail(normalizeEmail(request.email))
        val hash = user?.passwordHash
        if (user == null || hash == null) {
            passwordEncoder.matches(request.password, dummyHash)
            throw invalidCredentials()
        }
        if (!passwordEncoder.matches(request.password, hash)) throw invalidCredentials()
        if (!user.enabled) throw ForbiddenException("auth.account_disabled", "This account has been disabled")
        if (props.auth.requireVerifiedEmail && !user.emailVerified) {
            throw ForbiddenException("auth.email_not_verified", "Please confirm your email address first")
        }
        if (passwordEncoder.upgradeEncoding(hash)) user.passwordHash = passwordEncoder.encode(request.password)
        user.lastLoginAt = clock.instant()
        audit.record(AuditActions.USER_LOGGED_IN, actor = user, ctx = ctx, details = mapOf("method" to "password"))
        return sessions.startSession(user, ctx)
    }

    fun refresh(refreshToken: String, ctx: ClientContext): TokenResponse = sessions.refresh(refreshToken, ctx)

    fun logout(refreshToken: String) = sessions.revokeByRefreshToken(refreshToken, "logout")

    /**
     * Signs in with a Google ID token, creating or linking the account.
     *
     * Linking to an existing account whose email was never verified would let whoever
     * registered that address with a password keep access (a "pre-account takeover").
     * Google proves ownership, so in that case the unverified password is wiped and the
     * account's sessions are revoked before linking.
     */
    @Transactional
    fun loginWithGoogle(request: GoogleLoginRequest, ctx: ClientContext): TokenResponse {
        if (!props.google.enabled) throw BadRequestException("auth.google_disabled", "Google sign-in is not enabled")
        val identity = google.verify(request.idToken)
        if (!identity.emailVerified) throw UnauthorizedException("auth.google_email_unverified", "Your Google email address is not verified")
        val now = clock.instant()

        val user = users.findByGoogleSub(identity.subject) ?: users.findByEmail(normalizeEmail(identity.email))?.also { existing ->
            if (!existing.emailVerified) {
                existing.passwordHash = null
                existing.emailVerifiedAt = now
                existing.bumpSecurityVersion()
                sessions.revokeAll(existing.id, "google_link")
            }
            existing.googleSub = identity.subject
            audit.record(AuditActions.USER_GOOGLE_LINKED, actor = existing, target = existing, ctx = ctx)
        } ?: run {
            requireRegistrationOpen()
            users.save(
                User(
                    email = normalizeEmail(identity.email),
                    fullName = identity.name?.trim()?.takeIf { it.isNotEmpty() }?.take(120) ?: identity.email.substringBefore('@'),
                    googleSub = identity.subject,
                    emailVerifiedAt = now,
                    locale = SupportedLocales.normalize(request.locale),
                ),
            ).also { audit.record(AuditActions.USER_REGISTERED, actor = it, target = it, ctx = ctx, details = mapOf("method" to "google")) }
        }

        if (!user.enabled) throw ForbiddenException("auth.account_disabled", "This account has been disabled")
        user.lastLoginAt = now
        stateCache.evict(user.id)
        audit.record(AuditActions.USER_LOGGED_IN, actor = user, ctx = ctx, details = mapOf("method" to "google"))
        return sessions.startSession(user, ctx)
    }

    @Transactional
    fun verifyEmail(token: String, ctx: ClientContext) {
        val consumed = userTokens.consume(token, setOf(TokenPurpose.EMAIL_VERIFICATION))
        val user = users.findById(consumed.userId).orElseThrow { BadRequestException("auth.token_invalid", "This link is invalid or has expired") }
        if (!user.emailVerified) {
            user.emailVerifiedAt = clock.instant()
            audit.record(AuditActions.USER_EMAIL_VERIFIED, actor = user, target = user, ctx = ctx)
        }
    }

    /** Always succeeds from the caller's point of view: the response never reveals whether the email exists. */
    @Transactional
    fun resendVerification(email: String) {
        val user = users.findByEmail(normalizeEmail(email)) ?: return
        if (user.emailVerified || !user.enabled) return
        mailer.verifyEmail(user, userTokens.issue(user.id, TokenPurpose.EMAIL_VERIFICATION))
    }

    @Transactional
    fun forgotPassword(email: String) {
        val user = users.findByEmail(normalizeEmail(email)) ?: return
        if (!user.enabled) return
        mailer.passwordReset(user, userTokens.issue(user.id, TokenPurpose.PASSWORD_RESET))
    }

    /** Also accepts invitation tokens: accepting an invite is choosing a first password. */
    @Transactional
    fun resetPassword(request: ResetPasswordRequest, ctx: ClientContext): TokenResponse {
        val consumed = userTokens.consume(request.token, setOf(TokenPurpose.PASSWORD_RESET, TokenPurpose.INVITATION))
        val user = users.findById(consumed.userId).orElseThrow { BadRequestException("auth.token_invalid", "This link is invalid or has expired") }
        if (!user.enabled) throw ForbiddenException("auth.account_disabled", "This account has been disabled")

        val hadPassword = user.hasPassword
        user.passwordHash = passwordEncoder.encode(request.password)
        if (!user.emailVerified) user.emailVerifiedAt = clock.instant() // the link proved the address
        user.bumpSecurityVersion()
        user.lastLoginAt = clock.instant()
        sessions.revokeAll(user.id, "password_reset")
        userTokens.invalidateAll(user.id, listOf(TokenPurpose.PASSWORD_RESET, TokenPurpose.INVITATION))
        audit.record(AuditActions.USER_PASSWORD_RESET, actor = user, target = user, ctx = ctx, details = mapOf("purpose" to consumed.purpose.name))
        if (hadPassword) mailer.passwordChanged(user)
        return sessions.startSession(user, ctx)
    }

    private fun requireRegistrationOpen() {
        if (!settings.bool(Setting.REGISTRATION_ENABLED)) {
            throw ForbiddenException("auth.registration_disabled", "New registrations are currently closed")
        }
    }

    private fun invalidCredentials() = UnauthorizedException("auth.invalid_credentials", "Invalid email or password")

    companion object {
        fun normalizeEmail(email: String): String = email.trim().lowercase()
    }
}
