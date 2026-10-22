package tech.astro.liftoff.storage

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.springframework.stereotype.Component
import tech.astro.liftoff.shared.error.BadRequestException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ProcessedImage(val bytes: ByteArray, val contentType: String, val extension: String)

/**
 * Turns an uploaded picture into a square avatar or a product photo. The format is checked from the file's
 * magic bytes (never the client's Content-Type), dimensions are checked before decoding
 * (decompression bombs), and re-encoding drops EXIF data such as GPS position.
 */
@Component
class ImageProcessor {

    fun toAvatar(bytes: ByteArray, size: Int = AVATAR_SIZE): ProcessedImage {
        if (sniff(bytes) == null) throw invalid()
        val image = decode(bytes)
        val side = minOf(size, image.width, image.height)
        val hasAlpha = image.colorModel.hasAlpha()
        val output = ByteArrayOutputStream()
        Thumbnails.of(image)
            .crop(Positions.CENTER)
            .size(side, side)
            .apply { if (!hasAlpha) imageType(BufferedImage.TYPE_INT_RGB) }
            .outputFormat(if (hasAlpha) "png" else "jpg")
            .apply { if (!hasAlpha) outputQuality(0.85) }
            .toOutputStream(output)
        return if (hasAlpha) {
            ProcessedImage(output.toByteArray(), "image/png", "png")
        } else {
            ProcessedImage(output.toByteArray(), "image/jpeg", "jpg")
        }
    }

    /** Shrinks a product photo to fit in [maxSide]×[maxSide], keeping its proportions (never enlarges). */
    fun toProductImage(bytes: ByteArray, maxSide: Int = PRODUCT_IMAGE_SIZE): ProcessedImage {
        if (sniff(bytes) == null) throw invalid()
        val image = decode(bytes)
        val hasAlpha = image.colorModel.hasAlpha()
        val output = ByteArrayOutputStream()
        Thumbnails.of(image)
            .size(minOf(maxSide, image.width), minOf(maxSide, image.height))
            .keepAspectRatio(true)
            .apply { if (!hasAlpha) imageType(BufferedImage.TYPE_INT_RGB) }
            .outputFormat(if (hasAlpha) "png" else "jpg")
            .apply { if (!hasAlpha) outputQuality(0.85) }
            .toOutputStream(output)
        return if (hasAlpha) {
            ProcessedImage(output.toByteArray(), "image/png", "png")
        } else {
            ProcessedImage(output.toByteArray(), "image/jpeg", "jpg")
        }
    }

    private fun decode(bytes: ByteArray): BufferedImage {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) throw invalid()
            val reader = readers.next()
            try {
                reader.input = input
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                if (width <= 0 || height <= 0 || width > MAX_SIDE || height > MAX_SIDE || width.toLong() * height > MAX_PIXELS) {
                    throw BadRequestException("storage.image_too_large", "The image dimensions are too large")
                }
                return reader.read(0) ?: throw invalid()
            } catch (e: BadRequestException) {
                throw e
            } catch (e: Exception) {
                throw invalid()
            } finally {
                reader.dispose()
            }
        }
    }

    /** Returns the detected format, or null when the bytes are not JPEG/PNG/WebP/GIF. */
    fun sniff(bytes: ByteArray): String? {
        fun startsWith(vararg signature: Int) = bytes.size >= signature.size && signature.indices.all { (bytes[it].toInt() and 0xFF) == signature[it] }
        return when {
            startsWith(0xFF, 0xD8, 0xFF) -> "jpeg"
            startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "png"
            startsWith(0x47, 0x49, 0x46, 0x38) -> "gif"
            bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
            else -> null
        }
    }

    private fun invalid() = BadRequestException("storage.invalid_image", "Upload a JPEG, PNG, WebP or GIF image")

    companion object {
        const val AVATAR_SIZE = 512
        const val PRODUCT_IMAGE_SIZE = 1200
        const val MAX_SIDE = 10_000
        const val MAX_PIXELS = 40_000_000L
    }
}
