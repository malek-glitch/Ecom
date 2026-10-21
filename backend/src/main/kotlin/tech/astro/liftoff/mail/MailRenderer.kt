package tech.astro.liftoff.mail

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.i18n.SupportedLocales

/**
 * One responsive HTML layout (`templates/mail/action.html`) plus a plain-text alternative,
 * both filled from the recipient's locale. Arabic gets `dir="rtl"`.
 */
@Component
class MailRenderer(
    private val templateEngine: ITemplateEngine,
    private val messages: MessageSource,
    private val props: AppProperties,
) {
    fun render(mail: OutgoingMail): RenderedMail {
        val locale = SupportedLocales.toLocale(mail.locale)
        val args: Array<Any> = arrayOf(props.name, *mail.args.toTypedArray())
        val prefix = "mail.${mail.kind.key}"
        fun msg(key: String, arguments: Array<Any> = args) = messages.getMessage(key, arguments, locale)

        val rtl = SupportedLocales.isRtl(mail.locale)
        val subject = msg("$prefix.subject")
        val greeting = msg("mail.greeting", arrayOf(mail.recipientName))
        val intro = msg("$prefix.intro")
        val actionLabel = msg("$prefix.action")
        val outro = msg("$prefix.outro")
        val footer = msg("mail.footer")

        val context = Context(locale).apply {
            setVariable("lang", locale.language)
            setVariable("dir", if (rtl) "rtl" else "ltr")
            setVariable("align", if (rtl) "right" else "left")
            setVariable("appName", props.name)
            setVariable("subject", subject)
            setVariable("preheader", msg("$prefix.preheader"))
            setVariable("greeting", greeting)
            setVariable("intro", intro)
            setVariable("actionUrl", mail.actionUrl)
            setVariable("actionLabel", actionLabel)
            setVariable("outro", outro)
            setVariable("fallback", msg("mail.fallback"))
            setVariable("footer", footer)
        }
        val html = templateEngine.process("mail/action", context)
        val text = buildString {
            appendLine(greeting).appendLine()
            appendLine(intro).appendLine()
            mail.actionUrl?.let { appendLine("$actionLabel:").appendLine(it).appendLine() }
            appendLine(outro).appendLine()
            append("— ").append(footer)
        }
        return RenderedMail(mail.to, subject, html, text)
    }
}
