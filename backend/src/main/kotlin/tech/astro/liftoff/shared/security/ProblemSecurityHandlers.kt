package tech.astro.liftoff.shared.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tech.astro.liftoff.shared.error.ProblemResponseWriter

/**
 * 401 with a problem+json body. `auth.invalid_token` (expired/revoked token) tells the client
 * to try its refresh token; `auth.unauthorized` (no token at all) means "log in".
 */
@Component
class ProblemAuthenticationEntryPoint(private val writer: ProblemResponseWriter) : AuthenticationEntryPoint {
    private val bearer = BearerTokenAuthenticationEntryPoint()

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, ex: AuthenticationException) {
        bearer.commence(request, response, ex) // sets 401 + WWW-Authenticate
        if (ex is InvalidBearerTokenException) {
            writer.write(request, response, HttpStatus.UNAUTHORIZED, "auth.invalid_token", "The access token is invalid or expired")
        } else {
            writer.write(request, response, HttpStatus.UNAUTHORIZED, "auth.unauthorized", "Authentication required")
        }
    }
}

@Component
class ProblemAccessDeniedHandler(private val writer: ProblemResponseWriter) : AccessDeniedHandler {
    override fun handle(request: HttpServletRequest, response: HttpServletResponse, ex: AccessDeniedException) {
        writer.write(request, response, HttpStatus.FORBIDDEN, "auth.forbidden", "You are not allowed to do this")
    }
}
