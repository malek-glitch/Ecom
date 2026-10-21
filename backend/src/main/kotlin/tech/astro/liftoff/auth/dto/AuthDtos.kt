package tech.astro.liftoff.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import tech.astro.liftoff.auth.service.ValidPassword
import tech.astro.liftoff.shared.i18n.SupportedLocale
import tech.astro.liftoff.user.dto.MeResponse
import java.time.Instant

// Request fields default to "" rather than being nullable: a missing field then fails
// Bean Validation with a 422 field error instead of a 400 from JSON binding.

data class RegisterRequest(
    @field:NotBlank @field:Email @field:Size(max = 254)
    val email: String = "",
    @field:ValidPassword
    val password: String = "",
    @field:NotBlank @field:Size(max = 120)
    val fullName: String = "",
    @field:SupportedLocale
    val locale: String? = null,
)

data class LoginRequest(
    @field:NotBlank @field:Email
    val email: String = "",
    @field:NotBlank
    val password: String = "",
)

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String = "",
)

data class GoogleLoginRequest(
    /** The ID token (JWT) that Google Identity Services hands the browser. */
    @field:NotBlank
    val idToken: String = "",
    @field:SupportedLocale
    val locale: String? = null,
)

data class EmailTokenRequest(
    @field:NotBlank
    val token: String = "",
)

data class EmailRequest(
    @field:NotBlank @field:Email
    val email: String = "",
)

data class ResetPasswordRequest(
    @field:NotBlank
    val token: String = "",
    @field:ValidPassword
    val password: String = "",
)

data class TokenResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val tokenType: String = "Bearer",
    val user: MeResponse,
)

/** When email verification is required the account is created but no session is issued yet. */
data class RegisterResponse(
    val verificationRequired: Boolean,
    val session: TokenResponse?,
)
