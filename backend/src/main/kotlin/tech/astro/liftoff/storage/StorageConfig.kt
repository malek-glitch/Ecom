package tech.astro.liftoff.storage

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.S3Exception
import tech.astro.liftoff.shared.config.AppProperties
import java.net.URI
import java.time.Duration
import javax.imageio.ImageIO

/** Everything here exists only with STORAGE_ENABLED=true. Without it, uploads answer 404 `storage.disabled`. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.storage", name = ["enabled"], havingValue = "true")
class StorageConfig {

    @Bean(destroyMethod = "close")
    fun s3Client(props: AppProperties): S3Client {
        val storage = props.storage
        return S3Client.builder()
            .apply { if (storage.endpoint.isNotBlank()) endpointOverride(URI.create(storage.endpoint)) }
            .region(Region.of(storage.region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(storage.accessKey, storage.secretKey)))
            // Path-style URLs and checksums only when required: what most S3-compatible servers expect.
            .forcePathStyle(true)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .httpClient(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(5)).socketTimeout(Duration.ofSeconds(30)).build())
            .build()
    }

    @Bean
    fun objectStorage(s3: S3Client, props: AppProperties): ObjectStorage = S3ObjectStorage(s3, props.storage)

    /** Creates the bucket on startup (with retries: the storage container may still be starting). */
    @Bean
    fun bucketBootstrap(s3: S3Client, props: AppProperties) = ApplicationRunner { _: ApplicationArguments ->
        ImageIO.scanForPlugins() // registers the WebP/JPEG readers from the fat jar
        if (!props.storage.createBucket) return@ApplicationRunner
        val log = LoggerFactory.getLogger(StorageConfig::class.java)
        val bucket = props.storage.bucket
        repeat(10) { attempt ->
            try {
                try {
                    s3.headBucket { it.bucket(bucket) }
                } catch (e: NoSuchBucketException) {
                    s3.createBucket { it.bucket(bucket) }
                    log.info("Created storage bucket '{}'", bucket)
                } catch (e: S3Exception) {
                    if (e.statusCode() != 404) throw e
                    s3.createBucket { it.bucket(bucket) }
                    log.info("Created storage bucket '{}'", bucket)
                }
                return@ApplicationRunner
            } catch (e: Exception) {
                log.warn("Storage not ready (attempt {}): {}", attempt + 1, e.message)
                Thread.sleep(2_000)
            }
        }
        error("Could not reach object storage at ${props.storage.endpoint}; check STORAGE_* settings")
    }
}
