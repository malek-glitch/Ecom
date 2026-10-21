package tech.astro.liftoff.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import tech.astro.liftoff.auth.service.GoogleIdentity
import tech.astro.liftoff.auth.service.GoogleTokenVerifier
import tech.astro.liftoff.mail.MailGateway
import tech.astro.liftoff.mail.RenderedMail
import tech.astro.liftoff.shared.error.UnauthorizedException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@TestConfiguration(proxyBeanMethods = false)
class TestDoubles {
    @Bean
    @Primary
    fun capturingMailGateway() = CapturingMailGateway()

    @Bean
    @Primary
    fun fakeGoogleTokenVerifier() = FakeGoogleTokenVerifier()
}

/** Keeps sent emails in memory (mail is synchronous in the test profile). */
class CapturingMailGateway : MailGateway {
    val sent = CopyOnWriteArrayList<RenderedMail>()

    override fun send(mail: RenderedMail) {
        sent += mail
    }

    fun latestTo(email: String): RenderedMail =
        sent.lastOrNull { it.to.equals(email, ignoreCase = true) } ?: error("No email sent to $email")

    fun countTo(email: String): Int = sent.count { it.to.equals(email, ignoreCase = true) }

    /** The `token` query parameter of the link in the latest email to [email]. */
    fun tokenFor(email: String): String =
        TOKEN.find(latestTo(email).text)?.groupValues?.get(1) ?: error("No token link in email to $email")

    private companion object {
        val TOKEN = Regex("token=([A-Za-z0-9_-]+)")
    }
}

/** Maps a fake ID token string to the identity Google would have returned. */
class FakeGoogleTokenVerifier : GoogleTokenVerifier {
    val identities = ConcurrentHashMap<String, GoogleIdentity>()

    override fun verify(idToken: String): GoogleIdentity =
        identities[idToken] ?: throw UnauthorizedException("auth.google_invalid", "Google sign-in could not be verified")
}
