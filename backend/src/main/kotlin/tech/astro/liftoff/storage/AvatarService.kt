package tech.astro.liftoff.storage

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.error.AppException
import tech.astro.liftoff.shared.error.BadRequestException
import tech.astro.liftoff.shared.error.NotFoundException
import tech.astro.liftoff.shared.persistence.UuidV7
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.user.dto.MeResponse
import tech.astro.liftoff.user.repository.UserRepository
import tech.astro.liftoff.user.service.UserMapper

@Service
class AvatarService(
    private val storage: ObjectProvider<ObjectStorage>,
    private val users: UserRepository,
    private val mapper: UserMapper,
    private val images: ImageProcessor,
    private val events: ApplicationEventPublisher,
    private val props: AppProperties,
) {
    @Transactional
    fun upload(current: CurrentUser, file: MultipartFile): MeResponse {
        val store = storage.ifAvailable ?: throw disabled()
        if (file.isEmpty) throw BadRequestException("storage.empty_file", "The file is empty")
        if (file.size > props.storage.maxAvatarSize.toBytes()) {
            throw AppException(HttpStatus.CONTENT_TOO_LARGE, "storage.too_large", "The file is larger than ${props.storage.maxAvatarSize.toMegabytes()} MB")
        }
        val image = images.toAvatar(file.bytes)
        val user = users.findById(current.id).orElseThrow { NotFoundException("user.not_found", "User not found") }
        val key = "${ObjectStorage.PUBLIC_PREFIX}avatars/${user.id}/${UuidV7.generate()}.${image.extension}"
        store.put(key, image.bytes, image.contentType)
        user.avatarKey?.let { events.publishEvent(StorageObjectsOrphaned(listOf(it))) }
        user.avatarKey = key
        return mapper.toMe(user)
    }

    @Transactional
    fun remove(current: CurrentUser): MeResponse {
        storage.ifAvailable ?: throw disabled()
        val user = users.findById(current.id).orElseThrow { NotFoundException("user.not_found", "User not found") }
        user.avatarKey?.let { events.publishEvent(StorageObjectsOrphaned(listOf(it))) }
        user.avatarKey = null
        return mapper.toMe(user)
    }

    private fun disabled() = NotFoundException("storage.disabled", "File storage is not enabled")
}
