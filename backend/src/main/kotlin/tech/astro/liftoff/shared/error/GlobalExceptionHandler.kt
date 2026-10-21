package tech.astro.liftoff.shared.error

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI
import java.time.Instant

/**
 * Every error leaves the API as RFC 9457 `application/problem+json`:
 * `{ type, title, status, detail, instance, code, fields?, timestamp }`.
 * Spring MVC's own exceptions (bad JSON, wrong method, oversized upload...) keep their
 * correct status via [ResponseEntityExceptionHandler] instead of turning into 500s.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(AppException::class)
    fun handleApp(ex: AppException, request: WebRequest): ResponseEntity<Any> {
        val body = problem(ex.status, ex.message, ex.code, request)
        ex.fields?.let { body.setProperty("fields", it) }
        val headers = HttpHeaders()
        if (ex is TooManyRequestsException) headers.set(HttpHeaders.RETRY_AFTER, ex.retryAfterSeconds.toString())
        return ResponseEntity.status(ex.status).headers(headers).body(body)
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleStale(ex: ObjectOptimisticLockingFailureException, request: WebRequest): ResponseEntity<Any> =
        respond(HttpStatus.CONFLICT, "conflict.stale", "This record was changed by someone else. Reload and try again.", request)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleIntegrity(ex: DataIntegrityViolationException, request: WebRequest): ResponseEntity<Any> {
        log.warn("Integrity violation on {}: {}", path(request), ex.mostSpecificCause.message)
        return respond(HttpStatus.CONFLICT, "conflict.integrity", "The change conflicts with existing data", request)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleDenied(ex: AccessDeniedException, request: WebRequest): ResponseEntity<Any> =
        respond(HttpStatus.FORBIDDEN, "auth.forbidden", "You are not allowed to do this", request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        log.error("Unhandled error on {}", path(request), ex)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "server.error", "An unexpected error occurred", request)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val fields = linkedMapOf<String, FieldProblem>()
        ex.bindingResult.fieldErrors.forEach { error ->
            fields.putIfAbsent(error.field, FieldProblem(error.code ?: "Invalid", error.defaultMessage ?: "is invalid"))
        }
        ex.bindingResult.globalErrors.forEach { error ->
            fields.putIfAbsent(error.objectName, FieldProblem(error.code ?: "Invalid", error.defaultMessage ?: "is invalid"))
        }
        return validationProblem(fields, request)
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val fields = linkedMapOf<String, FieldProblem>()
        ex.parameterValidationResults.forEach { result ->
            val name = result.methodParameter.parameterName ?: "parameter"
            result.resolvableErrors.firstOrNull()?.let { error ->
                fields.putIfAbsent(name, FieldProblem(error.codes?.lastOrNull() ?: "Invalid", error.defaultMessage ?: "is invalid"))
            }
        }
        return validationProblem(fields, request)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> = respond(HttpStatus.BAD_REQUEST, "request.malformed", "The request body could not be read", request)

    /** Adds `code` and `timestamp` to the ProblemDetails Spring builds for its own exceptions. */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val response = super.handleExceptionInternal(ex, body, headers, statusCode, request)
        (response?.body as? ProblemDetail)?.let { problem ->
            if (problem.properties?.containsKey("code") != true) problem.setProperty("code", defaultCode(statusCode))
            if (problem.properties?.containsKey("timestamp") != true) problem.setProperty("timestamp", Instant.now())
            if (problem.instance == null) problem.instance = URI.create(path(request))
        }
        return response
    }

    private fun validationProblem(fields: Map<String, FieldProblem>, request: WebRequest): ResponseEntity<Any> {
        val body = problem(HttpStatus.UNPROCESSABLE_CONTENT, "One or more fields are invalid", "request.validation", request)
        body.setProperty("fields", fields)
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(body)
    }

    private fun respond(status: HttpStatus, code: String, detail: String, request: WebRequest): ResponseEntity<Any> =
        ResponseEntity.status(status).body(problem(status, detail, code, request))

    private fun problem(status: HttpStatus, detail: String, code: String, request: WebRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            instance = URI.create(path(request))
            setProperty("code", code)
            setProperty("timestamp", Instant.now())
        }

    private fun path(request: WebRequest): String =
        (request as? ServletWebRequest)?.request?.requestURI ?: request.getDescription(false).removePrefix("uri=")

    private fun defaultCode(status: HttpStatusCode): String = when (status.value()) {
        400 -> "request.bad"
        401 -> "auth.unauthorized"
        403 -> "auth.forbidden"
        404 -> "request.not_found"
        405 -> "request.method_not_allowed"
        406 -> "request.not_acceptable"
        409 -> "conflict"
        413 -> "request.too_large"
        415 -> "request.unsupported_media_type"
        422 -> "request.validation"
        429 -> "request.rate_limited"
        else -> if (status.is5xxServerError) "server.error" else "request.error"
    }
}
