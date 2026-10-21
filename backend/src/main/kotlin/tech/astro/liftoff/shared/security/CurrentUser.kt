package tech.astro.liftoff.shared.security

import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import tech.astro.liftoff.shared.error.UnauthorizedException
import tech.astro.liftoff.user.model.Role
import java.util.UUID

/**
 * The authenticated caller, read from the verified access token.
 * Declare `user: CurrentUser` on a controller method to receive it.
 */
data class CurrentUser(val id: UUID, val role: Role, val sessionId: UUID?) {
    val isAdmin: Boolean get() = role == Role.ADMIN

    companion object {
        fun fromSecurityContext(): CurrentUser {
            val auth = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
                ?: throw UnauthorizedException()
            val jwt = auth.token
            val subject = jwt.subject ?: throw UnauthorizedException()
            val role = jwt.getClaimAsString(JwtClaims.ROLE) ?: throw UnauthorizedException()
            return CurrentUser(
                id = UUID.fromString(subject),
                role = Role.valueOf(role),
                sessionId = jwt.getClaimAsString(JwtClaims.SESSION)?.let(UUID::fromString),
            )
        }
    }
}

class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == CurrentUser::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): CurrentUser = CurrentUser.fromSecurityContext()
}

object JwtClaims {
    const val ROLE = "role"
    const val SECURITY_VERSION = "sv"
    const val SESSION = "sid"
}
