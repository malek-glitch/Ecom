package tech.astro.liftoff.shared

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tech.astro.liftoff.support.IntegrationTest

/** Every error reaches the client as RFC 9457 problem+json with a stable `code`. */
class ErrorContractIntegrationTest : IntegrationTest() {

    @Test
    fun `missing token is a problem json 401`() {
        mvc.get("/api/v1/me").andExpect {
            status { isUnauthorized() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
            header { exists(HttpHeaders.WWW_AUTHENTICATE) }
            jsonPath("$.status") { value(401) }
            jsonPath("$.code") { value("auth.unauthorized") }
            jsonPath("$.instance") { value("/api/v1/me") }
        }
    }

    @Test
    fun `an invalid token tells the client to refresh`() {
        getAuthed("/api/v1/me", "not-a-jwt").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("auth.invalid_token") }
        }
    }

    @Test
    fun `public endpoints ignore a stale token`() {
        getAuthed("/api/v1/public/config", "stale-token").andExpect {
            status { isOk() }
            jsonPath("$.appName") { value("Liftoff") }
            jsonPath("$.googleClientId") { value("test-google-client-id") }
            jsonPath("$.locales.length()") { value(3) }
            jsonPath("$.settings['auth.registrationEnabled']") { value(true) }
        }
    }

    @Test
    fun `validation errors are 422 with a code per field`() {
        postJson("/api/v1/auth/register", mapOf("email" to "not-an-email", "password" to "short", "fullName" to "", "locale" to "de"))
            .andExpect {
                status { isEqualTo(422) }
                jsonPath("$.code") { value("request.validation") }
                jsonPath("$.fields.email.code") { value("Email") }
                jsonPath("$.fields.password.code") { value("ValidPassword") }
                jsonPath("$.fields.fullName.code") { value("NotBlank") }
                jsonPath("$.fields.locale.code") { value("SupportedLocale") }
            }
    }

    @Test
    fun `missing fields are validation errors, not server errors`() {
        postJson("/api/v1/auth/login", emptyMap<String, String>()).andExpect {
            status { isEqualTo(422) }
            jsonPath("$.fields.email.code") { value("NotBlank") }
        }
    }

    @Test
    fun `malformed json is a 400`() {
        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = "{nope"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("request.malformed") }
        }
    }

    @Test
    fun `unknown routes are 404 for signed-in users`() {
        val session = signUp()
        getAuthed("/api/v1/does-not-exist", session.accessToken).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("request.not_found") }
        }
    }

    @Test
    fun `users cannot reach admin endpoints`() {
        val session = signUp()
        getAuthed("/api/v1/admin/users", session.accessToken).andExpect {
            status { isForbidden() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.code") { value("auth.forbidden") }
        }
    }

    @Test
    fun `health probes are public`() {
        mvc.get("/actuator/health/liveness").andExpect { status { isOk() } }
        mvc.get("/actuator/health/readiness").andExpect { status { isOk() } }
    }
}
