package tech.astro.liftoff.mail

/** Each kind maps to `mail.<key>.*` messages in `i18n/messages*.properties`. */
enum class MailKind(val key: String) {
    VERIFY_EMAIL("verify"),
    RESET_PASSWORD("reset"),
    INVITATION("invite"),
    PASSWORD_CHANGED("password_changed"),
    ORDER_PLACED("order_placed"),
    ORDER_SHIPPED("order_shipped"),
    ORDER_CANCELLED("order_cancelled"),
    LOW_STOCK("low_stock"),
}

/** Published inside a transaction; delivered only after it commits. */
data class OutgoingMail(
    val to: String,
    val recipientName: String,
    val locale: String,
    val kind: MailKind,
    val actionUrl: String?,
    /** Extra message arguments after the app name ({0}): {1}, {2}... */
    val args: List<String> = emptyList(),
)

data class RenderedMail(val to: String, val subject: String, val html: String, val text: String)

/** Transport. Swap the bean to send through an HTTP API (Resend, Postmark...) instead of SMTP. */
fun interface MailGateway {
    fun send(mail: RenderedMail)
}
