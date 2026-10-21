package tech.astro.liftoff.shared.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * Every `app.*` setting, bound once at startup and validated, so a missing or weak
 * secret stops the boot instead of failing on the first request.
 * Each value maps to an environment variable in `application.yml`; see docs/CONFIGURATION.md.
 */
@Validated
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    @field:NotBlank val name: String = "Liftoff",
    /** Public URL of the web app. Used to build links in emails. */
    @field:NotBlank val frontendUrl: String = "http://localhost:5173",
    @field:Valid val auth: Auth = Auth(),
    @field:Valid val mail: Mail = Mail(),
    val cors: Cors = Cors(),
    val google: Google = Google(),
    val admin: Admin = Admin(),
    val storage: Storage = Storage(),
    val rateLimit: RateLimit = RateLimit(),
    val audit: Audit = Audit(),
    val shop: Shop = Shop(),
) {
    data class Auth(
        /** HMAC key for access tokens, 32+ characters. Rotating it is transparent: clients refresh (sessions live in the database). */
        @field:Size(min = 32, message = "must be at least 32 characters (set JWT_SECRET)")
        val jwtSecret: String = "",
        val issuer: String = "liftoff",
        val accessTokenTtl: Duration = Duration.ofMinutes(15),
        /** Sliding: every refresh pushes the session's expiry this far into the future. */
        val refreshTokenTtl: Duration = Duration.ofDays(30),
        /** A rotated refresh token may be replayed this long before it counts as theft (two tabs racing). */
        val refreshReuseGrace: Duration = Duration.ofSeconds(10),
        val requireVerifiedEmail: Boolean = true,
        val emailVerificationTtl: Duration = Duration.ofHours(48),
        val passwordResetTtl: Duration = Duration.ofHours(1),
        val invitationTtl: Duration = Duration.ofDays(7),
    )

    data class Mail(
        @field:NotBlank val from: String = "Liftoff <no-reply@liftoff.local>",
        /** false sends inline on the calling thread (tests); true hands mail to a small background pool. */
        val async: Boolean = true,
    )

    data class Cors(
        /** Only needed when the browser calls the API from another origin (Capacitor, separate API domain). */
        val allowedOrigins: List<String> = emptyList(),
    )

    data class Google(val clientId: String = "") {
        val enabled: Boolean get() = clientId.isNotBlank()
    }

    /** First-admin bootstrap: applied only while no ADMIN exists. */
    data class Admin(val email: String = "", val password: String = "")

    data class Storage(
        val enabled: Boolean = false,
        val endpoint: String = "",
        val region: String = "us-east-1",
        val bucket: String = "liftoff",
        val accessKey: String = "",
        val secretKey: String = "",
        /** Empty: files are streamed through the API (`/api/v1/files/public/...`). Set it to serve them from a CDN or public bucket. */
        val publicBaseUrl: String = "",
        val createBucket: Boolean = true,
        val maxAvatarSize: DataSize = DataSize.ofMegabytes(5),
        val maxProductImageSize: DataSize = DataSize.ofMegabytes(8),
    )

    data class RateLimit(
        val enabled: Boolean = true,
        val policies: Map<String, Policy> = emptyMap(),
    ) {
        data class Policy(val capacity: Long, val period: Duration)
    }

    data class Audit(val retention: Duration = Duration.ofDays(365))

    data class Shop(
        /** Fills an empty catalog with demo categories and products at startup (development and demos). */
        val seedDemo: Boolean = false,
        /** Products at or below this stock count show up as "low stock" for admins and customers. */
        val lowStockThreshold: Int = 5,
    )
}
