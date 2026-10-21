package tech.astro.liftoff.auth.service

import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.persistence.UuidV7
import tech.astro.liftoff.shared.security.JwtClaims
import tech.astro.liftoff.user.model.User
import java.time.Instant
import java.util.UUID

data class IssuedToken(val value: String, val expiresAt: Instant)

/** Short-lived access tokens. Long-lived state (sessions, refresh tokens) lives in the database. */
@Service
class TokenService(private val encoder: JwtEncoder, private val props: AppProperties) {

    fun issueAccessToken(user: User, sessionId: UUID, now: Instant): IssuedToken {
        val expiresAt = now.plus(props.auth.accessTokenTtl)
        val claims = JwtClaimsSet.builder()
            .issuer(props.auth.issuer)
            .subject(user.id.toString())
            .id(UuidV7.generate().toString())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim(JwtClaims.ROLE, user.role.name)
            .claim(JwtClaims.SECURITY_VERSION, user.securityVersion)
            .claim(JwtClaims.SESSION, sessionId.toString())
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return IssuedToken(encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue, expiresAt)
    }
}
