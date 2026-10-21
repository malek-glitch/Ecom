package tech.astro.liftoff.auth.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.auth.dto.TokenResponse
import tech.astro.liftoff.auth.model.RefreshToken
import tech.astro.liftoff.auth.model.UserSession
import tech.astro.liftoff.auth.repository.RefreshTokenRepository
import tech.astro.liftoff.auth.repository.UserSessionRepository
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.ForbiddenException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.error.UnauthorizedException
import tech.astro.liftoff.shared.security.Secrets
import tech.astro.liftoff.shared.security.UserSecurityStateCache
import tech.astro.liftoff.shared.web.ClientContext
import tech.astro.liftoff.user.dto.SessionResponse
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.repository.UserRepository
import tech.astro.liftoff.user.service.UserMapper
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Sessions and rotating refresh tokens.
 *
 * Every refresh exchanges the presented token for a new one. Presenting an already-rotated
 * token again means two parties hold the same token, so the whole session is revoked,
 * unless it happens within `refresh-reuse-grace` (two tabs refreshing at the same moment).
 */
@Service
class SessionService(
    private val sessions: UserSessionRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val users: UserRepository,
    private val tokens: TokenService,
    private val mapper: UserMapper,
    private val audit: AuditService,
    private val stateCache: UserSecurityStateCache,
    private val props: AppProperties,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun startSession(user: User, ctx: ClientContext): TokenResponse {
        val now = clock.instant()
        val session = sessions.save(
            UserSession(
                userId = user.id,
                createdAt = now,
                lastUsedAt = now,
                expiresAt = now.plus(props.auth.refreshTokenTtl),
                userAgent = ctx.userAgent,
                ipAddress = ctx.ipAddress,
            ),
        )
        return issue(user, session, now)
    }

    /** Commits the revocation even when it throws, so a detected theft isn't rolled back. */
    @Transactional(noRollbackFor = [UnauthorizedException::class, ForbiddenException::class])
    fun refresh(rawToken: String, ctx: ClientContext): TokenResponse {
        val now = clock.instant()
        val token = refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken.trim())) ?: throw invalidRefresh()
        val session = sessions.findById(token.sessionId).orElseThrow { invalidRefresh() }
        if (!session.isActive(now)) throw invalidRefresh()

        token.rotatedAt?.let { rotatedAt ->
            if (Duration.between(rotatedAt, now) >= props.auth.refreshReuseGrace) {
                session.revoke(now, "reuse_detected")
                log.warn("Refresh token reuse detected for session {} (user {})", session.id, session.userId)
                audit.record(AuditActions.REFRESH_REUSE_DETECTED, actorId = session.userId, ctx = ctx)
                throw UnauthorizedException("auth.refresh_reused", "This session was revoked for safety. Please sign in again.")
            }
        }

        val user = users.findById(session.userId).orElseThrow { invalidRefresh() }
        if (!user.enabled) {
            session.revoke(now, "account_disabled")
            throw ForbiddenException("auth.account_disabled", "This account has been disabled")
        }
        if (token.rotatedAt == null) token.rotatedAt = now
        session.lastUsedAt = now
        session.expiresAt = now.plus(props.auth.refreshTokenTtl)
        session.ipAddress = ctx.ipAddress
        if (ctx.userAgent != null) session.userAgent = ctx.userAgent
        return issue(user, session, now)
    }

    /** Idempotent: unknown or already-revoked tokens are ignored. */
    @Transactional
    fun revokeByRefreshToken(rawToken: String, reason: String) {
        val token = refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken.trim())) ?: return
        sessions.findById(token.sessionId).ifPresent { it.revoke(clock.instant(), reason) }
    }

    @Transactional(readOnly = true)
    fun listActive(userId: UUID, currentSessionId: UUID?): List<SessionResponse> =
        sessions.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(userId, clock.instant()).map {
            SessionResponse(it.id, it.createdAt, it.lastUsedAt, it.expiresAt, it.userAgent, it.ipAddress, it.id == currentSessionId)
        }

    @Transactional
    fun revoke(userId: UUID, sessionId: UUID, reason: String) {
        val session = sessions.findById(sessionId).filter { it.userId == userId }
            .orElseThrow { NotFoundException("session.not_found", "Session not found") }
        session.revoke(clock.instant(), reason)
    }

    /**
     * Revokes refresh tokens only; pair with a security-version bump to kill access tokens too.
     * Entity by entity rather than a bulk UPDATE: a user has a handful of sessions, and a bulk
     * query would have to clear the persistence context, silently detaching (and dropping
     * unsaved changes to) entities the caller is still modifying.
     */
    @Transactional
    fun revokeAll(userId: UUID, reason: String, keep: UUID? = null): Int {
        val now = clock.instant()
        val targets = sessions.findByUserIdAndRevokedAtIsNull(userId).filter { it.id != keep }
        targets.forEach { it.revoke(now, reason) }
        stateCache.evict(userId)
        return targets.size
    }

    @Transactional(readOnly = true)
    fun countActive(userId: UUID): Long = sessions.countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId, clock.instant())

    private fun issue(user: User, session: UserSession, now: Instant): TokenResponse {
        val raw = Secrets.randomToken()
        refreshTokens.save(RefreshToken(sessionId = session.id, tokenHash = Secrets.sha256Hex(raw), createdAt = now))
        val access = tokens.issueAccessToken(user, session.id, now)
        return TokenResponse(
            accessToken = access.value,
            accessTokenExpiresAt = access.expiresAt,
            refreshToken = raw,
            refreshTokenExpiresAt = session.expiresAt,
            user = mapper.toMe(user),
        )
    }

    private fun invalidRefresh() = UnauthorizedException("auth.refresh_invalid", "The session has expired. Please sign in again.")
}
