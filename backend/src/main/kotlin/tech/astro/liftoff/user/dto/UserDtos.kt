package tech.astro.liftoff.user.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import tech.astro.liftoff.auth.service.ValidPassword
import tech.astro.liftoff.shared.i18n.SupportedLocale
import tech.astro.liftoff.user.model.Role
import java.time.Instant
import java.util.UUID

data class MeResponse(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: Role,
    val locale: String,
    val avatarUrl: String?,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    val googleLinked: Boolean,
    val createdAt: Instant,
)

data class UpdateProfileRequest(
    @field:Size(max = 120)
    @field:Pattern(regexp = ".*\\S.*", message = "must not be blank")
    val fullName: String? = null,
    @field:SupportedLocale
    val locale: String? = null,
)

data class ChangePasswordRequest(
    /** Required when the account already has a password; Google-only accounts can set one without it. */
    val currentPassword: String? = null,
    @field:ValidPassword
    val newPassword: String = "",
)

data class DeleteAccountRequest(
    /** Required when the account has a password. */
    val password: String? = null,
    /** Required for accounts without a password: must equal the account email. */
    val confirmEmail: String? = null,
)

data class SessionResponse(
    val id: UUID,
    val createdAt: Instant,
    val lastUsedAt: Instant,
    val expiresAt: Instant,
    val userAgent: String?,
    val ipAddress: String?,
    val current: Boolean,
)
