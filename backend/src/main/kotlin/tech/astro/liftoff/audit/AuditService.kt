package tech.astro.liftoff.audit

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.shared.persistence.LIKE_ESCAPE
import tech.astro.liftoff.shared.persistence.containsPattern
import tech.astro.liftoff.shared.persistence.startsWithPattern
import tech.astro.liftoff.shared.web.ClientContext
import tech.astro.liftoff.user.model.User
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Writes in the caller's transaction: if the business change rolls back, so does its audit row.
 */
@Service
class AuditService(
    private val events: AuditEventRepository,
    private val jsonMapper: JsonMapper,
    private val clock: Clock,
) {
    @Transactional
    fun record(
        action: String,
        actor: User? = null,
        target: User? = null,
        ctx: ClientContext? = null,
        details: Map<String, Any?> = emptyMap(),
        actorId: UUID? = actor?.id,
    ) {
        events.save(
            AuditEvent(
                occurredAt = clock.instant(),
                actorId = actorId,
                actorEmail = actor?.email,
                action = action,
                targetType = target?.let { "user" },
                targetId = target?.id?.toString(),
                targetLabel = target?.email,
                ipAddress = ctx?.ipAddress,
                details = details.takeIf { it.isNotEmpty() }?.let(jsonMapper::writeValueAsString),
            ),
        )
    }

    /** [action] matches as a prefix (`admin.` → every admin action); [query] searches actor and target emails. */
    @Transactional(readOnly = true)
    fun search(action: String?, query: String?, pageable: Pageable): Page<AuditEvent> {
        var spec = Specification.unrestricted<AuditEvent>()
        if (!action.isNullOrBlank()) {
            spec = spec.and { root, _, cb -> cb.like(root.get("action"), startsWithPattern(action), LIKE_ESCAPE) }
        }
        if (!query.isNullOrBlank()) {
            val pattern = containsPattern(query)
            spec = spec.and { root, _, cb ->
                cb.or(
                    cb.like(cb.lower(root.get("actorEmail")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("targetLabel")), pattern, LIKE_ESCAPE),
                )
            }
        }
        return events.findAll(spec, pageable)
    }

    @Transactional
    fun purgeOlderThan(cutoff: Instant): Int = events.deleteOlderThan(cutoff)
}
