package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UserRepository

sealed interface SyncSecurityResult<out T> {
    data class Ok<T>(val value: T) : SyncSecurityResult<T>
    data object NotFound : SyncSecurityResult<Nothing>
    data class Forbidden(val message: String) : SyncSecurityResult<Nothing>
    data class Invalid(val message: String) : SyncSecurityResult<Nothing>
}

/**
 * Phones upload what they recorded about unsynced data (warnings, the Day-7 wipe and lock).
 * Any active user uploads their own; only an admin reads them, sees who is locked, or clears a lock.
 *
 * The user and phone come from the token and the site from the database, never from the request,
 * and the caller's role and active flag are read from the database, as elsewhere.
 */
class SyncSecurityService(
    private val userRepository: UserRepository,
    private val repository: SyncSecurityRepository,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun upload(
        principal: AuthPrincipal,
        request: SyncSecurityEventsRequest
    ): SyncSecurityResult<SyncSecurityUploadResult> {
        val user = userRepository.findById(principal.userId)
        if (user == null || !user.active) return SyncSecurityResult.Forbidden("Forbidden")

        if (request.events.size > SyncSecurityLimits.MAX_EVENTS_PER_REQUEST) {
            return SyncSecurityResult.Invalid(
                "Send at most ${SyncSecurityLimits.MAX_EVENTS_PER_REQUEST} events at a time"
            )
        }

        /* Another user's events, or ones with no key or type, are turned away, not stored. */
        val (valid, invalid) = request.events.partition {
            it.eventKey.isNotBlank() && it.eventKey.length <= 255 &&
                it.eventType.isNotBlank() && it.eventType.length <= 64 &&
                (it.userId == null || it.userId == user.userId)
        }

        val accepted = if (valid.isEmpty()) 0 else repository.insertAll(
            deviceId = principal.deviceId ?: UNKNOWN_DEVICE,
            userId = user.userId,
            username = user.username,
            events = valid.map { it.copy(details = it.details?.take(MAX_DETAILS)) },
            now = now()
        )

        return SyncSecurityResult.Ok(
            SyncSecurityUploadResult(
                accepted = accepted,
                duplicates = valid.size - accepted,
                rejected = invalid.size
            )
        )
    }

    suspend fun events(
        principal: AuthPrincipal,
        siteId: String?,
        userId: String?,
        eventType: String?,
        before: Long?,
        limit: Int?
    ): SyncSecurityResult<List<SyncSecurityEventDto>> {
        requireAdmin(principal) ?: return forbidden()

        return SyncSecurityResult.Ok(
            repository.list(
                siteId, userId, eventType, before,
                (limit ?: SyncSecurityLimits.DEFAULT_PAGE).coerceIn(1, SyncSecurityLimits.MAX_PAGE)
            )
        )
    }

    suspend fun locked(principal: AuthPrincipal, siteId: String?): SyncSecurityResult<List<LockedAccountDto>> {
        requireAdmin(principal) ?: return forbidden()

        return SyncSecurityResult.Ok(repository.locked(siteId))
    }

    /*
     * Tells the user's phone, the next time it checks in, that the admin has cleared the lock.
     * It does not bring back wiped data; it only lets the person sign in and capture again.
     */
    suspend fun clearLock(principal: AuthPrincipal, targetId: String): SyncSecurityResult<LockedAccountCleared> {
        val actor = requireAdmin(principal) ?: return forbidden()
        val target = userRepository.findById(targetId) ?: return SyncSecurityResult.NotFound

        val clearedAt = now()
        userRepository.clearSyncLock(
            userId = target.userId,
            clearedAt = clearedAt,
            audit = AuditEntry(
                action = AuditActions.SYNC_LOCK_CLEAR,
                entityType = AuditActions.ENTITY_USER,
                entityId = target.userId,
                actorUserId = actor.userId,
                actorUsername = actor.username,
                actorRole = Role.ADMIN.id,
                siteId = target.siteId
            )
        )

        return SyncSecurityResult.Ok(LockedAccountCleared(target.userId, clearedAt))
    }

    private suspend fun requireAdmin(principal: AuthPrincipal) =
        userRepository.findById(principal.userId)
            ?.takeIf { it.active && Role.fromId(it.role) == Role.ADMIN }

    private fun forbidden() = SyncSecurityResult.Forbidden("Only an admin can do this")

    private companion object {
        const val UNKNOWN_DEVICE = "unknown"
        const val MAX_DETAILS = 1000
    }
}
