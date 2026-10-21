package tech.astro.liftoff.shared.security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Turns stateless access tokens into revocable ones: a token is rejected when its user
 * is gone or disabled, or when its `sv` claim is older than the user's security version.
 */
@Component
class UserSecurityStateValidator(private val cache: UserSecurityStateCache) : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val userId = runCatching { UUID.fromString(token.subject) }.getOrNull() ?: return fail("Invalid subject")
        val state = cache.get(userId) ?: return fail("Unknown user")
        if (!state.enabled) return fail("Account disabled")
        val version = (token.claims[JwtClaims.SECURITY_VERSION] as? Number)?.toInt() ?: return fail("Missing security version")
        if (version != state.securityVersion) return fail("Token revoked")
        return OAuth2TokenValidatorResult.success()
    }

    private fun fail(description: String) =
        OAuth2TokenValidatorResult.failure(OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null))
}
