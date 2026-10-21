package tech.astro.liftoff.note.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.astro.liftoff.note.model.Note
import java.util.UUID

interface NoteRepository : JpaRepository<Note, UUID>, JpaSpecificationExecutor<Note> {
    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Note?

    fun countByOwnerId(ownerId: UUID): Long
}
