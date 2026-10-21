package tech.astro.liftoff.auth.service

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.user.model.Role
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.repository.UserRepository
import java.time.Clock

/**
 * Creates (or promotes) the first administrator from ADMIN_EMAIL / ADMIN_PASSWORD, but only
 * while the database has no admin at all. Once one exists these variables are ignored, so
 * leaving them set can't silently re-grant admin later.
 */
@Component
class AdminBootstrap(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val audit: AuditService,
    private val props: AppProperties,
    private val clock: Clock,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (users.existsByRole(Role.ADMIN)) return
        val email = props.admin.email.trim().lowercase()
        if (email.isEmpty()) {
            log.warn("No administrator exists. Set ADMIN_EMAIL and ADMIN_PASSWORD to create one at startup.")
            return
        }
        val existing = users.findByEmail(email)
        val admin = if (existing != null) {
            existing.role = Role.ADMIN
            existing.bumpSecurityVersion()
            if (!existing.emailVerified) existing.emailVerifiedAt = clock.instant()
            log.info("Promoted existing user {} to ADMIN", email)
            existing
        } else {
            if (!PasswordPolicy.isAcceptable(props.admin.password)) {
                log.error("ADMIN_PASSWORD must be 8 to 64 characters: administrator {} was not created", email)
                return
            }
            log.info("Created administrator {}", email)
            users.save(
                User(
                    email = email,
                    fullName = "Administrator",
                    passwordHash = passwordEncoder.encode(props.admin.password),
                    role = Role.ADMIN,
                    emailVerifiedAt = clock.instant(),
                ),
            )
        }
        audit.record(AuditActions.ADMIN_BOOTSTRAPPED, target = admin)
    }
}
