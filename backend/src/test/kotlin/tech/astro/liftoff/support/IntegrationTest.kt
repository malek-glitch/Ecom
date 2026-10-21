package tech.astro.liftoff.support

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tech.astro.liftoff.auth.dto.RegisterResponse
import tech.astro.liftoff.auth.dto.TokenResponse
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

/**
 * Base class for API tests: the whole app on a real Postgres (Testcontainers), driven
 * through MockMvc, with captured emails and a fake Google verifier.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, TestDoubles::class)
abstract class IntegrationTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var json: JsonMapper

    @Autowired
    lateinit var mail: CapturingMailGateway

    @Autowired
    lateinit var google: FakeGoogleTokenVerifier

    fun uniqueEmail(prefix: String = "user") = "$prefix-${UUID.randomUUID().toString().take(8)}@liftoff.test"

    fun body(value: Any): String = json.writeValueAsString(value)

    fun postJson(path: String, payload: Any, token: String? = null): ResultActionsDsl = mvc.post(path) {
        contentType = MediaType.APPLICATION_JSON
        content = body(payload)
        token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
    }

    fun putJson(path: String, payload: Any, token: String? = null): ResultActionsDsl = mvc.put(path) {
        contentType = MediaType.APPLICATION_JSON
        content = body(payload)
        token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
    }

    fun patchJson(path: String, payload: Any, token: String? = null): ResultActionsDsl = mvc.patch(path) {
        contentType = MediaType.APPLICATION_JSON
        content = body(payload)
        token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
    }

    fun getAuthed(path: String, token: String?): ResultActionsDsl = mvc.get(path) {
        token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
    }

    fun deleteAuthed(path: String, token: String?, payload: Any? = null): ResultActionsDsl = mvc.delete(path) {
        token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
        payload?.let {
            contentType = MediaType.APPLICATION_JSON
            content = body(it)
        }
    }

    fun ResultActionsDsl.bodyJson(): JsonNode = json.readTree(andReturn().response.contentAsString)

    fun <T : Any> ResultActionsDsl.bodyAs(type: Class<T>): T = json.readValue(andReturn().response.contentAsString, type)

    /** Registers and verifies an account, then signs in. */
    fun signUp(email: String = uniqueEmail(), password: String = PASSWORD, fullName: String = "Test User"): TokenResponse {
        postJson("/api/v1/auth/register", mapOf("email" to email, "password" to password, "fullName" to fullName, "locale" to "en"))
            .andExpect { status { isCreated() } }
            .bodyAs(RegisterResponse::class.java)
        postJson("/api/v1/auth/verify-email", mapOf("token" to mail.tokenFor(email))).andExpect { status { isNoContent() } }
        return login(email, password)
    }

    fun login(email: String, password: String = PASSWORD): TokenResponse =
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to password))
            .andExpect { status { isOk() } }
            .bodyAs(TokenResponse::class.java)

    fun adminLogin(): TokenResponse = login(ADMIN_EMAIL, ADMIN_PASSWORD)

    companion object {
        const val PASSWORD = "correct-horse-battery"
        const val ADMIN_EMAIL = "admin@liftoff.test"
        const val ADMIN_PASSWORD = "Admin-Password-1"
    }
}
