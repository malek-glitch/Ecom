package tech.astro.liftoff.shared.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.web.ClientContext

/**
 * Swagger UI at /swagger-ui.html, spec at /v3/api-docs (both off in the prod profile).
 * The frontend generates its TypeScript types from this spec: `npm run gen:api`.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig(private val props: AppProperties) {

    init {
        // Resolved by argument resolvers, not by the client.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser::class.java, ClientContext::class.java)
    }

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(Info().title("${props.name} API").version("v1"))
        .components(
            Components().addSecuritySchemes(
                BEARER,
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList(BEARER))

    companion object {
        const val BEARER = "bearer"
    }
}
