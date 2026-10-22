package tech.astro.liftoff.admin.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import tech.astro.liftoff.settings.SettingType
import tech.astro.liftoff.shared.i18n.SupportedLocale
import tech.astro.liftoff.user.model.Role
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class UserStatusFilter { ACTIVE, DISABLED, UNVERIFIED }

data class AdminUserSummary(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: Role,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    val googleLinked: Boolean,
    val avatarUrl: String?,
    val lastLoginAt: Instant?,
    val createdAt: Instant,
)

data class AdminUserDetail(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: Role,
    val locale: String,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    val googleLinked: Boolean,
    val avatarUrl: String?,
    val activeSessions: Long,
    val lastLoginAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class InviteUserRequest(
    @field:NotBlank @field:Email @field:Size(max = 254)
    val email: String = "",
    @field:NotBlank @field:Size(max = 120)
    val fullName: String = "",
    val role: Role = Role.USER,
    @field:SupportedLocale
    val locale: String? = null,
)

data class UpdateUserRequest(
    @field:Size(max = 120)
    @field:Pattern(regexp = ".*\\S.*", message = "must not be blank")
    val fullName: String? = null,
    val role: Role? = null,
    val enabled: Boolean? = null,
)

data class AdminStats(
    val totalUsers: Long,
    val admins: Long,
    val disabledUsers: Long,
    val unverifiedUsers: Long,
    val newUsersLast7Days: Long,
    val newUsersLast30Days: Long,
    val activeSessions: Long,
    val signupsPerDay: List<DailyCount>,
)

data class DailyCount(val date: LocalDate, val count: Long)

data class AdminSettingResponse(
    val key: String,
    val type: SettingType,
    val value: String,
    val default: String,
    val clientVisible: Boolean,
    val description: String,
    /** The allowed values of a CHOICE setting; empty otherwise. */
    val options: List<String>,
)

data class UpdateSettingRequest(
    @field:Size(max = 2000)
    val value: String = "",
)

data class AuditEventResponse(
    val id: UUID,
    val occurredAt: Instant,
    val action: String,
    val actorId: UUID?,
    val actorEmail: String?,
    val targetType: String?,
    val targetId: String?,
    val targetLabel: String?,
    val ipAddress: String?,
    val details: String?,
)
