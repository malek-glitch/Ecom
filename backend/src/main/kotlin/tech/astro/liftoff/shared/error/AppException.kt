package tech.astro.liftoff.shared.error

import org.springframework.http.HttpStatus

/**
 * Domain errors carry an HTTP status and a stable machine-readable `code`
 * (e.g. `auth.invalid_credentials`). The frontend translates the code; `message`
 * is an English fallback for logs and API clients.
 */
open class AppException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    val fields: Map<String, FieldProblem>? = null,
) : RuntimeException(message)

data class FieldProblem(val code: String, val message: String)

class BadRequestException(code: String, message: String) : AppException(HttpStatus.BAD_REQUEST, code, message)

class UnauthorizedException(code: String = "auth.unauthorized", message: String = "Authentication required") :
    AppException(HttpStatus.UNAUTHORIZED, code, message)

class ForbiddenException(code: String = "auth.forbidden", message: String = "You are not allowed to do this") :
    AppException(HttpStatus.FORBIDDEN, code, message)

class NotFoundException(code: String = "request.not_found", message: String = "Resource not found") :
    AppException(HttpStatus.NOT_FOUND, code, message)

class ConflictException(code: String, message: String) : AppException(HttpStatus.CONFLICT, code, message)

/** 422 for a business rule tied to one form field, shaped like a Bean Validation error. */
class FieldValidationException(field: String, code: String, message: String) :
    AppException(HttpStatus.UNPROCESSABLE_CONTENT, "request.validation", "One or more fields are invalid", mapOf(field to FieldProblem(code, message)))

class TooManyRequestsException(val retryAfterSeconds: Long) :
    AppException(HttpStatus.TOO_MANY_REQUESTS, "request.rate_limited", "Too many requests, try again in $retryAfterSeconds s")
