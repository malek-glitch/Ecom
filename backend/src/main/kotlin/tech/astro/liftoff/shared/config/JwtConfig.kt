package tech.astro.liftoff.shared.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import tech.astro.liftoff.shared.security.JwtClaims
import tech.astro.liftoff.shared.security.UserSecurityStateValidator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * HS256 access tokens via Spring Security's resource server: no hand-written JWT filter.
 * The decoder checks signature, expiry and issuer, then [UserSecurityStateValidator]
 * rejects tokens of disabled users or with a stale security version.
 */
@Configuration(proxyBeanMethods = false)
class JwtConfig(props: AppProperties) {

    private val key: SecretKey = SecretKeySpec(props.auth.jwtSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
    private val issuer = props.auth.issuer

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))

    @Bean
    fun jwtDecoder(stateValidator: UserSecurityStateValidator): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().apply {
            setJwtValidator(DelegatingOAuth2TokenValidator(JwtValidators.createDefaultWithIssuer(issuer), stateValidator))
        }

    /** `role` claim → `ROLE_<role>` authority; the principal name is the user id (`sub`). */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val authorities = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName(JwtClaims.ROLE)
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply { setJwtGrantedAuthoritiesConverter(authorities) }
    }
}
