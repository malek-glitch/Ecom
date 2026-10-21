package tech.astro.liftoff.admin

import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import tech.astro.liftoff.auth.dto.TokenResponse
import tech.astro.liftoff.support.IntegrationTest
import tech.astro.liftoff.user.model.Role
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminIntegrationTest : IntegrationTest() {

    @Test
    fun `an admin invites, promotes, disables and deletes a user`() {
        val admin = adminLogin()
        val email = uniqueEmail("invitee")
        val invited = postJson(
            "/api/v1/admin/users/invite",
            mapOf("email" to email, "fullName" to "New Colleague", "role" to "USER", "locale" to "ar"),
            admin.accessToken,
        ).andExpect { status { isCreated() } }.bodyJson()
        val id = invited["id"].asString()
        assertTrue(mail.latestTo(email).html.contains("dir=\"rtl\""), "Arabic invitation is right-to-left")

        // Accepting the invitation means choosing a password; it also proves the email address.
        val session = postJson("/api/v1/auth/password/reset", mapOf("token" to mail.tokenFor(email), "password" to "invited-user-pass"))
            .andExpect { status { isOk() } }
            .bodyAs(TokenResponse::class.java)
        assertTrue(session.user.emailVerified)

        getAuthed("/api/v1/admin/users?q=${email.substringBefore('@')}", admin.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.totalItems") { value(1) }
            jsonPath("$.items[0].email") { value(email) }
        }

        // A role change invalidates the user's access token; the next refresh carries the new role.
        patchJson("/api/v1/admin/users/$id", mapOf("role" to "ADMIN"), admin.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.role") { value("ADMIN") }
        }
        getAuthed("/api/v1/me", session.accessToken).andExpect { status { isUnauthorized() } }
        val promoted = postJson("/api/v1/auth/refresh", mapOf("refreshToken" to session.refreshToken))
            .andExpect { status { isOk() } }
            .bodyAs(TokenResponse::class.java)
        assertEquals(Role.ADMIN, promoted.user.role)
        getAuthed("/api/v1/admin/stats", promoted.accessToken).andExpect { status { isOk() } }

        patchJson("/api/v1/admin/users/$id", mapOf("role" to "USER", "enabled" to false), admin.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
            jsonPath("$.activeSessions") { value(0) }
        }
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to "invited-user-pass")).andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("auth.account_disabled") }
        }
        postJson("/api/v1/auth/refresh", mapOf("refreshToken" to promoted.refreshToken)).andExpect { status { isUnauthorized() } }

        getAuthed("/api/v1/admin/audit?action=admin.&q=$email", admin.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(greaterThanOrEqualTo(3)) }
        }

        deleteAuthed("/api/v1/admin/users/$id", admin.accessToken).andExpect { status { isNoContent() } }
        getAuthed("/api/v1/admin/users/$id", admin.accessToken).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("user.not_found") }
        }
    }

    @Test
    fun `admins cannot lock themselves out`() {
        val admin = adminLogin()
        val me = admin.user.id
        patchJson("/api/v1/admin/users/$me", mapOf("role" to "USER"), admin.accessToken)
            .andExpect { status { isConflict() }; jsonPath("$.code") { value("admin.cannot_change_own_role") } }
        patchJson("/api/v1/admin/users/$me", mapOf("enabled" to false), admin.accessToken)
            .andExpect { status { isConflict() }; jsonPath("$.code") { value("admin.cannot_disable_self") } }
        deleteAuthed("/api/v1/admin/users/$me", admin.accessToken)
            .andExpect { status { isConflict() }; jsonPath("$.code") { value("admin.cannot_delete_self") } }
        deleteAuthed("/api/v1/me", admin.accessToken, mapOf("password" to ADMIN_PASSWORD))
            .andExpect { status { isConflict() }; jsonPath("$.code") { value("admin.last_admin") } }
    }

    @Test
    fun `closing registration is a runtime setting`() {
        val admin = adminLogin()
        try {
            putJson("/api/v1/admin/settings/auth.registrationEnabled", mapOf("value" to "false"), admin.accessToken).andExpect {
                status { isOk() }
                jsonPath("$.value") { value("false") }
            }
            mvc.get("/api/v1/public/config").andExpect { jsonPath("$.registrationEnabled") { value(false) } }
            postJson("/api/v1/auth/register", mapOf("email" to uniqueEmail(), "password" to PASSWORD, "fullName" to "Late"))
                .andExpect { status { isForbidden() }; jsonPath("$.code") { value("auth.registration_disabled") } }
        } finally {
            putJson("/api/v1/admin/settings/auth.registrationEnabled", mapOf("value" to "true"), admin.accessToken)
        }
        putJson("/api/v1/admin/settings/auth.registrationEnabled", mapOf("value" to "maybe"), admin.accessToken)
            .andExpect { status { isEqualTo(422) }; jsonPath("$.fields.value.code") { value("Boolean") } }
        putJson("/api/v1/admin/settings/no.such.key", mapOf("value" to "x"), admin.accessToken)
            .andExpect { status { isNotFound() }; jsonPath("$.code") { value("settings.unknown_key") } }
    }

    @Test
    fun `stats count users and signups`() {
        signUp()
        val admin = adminLogin()
        getAuthed("/api/v1/admin/stats", admin.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.totalUsers") { value(greaterThanOrEqualTo(2)) }
            jsonPath("$.admins") { value(greaterThanOrEqualTo(1)) }
            jsonPath("$.signupsPerDay.length()") { value(30) }
        }
    }
}
