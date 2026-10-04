package com.beeftech.backend.api.auth

import com.beeftech.backend.api.AuditActions
import com.beeftech.backend.api.AuditEntry
import com.beeftech.backend.api.auditDetails

sealed interface DeviceAdminResult<out T> {
    data class Ok<T>(val value: T) : DeviceAdminResult<T>
    data object NotFound : DeviceAdminResult<Nothing>
    data class Forbidden(val message: String) : DeviceAdminResult<Nothing>
    data class Invalid(val message: String) : DeviceAdminResult<Nothing>
    data class Conflict(val message: String) : DeviceAdminResult<Nothing>
}

/**
 * Phones and login security. An admin sees every phone and can revoke or reinstate one, and
 * reads the login events and current lockouts; a manager sees only their own site's phones.
 *
 * As in UserAdminService, the caller's role, site and active flag come from the database.
 */
class DeviceAdminService(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val loginSecurity: LoginSecurityRepository,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun listDevices(principal: AuthPrincipal, siteId: String?, status: String?): DeviceAdminResult<List<DeviceDto>> {
        val actor = resolveActor(principal) ?: return forbidden()

        if (status != null && status != DeviceStatus.ACTIVE && status != DeviceStatus.REVOKED) {
            return DeviceAdminResult.Invalid("status must be ACTIVE or REVOKED")
        }

        val site = if (actor.role == Role.ADMIN) {
            siteId
        } else {
            /* A manager without a site sees no phones, as with scoped record reads. */
            val own = actor.siteId ?: return DeviceAdminResult.Ok(emptyList())
            if (siteId != null && siteId != own) {
                return DeviceAdminResult.Forbidden("Managers can only view their own site")
            }
            own
        }

        return DeviceAdminResult.Ok(deviceRepository.list(site, status))
    }

    suspend fun revoke(principal: AuthPrincipal, deviceId: String, reason: String): DeviceAdminResult<DeviceDto> {
        val actor = resolveActor(principal) ?: return forbidden()
        if (actor.role != Role.ADMIN) return DeviceAdminResult.Forbidden("Only an admin can revoke a device")

        val cleanReason = validReason(reason) ?: return reasonRequired()
        val device = deviceRepository.find(deviceId) ?: return DeviceAdminResult.NotFound
        if (device.status == DeviceStatus.REVOKED) return DeviceAdminResult.Conflict("That device is already revoked")

        deviceRepository.revoke(
            deviceId = deviceId,
            actorUserId = actor.userId,
            reason = cleanReason,
            now = now(),
            audit = auditEntry(actor, AuditActions.DEVICE_REVOKE, device, cleanReason)
        )

        return DeviceAdminResult.Ok(deviceRepository.find(deviceId)!!)
    }

    suspend fun reinstate(principal: AuthPrincipal, deviceId: String, reason: String): DeviceAdminResult<DeviceDto> {
        val actor = resolveActor(principal) ?: return forbidden()
        if (actor.role != Role.ADMIN) return DeviceAdminResult.Forbidden("Only an admin can reinstate a device")

        val cleanReason = validReason(reason) ?: return reasonRequired()
        val device = deviceRepository.find(deviceId) ?: return DeviceAdminResult.NotFound
        if (device.status != DeviceStatus.REVOKED) return DeviceAdminResult.Conflict("That device is not revoked")

        deviceRepository.reinstate(
            deviceId = deviceId,
            audit = auditEntry(actor, AuditActions.DEVICE_REINSTATE, device, cleanReason),
            now = now()
        )

        return DeviceAdminResult.Ok(deviceRepository.find(deviceId)!!)
    }

    suspend fun loginEvents(
        principal: AuthPrincipal,
        siteId: String?,
        userId: String?,
        outcome: String?,
        before: Long?,
        limit: Int?
    ): DeviceAdminResult<List<LoginEventDto>> {
        val actor = resolveActor(principal) ?: return forbidden()
        if (actor.role != Role.ADMIN) return DeviceAdminResult.Forbidden("Only an admin can read login events")

        return DeviceAdminResult.Ok(
            loginSecurity.events(siteId, userId, outcome, before, (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT))
        )
    }

    suspend fun lockouts(principal: AuthPrincipal): DeviceAdminResult<List<LockoutDto>> {
        val actor = resolveActor(principal) ?: return forbidden()
        if (actor.role != Role.ADMIN) return DeviceAdminResult.Forbidden("Only an admin can read lockouts")

        return DeviceAdminResult.Ok(loginSecurity.lockouts(now()))
    }

    private fun validReason(reason: String): String? = reason.trim().takeIf { it.length in 1..MAX_REASON_LENGTH }

    private fun reasonRequired() =
        DeviceAdminResult.Invalid("A reason of 1-$MAX_REASON_LENGTH characters is required")

    private class Actor(val userId: String, val username: String, val role: Role, val siteId: String?)

    private suspend fun resolveActor(principal: AuthPrincipal): Actor? {
        val record = userRepository.findById(principal.userId) ?: return null
        val role = Role.fromId(record.role) ?: return null
        if (!record.active || role == Role.WORKER) return null
        return Actor(record.userId, record.username, role, record.siteId)
    }

    /* The audit row's site is the phone's, so a manager can read it for their own site. */
    private fun auditEntry(actor: Actor, action: String, device: DeviceDto, reason: String) =
        AuditEntry(
            action = action,
            entityType = AuditActions.ENTITY_DEVICE,
            entityId = device.deviceId,
            actorUserId = actor.userId,
            actorUsername = actor.username,
            actorRole = actor.role.id,
            siteId = device.siteId,
            reason = reason,
            details = auditDetails("model" to (device.model ?: "unknown"), "lastUser" to (device.lastUsername ?: "unknown"))
        )

    private fun forbidden() = DeviceAdminResult.Forbidden("Forbidden")

    private companion object {
        const val MAX_REASON_LENGTH = 500
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 500
    }
}
