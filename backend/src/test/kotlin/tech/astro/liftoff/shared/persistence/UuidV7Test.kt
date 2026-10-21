package tech.astro.liftoff.shared.persistence

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UuidV7Test {
    @Test
    fun `ids are version 7 and sort by creation time`() {
        val first = UuidV7.generate()
        Thread.sleep(2)
        val second = UuidV7.generate()
        assertEquals(7, first.version())
        assertEquals(2, first.variant())
        assertTrue(first.toString() < second.toString())
    }
}
