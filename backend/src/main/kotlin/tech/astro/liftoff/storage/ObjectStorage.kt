package tech.astro.liftoff.storage

import java.io.InputStream

/**
 * Minimal S3-style storage. The only implementation talks plain S3 (RustFS, MinIO, Garage,
 * AWS, R2...), so switching providers is configuration. The bean exists only when
 * STORAGE_ENABLED=true; callers take an ObjectProvider and degrade when it's absent.
 */
interface ObjectStorage {
    fun put(key: String, bytes: ByteArray, contentType: String, cacheControl: String = IMMUTABLE)

    fun get(key: String): StoredObject?

    fun delete(key: String)

    /** Browser-usable URL for a key under `public/`: the CDN base when configured, else the API's streaming route. */
    fun publicUrl(key: String): String

    companion object {
        const val PUBLIC_PREFIX = "public/"
        const val IMMUTABLE = "public, max-age=31536000, immutable"
    }
}

class StoredObject(
    val content: InputStream,
    val contentType: String,
    val contentLength: Long,
    val eTag: String?,
    val cacheControl: String?,
)
