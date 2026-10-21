package tech.astro.liftoff.auth.service

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.UnauthorizedException

data class GoogleIdentity(val subject: String, val email: String, val emailVerified: Boolean, val name: String?)

fun interface GoogleTokenVerifier {
    fun verify(idToken: String): GoogleIdentity
}

/**
 * Verifies a Google Identity Services ID token against Google's published keys: signature,
 * expiry, issuer, and that it was minted for our client id. No Google SDK needed: Spring
 * Security's Nimbus decoder already does JWKS. Replaced by a fake in tests.
 */
@Component
class GoogleJwksTokenVerifier(props: AppProperties) : GoogleTokenVerifier {

    private val clientId = props.google.clientId

    private val decoder: JwtDecoder by lazy {
        NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build().apply {
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtTimestampValidator(),
                    JwtClaimValidator<String>("iss") { it in ISSUERS },
                    JwtClaimValidator<Collection<String>>("aud") { clientId in it },
                ),
            )
        }
    }

    override fun verify(idToken: String): GoogleIdentity {
        val jwt = try {
            decoder.decode(idToken)
        } catch (e: JwtException) {
            throw UnauthorizedException("auth.google_invalid", "Google sign-in could not be verified")
        }
        val email = jwt.getClaimAsString("email") ?: throw UnauthorizedException("auth.google_invalid", "Google did not share an email address")
        val verified = when (val claim = jwt.claims["email_verified"]) {
            is Boolean -> claim
            is String -> claim.toBoolean()
            else -> false
        }
        val subject = jwt.subject ?: throw UnauthorizedException("auth.google_invalid", "Google sign-in could not be verified")
        return GoogleIdentity(subject, email, verified, jwt.getClaimAsString("name"))
    }

    private companion object {
        const val JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs"
        val ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
    }
}
