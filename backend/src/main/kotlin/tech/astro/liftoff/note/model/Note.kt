package tech.astro.liftoff.note.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant
import java.util.UUID

/**
 * Example feature: a private note owned by one user. Copy this module's shape
 * (model/repository/dto/service/controller + migration + frontend route) for real features,
 * then remove it with `node scripts/init.mjs --remove-example`.
 */
@Entity
@Table(name = "notes")
class Note(
    @Column(name = "owner_id", nullable = false, updatable = false)
    val ownerId: UUID,

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "content", nullable = false, columnDefinition = "text")
    var content: String = "",

    @Column(name = "pinned", nullable = false)
    var pinned: Boolean = false,
) : BaseEntity() {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}
