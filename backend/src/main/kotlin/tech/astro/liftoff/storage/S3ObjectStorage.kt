package tech.astro.liftoff.storage

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception
import tech.astro.liftoff.shared.config.AppProperties

class S3ObjectStorage(private val s3: S3Client, private val config: AppProperties.Storage) : ObjectStorage {

    override fun put(key: String, bytes: ByteArray, contentType: String, cacheControl: String) {
        s3.putObject(
            { it.bucket(config.bucket).key(key).contentType(contentType).cacheControl(cacheControl) },
            RequestBody.fromBytes(bytes),
        )
    }

    override fun get(key: String): StoredObject? = try {
        val stream = s3.getObject { it.bucket(config.bucket).key(key) }
        val response = stream.response()
        StoredObject(stream, response.contentType() ?: "application/octet-stream", response.contentLength() ?: -1, response.eTag(), response.cacheControl())
    } catch (e: NoSuchKeyException) {
        null
    } catch (e: S3Exception) {
        if (e.statusCode() == 404) null else throw e
    }

    override fun delete(key: String) {
        s3.deleteObject { it.bucket(config.bucket).key(key) }
    }

    override fun publicUrl(key: String): String {
        val base = config.publicBaseUrl.trimEnd('/')
        return if (base.isNotEmpty()) "$base/$key" else "/api/v1/files/$key"
    }
}
