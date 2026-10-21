package tech.astro.liftoff.shared.security

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import tech.astro.liftoff.user.model.UserSecurityState
import tech.astro.liftoff.user.repository.UserRepository
import java.time.Duration
import java.util.Optional
import java.util.UUID

/**
 * Short-lived cache of (enabled, securityVersion) per user, so validating an access token
 * costs at most one indexed lookup per user every 30s. Services evict on every change, which
 * makes revocation immediate on this instance and ≤30s on others.
 */
@Component
class UserSecurityStateCache(private val users: UserRepository) {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .maximumSize(50_000)
        .build<UUID, Optional<UserSecurityState>> { id -> Optional.ofNullable(users.findSecurityState(id)) }

    fun get(userId: UUID): UserSecurityState? = cache.get(userId).orElse(null)

    fun evict(userId: UUID) = cache.invalidate(userId)
}
