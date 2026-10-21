package tech.astro.liftoff.shared.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

/**
 * Writes the same problem+json shape as [GlobalExceptionHandler] from places that run
 * before Spring MVC (the security filter chain), so 401/403 look like every other error.
 */
@Component
class ProblemResponseWriter(private val jsonMapper: JsonMapper) {
    fun write(request: HttpServletRequest, response: HttpServletResponse, status: HttpStatus, code: String, detail: String) {
        if (response.isCommitted) return
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        val body = linkedMapOf<String, Any?>(
            "type" to "about:blank",
            "title" to status.reasonPhrase,
            "status" to status.value(),
            "detail" to detail,
            "instance" to request.requestURI,
            "code" to code,
            "timestamp" to Instant.now().toString(),
        )
        jsonMapper.writeValue(response.outputStream, body)
    }
}
