package tech.astro.liftoff.catalog.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import tech.astro.liftoff.shared.persistence.BaseEntity
import java.time.Instant

/** A shelf of the store. Deleting one leaves its products uncategorised (ON DELETE SET NULL). */
@Entity
@Table(name = "categories")
class Category(
    @Column(name = "slug", nullable = false, unique = true, length = 80)
    var slug: String,

    @Column(name = "name", nullable = false, length = 120)
    var name: String,

    @Column(name = "description", nullable = false, columnDefinition = "text")
    var description: String = "",

    /** Sort order in menus, lowest first. */
    @Column(name = "position", nullable = false)
    var position: Int = 0,
) : BaseEntity() {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}
