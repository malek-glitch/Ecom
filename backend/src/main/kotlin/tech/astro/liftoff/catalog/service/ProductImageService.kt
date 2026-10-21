package tech.astro.liftoff.catalog.service

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tech.astro.liftoff.catalog.dto.AdminProductResponse
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.AppException
import tech.astro.liftoff.shared.error.BadRequestException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.persistence.UuidV7
import tech.astro.liftoff.storage.ImageProcessor
import tech.astro.liftoff.storage.ObjectStorage
import tech.astro.liftoff.storage.StorageObjectsOrphaned
import java.util.UUID

/** The product photo. Needs STORAGE_ENABLED=true; without it the store shows a placeholder. */
@Service
class ProductImageService(
    private val storage: ObjectProvider<ObjectStorage>,
    private val products: ProductService,
    private val repository: ProductRepository,
    private val mapper: CatalogMapper,
    private val images: ImageProcessor,
    private val events: ApplicationEventPublisher,
    private val props: AppProperties,
) {
    @Transactional
    fun upload(id: UUID, file: MultipartFile): AdminProductResponse {
        val store = storage.ifAvailable ?: throw disabled()
        if (file.isEmpty) throw BadRequestException("storage.empty_file", "The file is empty")
        val max = props.storage.maxProductImageSize
        if (file.size > max.toBytes()) {
            throw AppException(HttpStatus.CONTENT_TOO_LARGE, "storage.too_large", "The file is larger than ${max.toMegabytes()} MB")
        }
        val product = products.load(id)
        val image = images.toProductImage(file.bytes)
        val key = "${ObjectStorage.PUBLIC_PREFIX}products/${product.id}/${UuidV7.generate()}.${image.extension}"
        store.put(key, image.bytes, image.contentType)
        product.imageKey?.let { events.publishEvent(StorageObjectsOrphaned(listOf(it))) }
        product.imageKey = key
        // Flushed so the response carries the new version, which the edit form sends back.
        return mapper.admin(repository.saveAndFlush(product))
    }

    @Transactional
    fun remove(id: UUID): AdminProductResponse {
        storage.ifAvailable ?: throw disabled()
        val product = products.load(id)
        product.imageKey?.let { events.publishEvent(StorageObjectsOrphaned(listOf(it))) }
        product.imageKey = null
        return mapper.admin(repository.saveAndFlush(product))
    }

    private fun disabled() = NotFoundException("storage.disabled", "File storage is not enabled")
}
