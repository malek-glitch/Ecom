package tech.astro.liftoff.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tech.astro.liftoff.shared.security.CurrentUserArgumentResolver
import tech.astro.liftoff.shared.security.RateLimitInterceptor
import tech.astro.liftoff.shared.web.ClientContextArgumentResolver

@Configuration(proxyBeanMethods = false)
class WebConfig(
    private val props: AppProperties,
    private val rateLimitInterceptor: RateLimitInterceptor,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers += CurrentUserArgumentResolver()
        resolvers += ClientContextArgumentResolver()
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**")
    }

    /**
     * In the default deployment the browser reaches the API on the same origin (Vite proxy in
     * dev, Traefik in prod), so no origin is allowed. Add origins only for Capacitor shells or
     * a separate API domain. No credentials: auth travels in the Authorization header.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = props.cors.allowedOrigins.map(String::trim).filter(String::isNotEmpty)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "Accept-Language", "X-Request-Id")
            exposedHeaders = listOf("Retry-After", "X-Request-Id")
            allowCredentials = false
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", config) }
    }
}
