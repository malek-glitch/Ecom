package tech.astro.liftoff.user.model

/**
 * Ordered from least to most privileged. ADMIN inherits USER through the role hierarchy
 * in SecurityConfig. Adding a role means: an entry here, the CHECK constraint in a new
 * migration, and a line in the hierarchy (see docs/ARCHITECTURE.md).
 */
enum class Role { USER, ADMIN }
