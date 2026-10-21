package tech.astro.liftoff.settings

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.astro.liftoff.audit.AuditActions
import tech.astro.liftoff.audit.AuditService
import tech.astro.liftoff.shared.error.FieldValidationException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.user.model.User
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsServiceTest {
    private val repository = mockk<AppSettingRepository>()
    private val audit = mockk<AuditService>(relaxed = true)
    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val service = SettingsService(repository, audit, clock)
    private val admin = User(email = "admin@liftoff.test", fullName = "Admin")
    private val registration = Setting.REGISTRATION_ENABLED

    @Test
    fun `a missing row means the default`() {
        every { repository.findById(any()) } returns Optional.empty()
        assertTrue(service.bool(registration))
    }

    @Test
    fun `a stored value overrides the default`() {
        every { repository.findById(registration.key) } returns Optional.of(AppSetting(registration.key, "false", clock.instant()))
        assertFalse(service.bool(registration))
    }

    @Test
    fun `a malformed stored boolean falls back to the default`() {
        every { repository.findById(registration.key) } returns Optional.of(AppSetting(registration.key, "yes", clock.instant()))
        assertTrue(service.bool(registration))
    }

    @Test
    fun `unknown keys are rejected rather than created`() {
        assertThrows<NotFoundException> { service.set("no.such.key", "x", admin) }
    }

    @Test
    fun `a boolean setting refuses other values`() {
        every { repository.findById(any()) } returns Optional.empty()
        assertThrows<FieldValidationException> { service.set(registration.key, "maybe", admin) }
    }

    @Test
    fun `writing refreshes the cache and is audited`() {
        every { repository.findById(any()) } returns Optional.empty()
        every { repository.save(any()) } answers { firstArg() }
        assertTrue(service.bool(registration))
        service.set(registration.key, "false", admin)
        assertFalse(service.bool(registration))
        verify { audit.record(AuditActions.ADMIN_SETTING_CHANGED, actor = admin, details = any()) }
    }

    @Test
    fun `client-visible settings are typed`() {
        every { repository.findById(any()) } returns Optional.empty()
        val visible = service.clientVisible()
        assertEquals(true, visible["auth.registrationEnabled"])
        assertEquals("", visible["app.announcement"])
    }
}
