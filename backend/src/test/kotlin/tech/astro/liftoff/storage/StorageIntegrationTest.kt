package tech.astro.liftoff.storage

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import tech.astro.liftoff.support.IntegrationTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Avatars against a real S3 server (RustFS, as in compose.yaml). */
class StorageIntegrationTest : IntegrationTest() {

    companion object {
        private val rustfs: GenericContainer<*> = GenericContainer("rustfs/rustfs:1.0.0")
            .withEnv("RUSTFS_ACCESS_KEY", "test-access-key")
            .withEnv("RUSTFS_SECRET_KEY", "test-secret-key")
            .withExposedPorts(9000)
            .waitingFor(Wait.forListeningPort())

        @JvmStatic
        @DynamicPropertySource
        fun storageProperties(registry: DynamicPropertyRegistry) {
            rustfs.start()
            registry.add("app.storage.enabled") { "true" }
            registry.add("app.storage.endpoint") { "http://${rustfs.host}:${rustfs.getMappedPort(9000)}" }
            registry.add("app.storage.access-key") { "test-access-key" }
            registry.add("app.storage.secret-key") { "test-secret-key" }
            registry.add("app.storage.bucket") { "liftoff-test" }
        }
    }

    @Test
    fun `an uploaded avatar is cropped, stored and served`() {
        val session = signUp()
        mvc.get("/api/v1/public/config").andExpect { jsonPath("$.storageEnabled") { value(true) } }

        val upload = mvc.multipart("/api/v1/me/avatar") {
            file(MockMultipartFile("file", "photo.png", "image/png", png(1200, 800)))
            header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
        }.andExpect { status { isOk() } }.bodyJson()
        val url = upload["avatarUrl"].asString()
        assertTrue(url.startsWith("/api/v1/files/public/avatars/"), url)

        val started = mvc.get(url).andExpect { request { asyncStarted() } }.andReturn()
        val served = mvc.perform(asyncDispatch(started)).andExpect(status().isOk).andReturn().response
        assertTrue(served.getHeader(HttpHeaders.CACHE_CONTROL)!!.contains("immutable"))
        val image = assertNotNull(ImageIO.read(ByteArrayInputStream(served.contentAsByteArray)))
        assertEquals(512, image.width)
        assertEquals(512, image.height)

        mvc.multipart("/api/v1/me/avatar") {
            file(MockMultipartFile("file", "evil.png", "image/png", "<html>not an image</html>".toByteArray()))
            header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("storage.invalid_image") }
        }

        deleteAuthed("/api/v1/me/avatar", session.accessToken).andExpect {
            status { isOk() }
            jsonPath("$.avatarUrl") { doesNotExist() }
        }
    }

    private fun png(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            color = Color(40, 90, 200)
            fillRect(0, 0, width, height)
            dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }
}
