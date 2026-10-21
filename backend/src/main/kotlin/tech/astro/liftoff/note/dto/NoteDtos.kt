package tech.astro.liftoff.note.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class NoteResponse(
    val id: UUID,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Send it back on update: a stale version gets 409 `conflict.stale` instead of overwriting someone's edit. */
    val version: Long,
)

data class CreateNoteRequest(
    @field:NotBlank @field:Size(max = 200)
    val title: String = "",
    @field:Size(max = 20_000)
    val content: String = "",
    val pinned: Boolean = false,
)

data class UpdateNoteRequest(
    @field:NotBlank @field:Size(max = 200)
    val title: String = "",
    @field:Size(max = 20_000)
    val content: String = "",
    val pinned: Boolean = false,
    @field:NotNull
    val version: Long? = null,
)
