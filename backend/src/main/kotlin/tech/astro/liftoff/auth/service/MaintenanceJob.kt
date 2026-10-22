package tech.astro.liftoff.auth.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.auth.repository.UserSessionRepository
import tech.astro.liftoff.auth.repository.UserTokenRepository
import tech.astro.liftoff.catalog.service.SearchInsightsService
import tech.astro.liftoff.shared.config.AppProperties
import java.time.Clock
import java.time.Duration

/** Nightly cleanup of dead sessions, used/expired email tokens, old audit events and stale search queries. */
@Component
class MaintenanceJob(
    private val sessions: UserSessionRepository,
    private val tokens: UserTokenRepository,
    private val audit: AuditService,
    private val searches: SearchInsightsService,
    private val props: AppProperties,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.maintenance.cron:0 17 3 * * *}")
    @Transactional
    fun run() {
        val now = clock.instant()
        val keepFor = Duration.ofDays(7)
        val removedSessions = sessions.deleteStale(now.minus(keepFor))
        val removedTokens = tokens.deleteStale(now.minus(keepFor))
        val removedEvents = audit.purgeOlderThan(now.minus(props.audit.retention))
        val removedSearches = searches.purgeOlderThan(now.minus(Duration.ofDays(180)))
        log.info(
            "Maintenance: removed {} sessions, {} email tokens, {} audit events, {} search queries",
            removedSessions, removedTokens, removedEvents, removedSearches,
        )
    }
}
