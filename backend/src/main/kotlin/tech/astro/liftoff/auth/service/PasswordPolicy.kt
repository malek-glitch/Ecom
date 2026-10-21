package tech.astro.liftoff.auth.service

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * 8–64 characters and at most 72 UTF-8 bytes. BCrypt only looks at the first 72 bytes and
 * Spring Security rejects longer input, which matters for Arabic (2 bytes per letter).
 */
object PasswordPolicy {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 64
    const val MAX_BYTES = 72

    fun isAcceptable(password: String): Boolean =
        password.isNotBlank() &&
            password.length in MIN_LENGTH..MAX_LENGTH &&
            password.toByteArray(Charsets.UTF_8).size <= MAX_BYTES
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidPasswordValidator::class])
@MustBeDocumented
annotation class ValidPassword(
    val message: String = "must be 8 to 64 characters",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class ValidPasswordValidator : ConstraintValidator<ValidPassword, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value != null && PasswordPolicy.isAcceptable(value)
}
