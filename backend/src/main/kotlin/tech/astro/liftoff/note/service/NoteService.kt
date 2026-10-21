package tech.astro.liftoff.note.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.note.dto.CreateNoteRequest
import tech.astro.liftoff.note.dto.NoteResponse
import tech.astro.liftoff.note.dto.UpdateNoteRequest
import tech.astro.liftoff.note.model.Note
import tech.astro.liftoff.note.repository.NoteRepository
import tech.astro.liftoff.shared.error.ConflictException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.persistence.LIKE_ESCAPE
import tech.astro.liftoff.shared.persistence.containsPattern
import java.util.UUID

/**
 * Every query is scoped to the owner, so another user's note is indistinguishable from a
 * missing one (404, never 403): ids can't be probed.
 */
@Service
class NoteService(private val notes: NoteRepository) {

    @Transactional(readOnly = true)
    fun list(ownerId: UUID, query: String?, pageable: Pageable): Page<NoteResponse> {
        var spec = Specification.where<Note> { root, _, cb -> cb.equal(root.get<UUID>("ownerId"), ownerId) }
        if (!query.isNullOrBlank()) {
            val pattern = containsPattern(query)
            spec = spec.and { root, _, cb ->
                cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("content")), pattern, LIKE_ESCAPE),
                )
            }
        }
        return notes.findAll(spec, pageable).map(::toResponse)
    }

    @Transactional(readOnly = true)
    fun get(ownerId: UUID, id: UUID): NoteResponse = toResponse(load(ownerId, id))

    @Transactional
    fun create(ownerId: UUID, request: CreateNoteRequest): NoteResponse =
        toResponse(notes.saveAndFlush(Note(ownerId, request.title.trim(), request.content, request.pinned)))

    @Transactional
    fun update(ownerId: UUID, id: UUID, request: UpdateNoteRequest): NoteResponse {
        val note = load(ownerId, id)
        if (request.version != note.version) {
            throw ConflictException("conflict.stale", "This note was changed elsewhere. Reload and try again.")
        }
        note.title = request.title.trim()
        note.content = request.content
        note.pinned = request.pinned
        return toResponse(notes.saveAndFlush(note))
    }

    @Transactional
    fun delete(ownerId: UUID, id: UUID) = notes.delete(load(ownerId, id))

    private fun load(ownerId: UUID, id: UUID): Note =
        notes.findByIdAndOwnerId(id, ownerId) ?: throw NotFoundException("note.not_found", "Note not found")

    private fun toResponse(note: Note) =
        NoteResponse(note.id, note.title, note.content, note.pinned, note.createdAt, note.updatedAt, note.version ?: 0)
}
