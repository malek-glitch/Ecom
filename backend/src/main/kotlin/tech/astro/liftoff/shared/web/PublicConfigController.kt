package tech.astro.liftoff.shared.web

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.ObjectProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.settings.Setting
import tech.astro.liftoff.settings.SettingsService
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.i18n.SupportedLocales
import tech.astro.liftoff.storage.ObjectStorage

data class PublicConfig(
    val appName: String,
    val registrationEnabled: Boolean,
    val requireVerifiedEmail: Boolean,
    /** Null when Google sign-in is off: the frontend hides the button. */
    val googleClientId: String?,
    val storageEnabled: Boolean,
    val locales: List<String>,
    val defaultLocale: String,
    /** Client-visible runtime settings (see Setting), typed. */
    val settings: Map<String, Any>,
)

/** What the frontend needs before anyone signs in. Runtime values, so one build serves every environment. */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public")
@SecurityRequirements
class PublicConfigController(
    private val props: AppProperties,
    private val settings: SettingsService,
    private val storage: ObjectProvider<ObjectStorage>,
) {
    @GetMapping("/config")
    fun config() = PublicConfig(
        appName = props.name,
        registrationEnabled = settings.bool(Setting.REGISTRATION_ENABLED),
        requireVerifiedEmail = props.auth.requireVerifiedEmail,
        googleClientId = props.google.clientId.takeIf { props.google.enabled },
        storageEnabled = storage.ifAvailable != null,
        locales = SupportedLocales.ALL,
        defaultLocale = SupportedLocales.DEFAULT,
        settings = settings.clientVisible(),
    )
}
