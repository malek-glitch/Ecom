package tech.astro.liftoff.user.service

import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import tech.astro.liftoff.storage.ObjectStorage
import tech.astro.liftoff.user.dto.MeResponse
import tech.astro.liftoff.user.model.User

@Component
class UserMapper(private val storage: ObjectProvider<ObjectStorage>) {

    fun avatarUrl(user: User): String? = user.avatarKey?.let { key -> storage.ifAvailable?.publicUrl(key) }

    fun toMe(user: User) = MeResponse(
        id = user.id,
        email = user.email,
        fullName = user.fullName,
        role = user.role,
        locale = user.locale,
        avatarUrl = avatarUrl(user),
        emailVerified = user.emailVerified,
        hasPassword = user.hasPassword,
        googleLinked = user.googleSub != null,
        createdAt = user.createdAt,
    )
}
