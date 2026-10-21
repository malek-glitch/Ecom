package tech.astro.liftoff.auth.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import tech.astro.liftoff.auth.model.RefreshToken
import tech.astro.liftoff.auth.model.TokenPurpose
import tech.astro.liftoff.auth.model.UserSession
import tech.astro.liftoff.auth.model.UserToken
import java.time.Instant
import java.util.UUID

interface UserSessionRepository : JpaRepository<UserSession, UUID> {
    fun findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(userId: UUID, now: Instant): List<UserSession>

    fun countByRevokedAtIsNullAndExpiresAtAfter(now: Instant): Long

    fun countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId: UUID, now: Instant): Long

    fun findByUserIdAndRevokedAtIsNull(userId: UUID): List<UserSession>

    /** Refresh tokens go with their session (ON DELETE CASCADE). */
    @Modifying
    @Query("delete from UserSession s where s.expiresAt < :before or s.revokedAt < :before")
    fun deleteStale(before: Instant): Int
}

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    /** Row lock so two simultaneous refreshes with the same token are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByTokenHash(tokenHash: String): RefreshToken?
}

interface UserTokenRepository : JpaRepository<UserToken, UUID> {
    fun findByTokenHash(tokenHash: String): UserToken?

    fun findByUserIdAndPurposeInAndUsedAtIsNull(userId: UUID, purposes: Collection<TokenPurpose>): List<UserToken>

    @Modifying
    @Query("delete from UserToken t where t.expiresAt < :before or t.usedAt < :before")
    fun deleteStale(before: Instant): Int
}
