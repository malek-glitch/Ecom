package tech.astro.liftoff.note.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import tech.astro.liftoff.note.dto.CreateNoteRequest
import tech.astro.liftoff.note.dto.NoteResponse
import tech.astro.liftoff.note.dto.UpdateNoteRequest
import tech.astro.liftoff.note.service.NoteService
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.web.PageResponse
import tech.astro.liftoff.shared.web.Paging
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notes")
@Tag(name = "Notes (example)")
class NoteController(private val notes: NoteService) {

    /** Pinned notes first, then most recently edited. */
    @GetMapping
    fun list(
        user: CurrentUser,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<NoteResponse> {
        val sort = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("updatedAt"))
        return PageResponse.of(notes.list(user.id, q, Paging.of(page, size, sort))) { it }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(user: CurrentUser, @Valid @RequestBody request: CreateNoteRequest): NoteResponse = notes.create(user.id, request)

    @GetMapping("/{id}")
    fun get(user: CurrentUser, @PathVariable id: UUID): NoteResponse = notes.get(user.id, id)

    @PutMapping("/{id}")
    fun update(user: CurrentUser, @PathVariable id: UUID, @Valid @RequestBody request: UpdateNoteRequest): NoteResponse =
        notes.update(user.id, id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: CurrentUser, @PathVariable id: UUID) = notes.delete(user.id, id)
}
