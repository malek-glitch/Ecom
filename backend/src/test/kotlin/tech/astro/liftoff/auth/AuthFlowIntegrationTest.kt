package tech.astro.liftoff.auth

import org.junit.jupiter.api.Test
import tech.astro.liftoff.auth.dto.TokenResponse
import tech.astro.liftoff.auth.service.GoogleIdentity
import tech.astro.liftoff.support.IntegrationTest
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthFlowIntegrationTest : IntegrationTest() {

    private fun refresh(refreshToken: String) = postJson("/api/v1/auth/refresh", mapOf("refreshToken" to refreshToken))

    @Test
    fun `a new account must confirm its email before signing in`() {
        val email = uniqueEmail()
        postJson("/api/v1/auth/register", mapOf("email" to email, "password" to PASSWORD, "fullName" to "Ada", "locale" to "fr"))
            .andExpect {
                status { isCreated() }
                jsonPath("$.verificationRequired") { value(true) }
                jsonPath("$.session") { doesNotExist() }
            }
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to PASSWORD))
            .andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("auth.email_not_verified") }
            }

        assertTrue(mail.latestTo(email).subject.startsWith("Confirmez"), "verification email follows the account locale")
        val token = mail.tokenFor(email)
        postJson("/api/v1/auth/verify-email", mapOf("token" to token)).andExpect { status { isNoContent() } }
        postJson("/api/v1/auth/verify-email", mapOf("token" to token))
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("auth.token_invalid") }
            }

        val session = login(email)
        getAuthed("/api/v1/me", session.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.email") { value(email) }
            jsonPath("$.emailVerified") { value(true) }
            jsonPath("$.locale") { value("fr") }
        }
    }

    @Test
    fun `registering twice with the same email is a conflict`() {
        val email = uniqueEmail()
        signUp(email)
        postJson("/api/v1/auth/register", mapOf("email" to email.uppercase(), "password" to PASSWORD, "fullName" to "Again"))
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("auth.email_taken") }
            }
    }

    @Test
    fun `refresh rotates the token and a replayed token revokes the session`() {
        val first = signUp()
        val second = refresh(first.refreshToken).andExpect { status { isOk() } }.bodyAs(TokenResponse::class.java)
        assertNotEquals(first.refreshToken, second.refreshToken)

        refresh(first.refreshToken).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("auth.refresh_reused") }
        }
        refresh(second.refreshToken).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("auth.refresh_invalid") }
        }
    }

    @Test
    fun `logout revokes the session`() {
        val session = signUp()
        postJson("/api/v1/auth/logout", mapOf("refreshToken" to session.refreshToken)).andExpect { status { isNoContent() } }
        refresh(session.refreshToken).andExpect { status { isUnauthorized() } }
        // idempotent
        postJson("/api/v1/auth/logout", mapOf("refreshToken" to session.refreshToken)).andExpect { status { isNoContent() } }
    }

    @Test
    fun `a wrong password and an unknown email get the same answer`() {
        val email = uniqueEmail()
        signUp(email)
        for (credentials in listOf(email to "wrong-password", uniqueEmail("nobody") to PASSWORD)) {
            postJson("/api/v1/auth/login", mapOf("email" to credentials.first, "password" to credentials.second))
                .andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.code") { value("auth.invalid_credentials") }
                }
        }
    }

    @Test
    fun `password reset signs every device out and notifies the owner`() {
        val email = uniqueEmail()
        val old = signUp(email)
        postJson("/api/v1/auth/password/forgot", mapOf("email" to email)).andExpect { status { isAccepted() } }
        val reset = postJson("/api/v1/auth/password/reset", mapOf("token" to mail.tokenFor(email), "password" to "a-brand-new-password"))
            .andExpect { status { isOk() } }
            .bodyAs(TokenResponse::class.java)

        getAuthed("/api/v1/me", old.accessToken).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("auth.invalid_token") }
        }
        refresh(old.refreshToken).andExpect { status { isUnauthorized() } }
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to PASSWORD)).andExpect { status { isUnauthorized() } }
        login(email, "a-brand-new-password")
        getAuthed("/api/v1/me", reset.accessToken).andExpect { status { isOk() } }
        assertTrue(mail.latestTo(email).subject.contains("password was changed", ignoreCase = true))
    }

    @Test
    fun `forgot password never reveals whether an account exists`() {
        val nobody = uniqueEmail("nobody")
        postJson("/api/v1/auth/password/forgot", mapOf("email" to nobody)).andExpect { status { isAccepted() } }
        postJson("/api/v1/auth/verify-email/resend", mapOf("email" to nobody)).andExpect { status { isAccepted() } }
        assertEquals(0, mail.countTo(nobody))
    }

    @Test
    fun `changing the password keeps this device signed in and signs out the others`() {
        val email = uniqueEmail()
        val deviceA = signUp(email)
        val deviceB = login(email)
        putJson("/api/v1/me/password", mapOf("currentPassword" to PASSWORD, "newPassword" to "another-strong-pass"), deviceA.accessToken)
            .andExpect { status { isNoContent() } }

        getAuthed("/api/v1/me", deviceA.accessToken).andExpect { status { isUnauthorized() } }
        val refreshedA = refresh(deviceA.refreshToken).andExpect { status { isOk() } }.bodyAs(TokenResponse::class.java)
        getAuthed("/api/v1/me", refreshedA.accessToken).andExpect { status { isOk() } }
        refresh(deviceB.refreshToken).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `a wrong current password is reported on its field`() {
        val session = signUp()
        putJson("/api/v1/me/password", mapOf("currentPassword" to "nope", "newPassword" to "another-strong-pass"), session.accessToken)
            .andExpect {
                status { isEqualTo(422) }
                jsonPath("$.fields.currentPassword.code") { value("Incorrect") }
            }
    }

    @Test
    fun `sessions can be listed and revoked`() {
        val email = uniqueEmail()
        val deviceA = signUp(email)
        val deviceB = login(email)
        getAuthed("/api/v1/me/sessions", deviceA.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.current == true)]") { exists() }
        }
        deleteAuthed("/api/v1/me/sessions", deviceA.accessToken).andExpect { status { isNoContent() } }
        refresh(deviceB.refreshToken).andExpect { status { isUnauthorized() } }
        refresh(deviceA.refreshToken).andExpect { status { isOk() } }
    }

    @Test
    fun `google sign-in takes over an unverified account and wipes its password`() {
        val email = uniqueEmail()
        postJson("/api/v1/auth/register", mapOf("email" to email, "password" to PASSWORD, "fullName" to "Squatter"))
            .andExpect { status { isCreated() } }
        val subject = "sub-${UUID.randomUUID()}"
        google.identities["takeover"] = GoogleIdentity(subject, email, true, "Real Owner")
        google.identities["takeover-again"] = GoogleIdentity(subject, email, true, "Real Owner")

        val session = postJson("/api/v1/auth/google", mapOf("idToken" to "takeover")).andExpect { status { isOk() } }.bodyAs(TokenResponse::class.java)
        assertTrue(session.user.googleLinked)
        assertTrue(session.user.emailVerified)
        assertFalse(session.user.hasPassword)
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to PASSWORD)).andExpect { status { isUnauthorized() } }
        // the link was persisted: the next Google sign-in finds the same account by its subject
        val again = postJson("/api/v1/auth/google", mapOf("idToken" to "takeover-again")).andExpect { status { isOk() } }.bodyAs(TokenResponse::class.java)
        assertEquals(session.user.id, again.user.id)
        getAuthed("/api/v1/me", again.accessToken).andExpect { jsonPath("$.googleLinked") { value(true) } }
    }

    @Test
    fun `google sign-in creates the account once and finds it again`() {
        val email = uniqueEmail("google")
        val subject = "sub-${UUID.randomUUID()}"
        google.identities["first"] = GoogleIdentity(subject, email, true, "Grace Hopper")
        google.identities["second"] = GoogleIdentity(subject, email, true, "Grace Hopper")
        val first = postJson("/api/v1/auth/google", mapOf("idToken" to "first", "locale" to "ar")).andExpect { status { isOk() } }.bodyAs(TokenResponse::class.java)
        val second = postJson("/api/v1/auth/google", mapOf("idToken" to "second")).andExpect { status { isOk() } }.bodyAs(TokenResponse::class.java)
        assertEquals(first.user.id, second.user.id)
        assertEquals("Grace Hopper", first.user.fullName)
        assertEquals("ar", first.user.locale)
    }

    @Test
    fun `google accounts with an unverified email are refused`() {
        google.identities["unverified"] = GoogleIdentity("sub-${UUID.randomUUID()}", uniqueEmail(), false, null)
        postJson("/api/v1/auth/google", mapOf("idToken" to "unverified")).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("auth.google_email_unverified") }
        }
    }

    @Test
    fun `users can delete their own account`() {
        val email = uniqueEmail()
        val session = signUp(email)
        deleteAuthed("/api/v1/me", session.accessToken, mapOf("password" to "wrong")).andExpect { status { isEqualTo(422) } }
        deleteAuthed("/api/v1/me", session.accessToken, mapOf("password" to PASSWORD)).andExpect { status { isNoContent() } }
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to PASSWORD)).andExpect { status { isUnauthorized() } }
        refresh(session.refreshToken).andExpect { status { isUnauthorized() } }
    }
}
