package tech.astro.liftoff.user.model

import java.util.UUID

/** The few columns the access-token validator needs on every request. */
data class UserSecurityState(val id: UUID, val enabled: Boolean, val securityVersion: Int)
