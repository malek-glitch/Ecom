package tech.astro.liftoff.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant
import java.util.UUID

/** Append-only record of security and admin actions. Ids are snapshots, not foreign keys, so history survives deletes. */
@Entity
@Table(name = "audit_events")
class AuditEvent(
    @Column(name = "occurred_at", nullable = false, updatable = false)
    val occurredAt: Instant,

    @Column(name = "actor_id", updatable = false)
    val actorId: UUID?,

    @Column(name = "actor_email", length = 254, updatable = false)
    val actorEmail: String?,

    @Column(name = "action", nullable = false, length = 64, updatable = false)
    val action: String,

    @Column(name = "target_type", length = 32, updatable = false)
    val targetType: String?,

    @Column(name = "target_id", length = 64, updatable = false)
    val targetId: String?,

    @Column(name = "target_label", length = 254, updatable = false)
    val targetLabel: String?,

    @Column(name = "ip_address", length = 64, updatable = false)
    val ipAddress: String?,

    /** JSON object as text (no JSONB mapping needed: it is only displayed). */
    @Column(name = "details", columnDefinition = "text", updatable = false)
    val details: String?,
) : BaseEntity()

interface AuditEventRepository : JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {
    @Modifying
    @Query("delete from AuditEvent e where e.occurredAt < :before")
    fun deleteOlderThan(before: Instant): Int
}

object AuditActions {
    const val USER_REGISTERED = "user.registered"
    const val USER_LOGGED_IN = "user.logged_in"
    const val USER_EMAIL_VERIFIED = "user.email_verified"
    const val USER_PASSWORD_CHANGED = "user.password_changed"
    const val USER_PASSWORD_RESET = "user.password_reset"
    const val USER_PROFILE_UPDATED = "user.profile_updated"
    const val USER_SESSIONS_REVOKED = "user.sessions_revoked"
    const val USER_ACCOUNT_DELETED = "user.account_deleted"
    const val USER_GOOGLE_LINKED = "user.google_linked"
    const val REFRESH_REUSE_DETECTED = "security.refresh_reuse_detected"
    const val ADMIN_USER_INVITED = "admin.user_invited"
    const val ADMIN_USER_UPDATED = "admin.user_updated"
    const val ADMIN_USER_DELETED = "admin.user_deleted"
    const val ADMIN_SESSIONS_REVOKED = "admin.sessions_revoked"
    const val ADMIN_PASSWORD_RESET_SENT = "admin.password_reset_sent"
    const val ADMIN_SETTING_CHANGED = "admin.setting_changed"
    const val ADMIN_PRODUCT_CREATED = "admin.product_created"
    const val ADMIN_PRODUCT_UPDATED = "admin.product_updated"
    const val ADMIN_PRODUCT_DELETED = "admin.product_deleted"
    const val ADMIN_CATEGORY_CREATED = "admin.category_created"
    const val ADMIN_CATEGORY_UPDATED = "admin.category_updated"
    const val ADMIN_CATEGORY_DELETED = "admin.category_deleted"
    const val ADMIN_ORDER_STATUS_CHANGED = "admin.order_status_changed"
    const val ADMIN_STOCK_ADJUSTED = "admin.stock_adjusted"
    const val ADMIN_INVENTORY_SETTINGS_CHANGED = "admin.inventory_settings_changed"
    const val ADMIN_BOOTSTRAPPED = "system.admin_bootstrapped"
}
