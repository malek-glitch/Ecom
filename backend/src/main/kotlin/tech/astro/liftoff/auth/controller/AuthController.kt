package tech.astro.liftoff.auth.controller

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.auth.dto.EmailRequest
import tech.astro.liftoff.auth.dto.EmailTokenRequest
import tech.astro.liftoff.auth.dto.GoogleLoginRequest
import tech.astro.liftoff.auth.dto.LoginRequest
import tech.astro.liftoff.auth.dto.RefreshRequest
import tech.astro.liftoff.auth.dto.RegisterRequest
import tech.astro.liftoff.auth.dto.RegisterResponse
import tech.astro.liftoff.auth.dto.ResetPasswordRequest
import tech.astro.liftoff.auth.dto.TokenResponse
import tech.astro.liftoff.auth.service.AuthService
import tech.astro.liftoff.shared.security.RateLimited
import tech.astro.liftoff.shared.web.ClientContext

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
@SecurityRequirements // public endpoints
class AuthController(private val auth: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimited("register")
    fun register(@Valid @RequestBody request: RegisterRequest, ctx: ClientContext): RegisterResponse =
        auth.register(request, ctx)

    @PostMapping("/login")
    @RateLimited("login")
    fun login(@Valid @RequestBody request: LoginRequest, ctx: ClientContext): TokenResponse =
        auth.login(request, ctx)

    /** Exchanges a refresh token for a new pair. The presented token stops working. */
    @PostMapping("/refresh")
    @RateLimited("refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest, ctx: ClientContext): TokenResponse =
        auth.refresh(request.refreshToken, ctx)

    /** Revokes the session the refresh token belongs to. Idempotent. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshRequest) = auth.logout(request.refreshToken)

    @PostMapping("/google")
    @RateLimited("google")
    fun google(@Valid @RequestBody request: GoogleLoginRequest, ctx: ClientContext): TokenResponse =
        auth.loginWithGoogle(request, ctx)

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RateLimited("login")
    fun verifyEmail(@Valid @RequestBody request: EmailTokenRequest, ctx: ClientContext) =
        auth.verifyEmail(request.token, ctx)

    @PostMapping("/verify-email/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimited("email")
    fun resendVerification(@Valid @RequestBody request: EmailRequest) = auth.resendVerification(request.email)

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimited("email")
    fun forgotPassword(@Valid @RequestBody request: EmailRequest) = auth.forgotPassword(request.email)

    /** Sets a new password from an emailed reset or invitation link, and signs in. */
    @PostMapping("/password/reset")
    @RateLimited("login")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest, ctx: ClientContext): TokenResponse =
        auth.resetPassword(request, ctx)
}
