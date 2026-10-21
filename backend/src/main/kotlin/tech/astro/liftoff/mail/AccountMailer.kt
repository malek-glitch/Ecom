package tech.astro.liftoff.mail

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.user.model.User

/** The account emails. Links point at frontend pages, which call the API. */
@Component
class AccountMailer(
    private val events: ApplicationEventPublisher,
    private val props: AppProperties,
) {
    fun verifyEmail(user: User, token: String) =
        send(user, MailKind.VERIFY_EMAIL, link("/verify-email", token))

    fun passwordReset(user: User, token: String) =
        send(user, MailKind.RESET_PASSWORD, link("/reset-password", token))

    fun invitation(user: User, token: String, invitedBy: String) =
        send(user, MailKind.INVITATION, link("/reset-password", token, invite = true), listOf(invitedBy))

    fun passwordChanged(user: User) =
        send(user, MailKind.PASSWORD_CHANGED, UriComponentsBuilder.fromUriString(base()).path("/login").build().toUriString())

    private fun send(user: User, kind: MailKind, actionUrl: String?, args: List<String> = emptyList()) =
        events.publishEvent(OutgoingMail(user.email, user.fullName, user.locale, kind, actionUrl, args))

    private fun link(path: String, token: String, invite: Boolean = false): String =
        UriComponentsBuilder.fromUriString(base()).path(path).queryParam("token", token)
            .apply { if (invite) queryParam("invite", "1") }
            .build().toUriString()

    private fun base() = props.frontendUrl.trimEnd('/')
}
