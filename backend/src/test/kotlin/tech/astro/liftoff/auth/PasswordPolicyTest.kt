package tech.astro.liftoff.auth

import org.junit.jupiter.api.Test
import tech.astro.liftoff.auth.service.PasswordPolicy.isAcceptable
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordPolicyTest {
    @Test
    fun `length limits`() {
        assertFalse(isAcceptable("short"))
        assertTrue(isAcceptable("eight-ch"))
        assertTrue(isAcceptable("x".repeat(64)))
        assertFalse(isAcceptable("x".repeat(65)))
        assertFalse(isAcceptable(" ".repeat(10)))
    }

    @Test
    fun `bcrypt's 72-byte limit applies to multi-byte scripts`() {
        assertTrue(isAcceptable("ب".repeat(36))) // 72 bytes
        assertFalse(isAcceptable("ب".repeat(37))) // 74 bytes, although only 37 characters
    }
}
