package tech.astro.liftoff.shared.persistence

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.UUID

/**
 * Time-ordered UUIDv7 (RFC 9562). IDs sort by creation time, which keeps primary-key
 * indexes compact, and they are assigned in Kotlin so entities never carry a null id.
 */
object UuidV7 {
    private val random = SecureRandom()

    fun generate(): UUID {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        val millis = System.currentTimeMillis()
        for (i in 0 until 6) {
            bytes[i] = (millis ushr (40 - 8 * i)).toByte()
        }
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long)
    }
}
