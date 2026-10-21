package tech.astro.liftoff.auth.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.auth.model.TokenPurpose
import tech.astro.liftoff.auth.model.UserToken
import tech.astro.liftoff.auth.repository.UserTokenRepository
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.BadRequestException
import tech.astro.liftoff.shared.security.Secrets
import java.time.Clock
import java.time.Duration
import java.util.UUID

/** Single-use email-link tokens: verification, password reset, invitation. */
@Service
class UserTokenService(
    private val tokens: UserTokenRepository,
    private val props: AppProperties,
    private val clock: Clock,
) {
    /** Returns the raw token (it only ever leaves in an email). Earlier unused tokens of the same purpose stop working. */
    @Transactional
    fun issue(userId: UUID, purpose: TokenPurpose): String {
        val now = clock.instant()
        invalidate(userId, listOf(purpose))
        val raw = Secrets.randomToken()
        tokens.save(UserToken(userId, purpose, Secrets.sha256Hex(raw), now, now.plus(ttl(purpose))))
        return raw
    }

    @Transactional
    fun consume(rawToken: String, purposes: Set<TokenPurpose>): UserToken {
        val now = clock.instant()
        val token = tokens.findByTokenHash(Secrets.sha256Hex(rawToken.trim()))
            ?.takeIf { it.purpose in purposes && it.usedAt == null && it.expiresAt.isAfter(now) }
            ?: throw BadRequestException("auth.token_invalid", "This link is invalid or has expired")
        token.usedAt = now
        return token
    }

    @Transactional
    fun invalidateAll(userId: UUID, purposes: Collection<TokenPurpose>) = invalidate(userId, purposes)

    private fun invalidate(userId: UUID, purposes: Collection<TokenPurpose>) {
        val now = clock.instant()
        tokens.findByUserIdAndPurposeInAndUsedAtIsNull(userId, purposes).forEach { it.usedAt = now }
    }

    private fun ttl(purpose: TokenPurpose): Duration = when (purpose) {
        TokenPurpose.EMAIL_VERIFICATION -> props.auth.emailVerificationTtl
        TokenPurpose.PASSWORD_RESET -> props.auth.passwordResetTtl
        TokenPurpose.INVITATION -> props.auth.invitationTtl
    }
}
