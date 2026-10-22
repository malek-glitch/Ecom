package tech.astro.liftoff.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import tech.astro.liftoff.user.model.Role
import tech.astro.liftoff.user.model.User
import tech.astro.liftoff.user.model.UserSecurityState
import java.time.Instant
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    fun findByGoogleSub(googleSub: String): User?

    fun existsByRole(role: Role): Boolean

    fun countByRole(role: Role): Long

    fun countByRoleAndEnabledTrue(role: Role): Long

    fun findAllByRoleAndEnabledTrue(role: Role): List<User>

    fun countByEnabledFalse(): Long

    fun countByEmailVerifiedAtIsNull(): Long

    fun countByCreatedAtAfter(after: Instant): Long

    @Query(
        "select new tech.astro.liftoff.user.model.UserSecurityState(u.id, u.enabled, u.securityVersion) " +
            "from User u where u.id = :id",
    )
    fun findSecurityState(id: UUID): UserSecurityState?

    @Query(
        value = "select cast(date_trunc('day', created_at at time zone 'UTC') as date) as day, count(*) as total " +
            "from users where created_at >= :since group by 1 order by 1",
        nativeQuery = true,
    )
    fun countSignupsPerDay(since: Instant): List<Array<Any>>
}
