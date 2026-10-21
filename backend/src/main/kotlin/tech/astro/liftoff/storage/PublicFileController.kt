package tech.astro.liftoff.storage

import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import tech.astro.liftoff.shared.error.NotFoundException

/**
 * Streams objects under `public/` from the bucket, so the bucket itself can stay private and
 * files are same-origin (no CORS or CSP exceptions). Nothing is buffered or cached on disk.
 * For heavy traffic, set STORAGE_PUBLIC_BASE_URL and serve from a CDN instead.
 */
@Hidden
@RestController
class PublicFileController(private val storage: ObjectProvider<ObjectStorage>) {

    @GetMapping("/api/v1/files/public/**")
    fun get(request: HttpServletRequest): ResponseEntity<StreamingResponseBody> {
        val store = storage.ifAvailable ?: throw NotFoundException()
        val key = request.requestURI.removePrefix("/api/v1/files/")
        if (!key.startsWith(ObjectStorage.PUBLIC_PREFIX) || key.contains("..") || key.contains("//")) throw NotFoundException()
        val stored = store.get(key) ?: throw NotFoundException("file.not_found", "File not found")

        val etag = stored.eTag
        if (etag != null && request.getHeader(HttpHeaders.IF_NONE_MATCH) == etag) {
            stored.content.close()
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build()
        }
        val body = StreamingResponseBody { out -> stored.content.use { it.transferTo(out) } }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(stored.contentType))
            .apply { if (stored.contentLength >= 0) contentLength(stored.contentLength) }
            .apply { if (etag != null) eTag(etag) }
            .header(HttpHeaders.CACHE_CONTROL, stored.cacheControl ?: ObjectStorage.IMMUTABLE)
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", "default-src 'none'; sandbox")
            .body(body)
    }
}
