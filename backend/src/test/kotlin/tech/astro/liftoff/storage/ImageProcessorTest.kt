package tech.astro.liftoff.storage

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.astro.liftoff.shared.error.BadRequestException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageProcessorTest {
    private val processor = ImageProcessor()

    private fun encode(width: Int, height: Int, type: Int, format: String): ByteArray {
        val image = BufferedImage(width, height, type)
        return ByteArrayOutputStream().also { ImageIO.write(image, format, it) }.toByteArray()
    }

    @Test
    fun `large photos become 512px square jpegs`() {
        val result = processor.toAvatar(encode(1600, 900, BufferedImage.TYPE_INT_RGB, "jpg"))
        assertEquals("image/jpeg", result.contentType)
        val image = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertEquals(512, image.width)
        assertEquals(512, image.height)
    }

    @Test
    fun `transparency is kept as png and small images are not upscaled`() {
        val result = processor.toAvatar(encode(120, 80, BufferedImage.TYPE_INT_ARGB, "png"))
        assertEquals("image/png", result.contentType)
        val image = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertEquals(80, image.width)
        assertEquals(80, image.height)
    }

    @Test
    fun `non-images are rejected by their bytes, whatever their name`() {
        assertNull(processor.sniff("<svg onload=alert(1)>".toByteArray()))
        assertThrows<BadRequestException> { processor.toAvatar("GIF8 but not really".toByteArray()) }
        assertThrows<BadRequestException> { processor.toAvatar(ByteArray(0)) }
    }
}
