package tech.astro.liftoff.note

import org.junit.jupiter.api.Test
import tech.astro.liftoff.support.IntegrationTest

class NoteIntegrationTest : IntegrationTest() {

    @Test
    fun `notes are private, searchable and protected against lost updates`() {
        val alice = signUp()
        val bob = signUp()
        val note = postJson("/api/v1/notes", mapOf("title" to "Groceries", "content" to "Milk, bread"), alice.accessToken)
            .andExpect { status { isCreated() } }
            .bodyJson()
        val id = note["id"].asString()
        val version = note["version"].asLong()

        getAuthed("/api/v1/notes/$id", bob.accessToken).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("note.not_found") }
        }
        getAuthed("/api/v1/notes?q=GROCER", alice.accessToken).andExpect { jsonPath("$.totalItems") { value(1) } }
        getAuthed("/api/v1/notes", bob.accessToken).andExpect { jsonPath("$.totalItems") { value(0) } }

        putJson("/api/v1/notes/$id", mapOf("title" to "Groceries!", "content" to "", "pinned" to true, "version" to version), alice.accessToken)
            .andExpect {
                status { isOk() }
                jsonPath("$.pinned") { value(true) }
            }
        putJson("/api/v1/notes/$id", mapOf("title" to "Stale edit", "content" to "", "pinned" to false, "version" to version), alice.accessToken)
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("conflict.stale") }
            }

        deleteAuthed("/api/v1/notes/$id", bob.accessToken).andExpect { status { isNotFound() } }
        deleteAuthed("/api/v1/notes/$id", alice.accessToken).andExpect { status { isNoContent() } }
    }

    @Test
    fun `search treats wildcard characters literally`() {
        val session = signUp()
        for (title in listOf("50% off", "500 reasons", "snake_case", "snakeXcase")) {
            postJson("/api/v1/notes", mapOf("title" to title, "content" to ""), session.accessToken).andExpect { status { isCreated() } }
        }
        // MockMvc encodes the URL template, so the raw % arrives as "50%".
        getAuthed("/api/v1/notes?q=50%", session.accessToken).andExpect { jsonPath("$.totalItems") { value(1) } }
        getAuthed("/api/v1/notes?q=snake_", session.accessToken).andExpect { jsonPath("$.totalItems") { value(1) } }
    }

    @Test
    fun `note validation`() {
        val session = signUp()
        postJson("/api/v1/notes", mapOf("title" to " ", "content" to "x"), session.accessToken).andExpect {
            status { isEqualTo(422) }
            jsonPath("$.fields.title.code") { value("NotBlank") }
        }
    }
}
