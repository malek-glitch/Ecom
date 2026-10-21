package tech.astro.liftoff.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant
import java.util.UUID

/**
 * Only the SHA-256 of the token is stored. Rotated tokens are kept until their session
 * expires: presenting one again (after the grace window) is how token theft is detected.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Column(name = "session_id", nullable = false, updatable = false)
    val sessionId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    val tokenHash: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "rotated_at")
    var rotatedAt: Instant? = null,
) : BaseEntity()
