package tech.astro.liftoff.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant
import java.util.UUID

enum class TokenPurpose { EMAIL_VERIFICATION, PASSWORD_RESET, INVITATION }

/** Single-use, expiring token sent by email. Stored hashed, like refresh tokens. */
@Entity
@Table(name = "user_tokens")
class UserToken(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32, updatable = false)
    val purpose: TokenPurpose,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    val tokenHash: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,
) : BaseEntity()
