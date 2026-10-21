package tech.astro.liftoff.shared.config

import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import tech.astro.liftoff.shared.security.ProblemAccessDeniedHandler
import tech.astro.liftoff.shared.security.ProblemAuthenticationEntryPoint

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        entryPoint: ProblemAuthenticationEntryPoint,
        deniedHandler: ProblemAccessDeniedHandler,
    ): SecurityFilterChain {
        http {
            csrf { disable() } // bearer tokens only: no cookies, nothing to forge
            cors { }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                // async/error re-dispatches were already authorized on the original request
                authorize(DispatcherTypeRequestMatcher(DispatcherType.ERROR), permitAll)
                authorize(DispatcherTypeRequestMatcher(DispatcherType.ASYNC), permitAll)
                authorize(HttpMethod.OPTIONS, "/**", permitAll)
                PUBLIC_PATTERNS.forEach { authorize(it, permitAll) }
                authorize("/api/v1/admin/**", hasRole("ADMIN"))
                authorize("/actuator/**", hasRole("ADMIN"))
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                bearerTokenResolver = PublicPathAwareBearerTokenResolver(PUBLIC_MATCHER)
                authenticationEntryPoint = entryPoint
                accessDeniedHandler = deniedHandler
                jwt { this.jwtAuthenticationConverter = jwtAuthenticationConverter }
            }
            exceptionHandling {
                authenticationEntryPoint = entryPoint
                accessDeniedHandler = deniedHandler
            }
        }
        return http.build()
    }

    /** ADMIN implies USER, for URL rules and @PreAuthorize alike. */
    @Bean
    fun roleHierarchy(): RoleHierarchy = RoleHierarchyImpl.withDefaultRolePrefix().role("ADMIN").implies("USER").build()

    /** Stores `{bcrypt}…` hashes, so the algorithm can be upgraded later without breaking old ones. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    companion object {
        val PUBLIC_PATTERNS = listOf(
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/api/v1/files/public/**",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
        )

        private val PUBLIC_MATCHER: RequestMatcher =
            OrRequestMatcher(PUBLIC_PATTERNS.map { PathPatternRequestMatcher.withDefaults().matcher(it) })
    }
}

/**
 * Ignores the Authorization header on public endpoints. Otherwise a stale token left in the
 * browser would turn `POST /auth/login` or `GET /public/config` into a 401.
 */
class PublicPathAwareBearerTokenResolver(private val publicMatcher: RequestMatcher) : BearerTokenResolver {
    private val delegate = DefaultBearerTokenResolver()

    override fun resolve(request: HttpServletRequest): String? =
        if (publicMatcher.matches(request)) null else delegate.resolve(request)
}
