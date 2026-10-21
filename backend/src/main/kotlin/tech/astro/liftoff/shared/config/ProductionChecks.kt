package tech.astro.liftoff.shared.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/** Refuses to run in production with example secrets, and warns about settings that break email links. */
@Component
@Profile("prod")
class ProductionChecks(
    private val props: AppProperties,
    @param:Value("\${spring.mail.host:}") private val mailHost: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        check(PLACEHOLDERS.none { props.auth.jwtSecret.contains(it, ignoreCase = true) }) {
            "JWT_SECRET still holds an example value. Generate one with: openssl rand -base64 48"
        }
        if (mailHost.isBlank()) {
            log.warn("MAIL_HOST is not set: emails are only logged, so nobody can verify an address or reset a password.")
        }
        if (props.frontendUrl.contains("localhost")) {
            log.warn("APP_URL is {}: links in emails will point to localhost.", props.frontendUrl)
        }
    }

    private companion object {
        val PLACEHOLDERS = listOf("change-me", "changeme", "dev-only", "example")
    }
}
