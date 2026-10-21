package tech.astro.liftoff.shared.persistence

/**
 * Builds SQL LIKE patterns from user input. `%` and `_` are escaped, so searching "50%" finds
 * "50%" instead of everything. Pass [LIKE_ESCAPE] as the escape character:
 *
 *     cb.like(cb.lower(root.get("title")), containsPattern(query), LIKE_ESCAPE)
 */
const val LIKE_ESCAPE = '\\'

/** Case-insensitive "contains" (compare against `cb.lower(...)`). */
fun containsPattern(query: String): String = "%${escapeLike(query.trim().lowercase())}%"

fun startsWithPattern(prefix: String): String = "${escapeLike(prefix.trim())}%"

private fun escapeLike(value: String): String =
    value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
