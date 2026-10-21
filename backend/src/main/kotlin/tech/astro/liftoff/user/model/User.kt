package tech.astro.liftoff.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    /** citext: unique and compared case-insensitively by Postgres. Still normalised to lower case on input. */
    @Column(name = "email", nullable = false, unique = true, columnDefinition = "citext")
    var email: String,

    @Column(name = "full_name", nullable = false, length = 120)
    var fullName: String,

    /** Null for accounts that only sign in with Google, or invited users who haven't set one yet. */
    @Column(name = "password_hash")
    var passwordHash: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: Role = Role.USER,

    /** Preferred language (fr/en/ar): picks the email templates and is synced from the UI. */
    @Column(name = "locale", nullable = false, length = 8)
    var locale: String = "fr",

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    @Column(name = "email_verified_at")
    var emailVerifiedAt: Instant? = null,

    @Column(name = "google_sub", unique = true)
    var googleSub: String? = null,

    /** Object-storage key of the avatar (only when storage is enabled). */
    @Column(name = "avatar_key", length = 512)
    var avatarKey: String? = null,

    /**
     * Embedded in access tokens as `sv`. Bumping it makes every outstanding access token
     * fail validation within ~30s (role change, disable, password change, "log out everywhere").
     */
    @Column(name = "security_version", nullable = false)
    var securityVersion: Int = 0,

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,
) : BaseEntity() {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    val emailVerified: Boolean get() = emailVerifiedAt != null
    val hasPassword: Boolean get() = passwordHash != null

    fun bumpSecurityVersion() {
        securityVersion += 1
    }
}
