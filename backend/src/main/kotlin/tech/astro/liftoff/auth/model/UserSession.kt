package tech.astro.liftoff.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant
import java.util.UUID

/**
 * One signed-in device. Its refresh tokens rotate on every use; the session itself is what
 * "Active sessions" lists and what gets revoked. Expiry slides forward on each refresh.
 */
@Entity
@Table(name = "user_sessions")
class UserSession(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Instant,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "user_agent", length = 512)
    var userAgent: String?,

    @Column(name = "ip_address", length = 64)
    var ipAddress: String?,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "revoked_reason", length = 40)
    var revokedReason: String? = null,
) : BaseEntity() {

    fun isActive(now: Instant): Boolean = revokedAt == null && expiresAt.isAfter(now)

    fun revoke(now: Instant, reason: String) {
        if (revokedAt == null) {
            revokedAt = now
            revokedReason = reason
        }
    }
}
