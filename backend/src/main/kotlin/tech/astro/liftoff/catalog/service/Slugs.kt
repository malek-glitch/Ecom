package tech.astro.liftoff.catalog.service

import tech.astro.liftoff.shared.persistence.UuidV7
import java.text.Normalizer

/** URL-safe identifiers for store pages: "Café Crème 250g" → "cafe-creme-250g". */
object Slugs {
    private val DIACRITICS = Regex("\\p{M}+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")

    /** A slug from free text. Names without Latin letters (Arabic...) get a short random slug instead. */
    fun from(text: String, maxLength: Int = 60): String {
        val ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()
        val slug = ascii.replace(NON_ALPHANUMERIC, "-").trim('-').take(maxLength).trimEnd('-')
        return slug.ifEmpty { "item-" + UuidV7.generate().toString().takeLast(8) }
    }

    /** [base], or [base]-2, [base]-3... whichever is free. */
    fun unique(base: String, taken: (String) -> Boolean): String {
        if (!taken(base)) return base
        var n = 2
        while (taken("$base-$n")) n++
        return "$base-$n"
    }
}
