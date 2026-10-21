package tech.astro.liftoff.mail

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import tech.astro.liftoff.shared.config.AppProperties

/**
 * SMTP when MAIL_HOST is set (Mailpit in dev, any provider in prod). Without it, emails are
 * only written to the log, links included, so local development works with no mail server.
 */
@Component
class SmtpMailGateway(
    private val mailSender: ObjectProvider<JavaMailSender>,
    @param:Value("\${spring.mail.host:}") private val host: String,
    private val props: AppProperties,
) : MailGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(mail: RenderedMail) {
        val sender = mailSender.ifAvailable
        if (host.isBlank() || sender == null) {
            log.info("MAIL_HOST is not set: email logged instead of sent.\nTo: {}\nSubject: {}\n\n{}", mail.to, mail.subject, mail.text)
            return
        }
        val message = sender.createMimeMessage()
        MimeMessageHelper(message, true, Charsets.UTF_8.name()).apply {
            setFrom(props.mail.from)
            setTo(mail.to)
            setSubject(mail.subject)
            setText(mail.text, mail.html)
        }
        sender.send(message)
    }
}
