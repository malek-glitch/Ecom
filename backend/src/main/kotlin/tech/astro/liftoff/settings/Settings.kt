package tech.astro.liftoff.settings

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

enum class SettingType { BOOLEAN, STRING, INTEGER }

/**
 * Every runtime setting, declared once with its type and default. A missing database row
 * means "use the default", so the table only holds deliberate changes and a fresh database
 * behaves exactly like a live one. `clientVisible` settings are served to the frontend
 * through `GET /api/v1/public/config`.
 */
enum class Setting(
    val key: String,
    val type: SettingType,
    val default: String,
    val clientVisible: Boolean,
    val description: String,
) {
    REGISTRATION_ENABLED(
        key = "auth.registrationEnabled",
        type = SettingType.BOOLEAN,
        default = "true",
        clientVisible = true,
        description = "Allow new people to create an account. Turn off for invite-only apps.",
    ),
    ANNOUNCEMENT(
        key = "app.announcement",
        type = SettingType.STRING,
        default = "",
        clientVisible = true,
        description = "Banner shown to every signed-in user. Leave empty to hide it.",
    ),
    SUPPORT_EMAIL(
        key = "app.supportEmail",
        type = SettingType.STRING,
        default = "",
        clientVisible = true,
        description = "Contact address shown in the app footer.",
    ),
    ;

    companion object {
        fun byKey(key: String): Setting? = entries.firstOrNull { it.key == key }
    }
}

@Entity
@Table(name = "app_settings")
class AppSetting(
    @Id
    @Column(name = "key", length = 100)
    val key: String,

    @Column(name = "value", nullable = false, columnDefinition = "text")
    var value: String,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column(name = "updated_by")
    var updatedBy: UUID? = null,
)

interface AppSettingRepository : JpaRepository<AppSetting, String>
