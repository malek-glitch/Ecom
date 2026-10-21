package tech.astro.liftoff.shared

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import tech.astro.liftoff.support.IntegrationTest

@TestPropertySource(
    properties = [
        "app.rate-limit.enabled=true",
        "app.rate-limit.policies.login.capacity=2",
        "app.rate-limit.policies.login.period=1h",
    ],
)
class RateLimitIntegrationTest : IntegrationTest() {

    @Test
    fun `repeated login attempts are throttled`() {
        val attempt = mapOf("email" to uniqueEmail(), "password" to "whatever-password")
        repeat(2) { postJson("/api/v1/auth/login", attempt).andExpect { status { isUnauthorized() } } }
        postJson("/api/v1/auth/login", attempt).andExpect {
            status { isTooManyRequests() }
            header { exists(HttpHeaders.RETRY_AFTER) }
            jsonPath("$.code") { value("request.rate_limited") }
        }
    }
}
