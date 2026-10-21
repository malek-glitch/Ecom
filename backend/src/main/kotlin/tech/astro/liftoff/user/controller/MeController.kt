package tech.astro.liftoff.user.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import tech.astro.liftoff.auth.service.SessionService
import tech.astro.liftoff.shared.security.CurrentUser
import tech.astro.liftoff.shared.security.RateLimited
import tech.astro.liftoff.shared.web.ClientContext
import tech.astro.liftoff.storage.AvatarService
import tech.astro.liftoff.user.dto.ChangePasswordRequest
import tech.astro.liftoff.user.dto.DeleteAccountRequest
import tech.astro.liftoff.user.dto.MeResponse
import tech.astro.liftoff.user.dto.SessionResponse
import tech.astro.liftoff.user.dto.UpdateProfileRequest
import tech.astro.liftoff.user.service.UserService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Me")
class MeController(
    private val users: UserService,
    private val sessions: SessionService,
    private val avatars: AvatarService,
) {
    @GetMapping
    fun me(user: CurrentUser): MeResponse = users.me(user)

    @PatchMapping
    fun update(user: CurrentUser, @Valid @RequestBody request: UpdateProfileRequest, ctx: ClientContext): MeResponse =
        users.updateProfile(user, request, ctx)

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RateLimited("login")
    fun changePassword(user: CurrentUser, @Valid @RequestBody request: ChangePasswordRequest, ctx: ClientContext) =
        users.changePassword(user, request, ctx)

    @GetMapping("/sessions")
    fun sessions(user: CurrentUser): List<SessionResponse> = sessions.listActive(user.id, user.sessionId)

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeSession(user: CurrentUser, @PathVariable id: UUID) = sessions.revoke(user.id, id, "signed_out")

    /** Signs out every other device. */
    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeOtherSessions(user: CurrentUser, ctx: ClientContext) {
        users.revokeOtherSessions(user, ctx)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccount(user: CurrentUser, @RequestBody request: DeleteAccountRequest, ctx: ClientContext) =
        users.deleteAccount(user, request, ctx)

    /** 404 `storage.disabled` unless STORAGE_ENABLED=true. */
    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RateLimited("upload")
    fun uploadAvatar(user: CurrentUser, @RequestPart("file") file: MultipartFile): MeResponse = avatars.upload(user, file)

    @DeleteMapping("/avatar")
    fun deleteAvatar(user: CurrentUser): MeResponse = avatars.remove(user)
}
