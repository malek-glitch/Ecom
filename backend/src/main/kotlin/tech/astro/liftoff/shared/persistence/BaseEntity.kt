package tech.astro.liftoff.shared.persistence

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.Hibernate
import java.util.UUID

/**
 * The id is generated up front (UUIDv7), and the nullable `version` doubles as Spring Data's
 * "is new" marker: null means not yet persisted, so `save()` inserts without a SELECT first.
 * It also gives every entity optimistic locking for free.
 */
@MappedSuperclass
abstract class BaseEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UuidV7.generate(),
) {
    @Version
    @Column(name = "version", nullable = false)
    var version: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        return id == (other as BaseEntity).id
    }

    override fun hashCode(): Int = id.hashCode()
}
