package tech.astro.liftoff.settings

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.shared.error.FieldValidationException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.user.model.User
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

data class SettingView(
    val key: String,
    val type: SettingType,
    val value: String,
    val default: String,
    val clientVisible: Boolean,
    val description: String,
)

/**
 * Admin-tunable settings, changeable without a redeploy (ported from Wingeo).
 * Reads are cached in memory and refreshed on write: right for one instance; with several,
 * add a TTL or a broadcast.
 */
@Service
class SettingsService(
    private val repository: AppSettingRepository,
    private val audit: AuditService,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = ConcurrentHashMap<String, String>()

    fun raw(setting: Setting): String =
        cache.computeIfAbsent(setting.key) { key -> repository.findById(key).map { it.value }.orElse(setting.default) }

    fun string(setting: Setting): String = raw(setting)

    fun bool(setting: Setting): Boolean = raw(setting).toBooleanStrictOrNull() ?: run {
        log.warn("Setting {} holds a non-boolean value; using default {}", setting.key, setting.default)
        setting.default.toBoolean()
    }

    fun int(setting: Setting): Int = raw(setting).toIntOrNull() ?: run {
        log.warn("Setting {} holds a non-integer value; using default {}", setting.key, setting.default)
        setting.default.toInt()
    }

    fun all(): List<SettingView> = Setting.entries.map(::view)

    /** Typed values for the frontend, keyed by setting key. */
    fun clientVisible(): Map<String, Any> = Setting.entries.filter { it.clientVisible }.associate { it.key to typed(it) }

    @Transactional
    fun set(key: String, value: String, changedBy: User): SettingView {
        val setting = Setting.byKey(key) ?: throw NotFoundException("settings.unknown_key", "Unknown setting: $key")
        val normalized = value.trim()
        when (setting.type) {
            SettingType.BOOLEAN -> normalized.toBooleanStrictOrNull()
                ?: throw FieldValidationException("value", "Boolean", "must be true or false")
            SettingType.INTEGER -> {
                val number = normalized.toIntOrNull() ?: throw FieldValidationException("value", "Integer", "must be a whole number")
                if (setting.min != null && number < setting.min) {
                    throw FieldValidationException("value", "Min", "must be at least ${setting.min}")
                }
            }
            SettingType.STRING -> {
                if (normalized.length > 2000) throw FieldValidationException("value", "Size", "must be at most 2000 characters")
                if (setting.pattern != null && !setting.pattern.matches(normalized)) {
                    throw FieldValidationException("value", "Pattern", "has an invalid format")
                }
            }
        }
        val previous = raw(setting)
        val row = repository.findById(key).orElse(null)
        if (row == null) {
            repository.save(AppSetting(key, normalized, clock.instant(), changedBy.id))
        } else {
            row.value = normalized
            row.updatedAt = clock.instant()
            row.updatedBy = changedBy.id
        }
        cache[key] = normalized
        audit.record(AuditActions.ADMIN_SETTING_CHANGED, actor = changedBy, details = mapOf("key" to key, "from" to previous, "to" to normalized))
        log.info("Setting {} changed by {}: '{}' -> '{}'", key, changedBy.email, previous, normalized)
        return view(setting)
    }


    private fun view(setting: Setting) = SettingView(
        key = setting.key,
        type = setting.type,
        value = raw(setting),
        default = setting.default,
        clientVisible = setting.clientVisible,
        description = setting.description,
    )

    private fun typed(setting: Setting): Any = when (setting.type) {
        SettingType.BOOLEAN -> bool(setting)
        SettingType.INTEGER -> int(setting)
        SettingType.STRING -> string(setting)
    }
}
