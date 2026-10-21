package tech.astro.liftoff.shared.i18n

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.util.Locale
import kotlin.reflect.KClass

/** Languages the app ships with. Keep in sync with frontend/project.inlang/settings.json. */
object SupportedLocales {
    const val DEFAULT = "fr"
    val ALL = listOf("fr", "en", "ar")

    fun normalize(raw: String?): String = raw?.trim()?.lowercase()?.takeIf { it in ALL } ?: DEFAULT

    fun toLocale(tag: String): Locale = Locale.forLanguageTag(normalize(tag))

    fun isRtl(tag: String): Boolean = normalize(tag) == "ar"
}

/** Null is allowed (field omitted); otherwise the value must be one of [SupportedLocales.ALL]. */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [SupportedLocaleValidator::class])
@MustBeDocumented
annotation class SupportedLocale(
    val message: String = "must be a supported locale",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class SupportedLocaleValidator : ConstraintValidator<SupportedLocale, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value == null || value in SupportedLocales.ALL
}
