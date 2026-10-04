package com.beeftech.backend.api.auth

import com.beeftech.backend.api.AuditActions
import com.beeftech.backend.api.AuditEntry
import com.beeftech.backend.api.auditDetails
import kotlinx.serialization.json.JsonObject
import java.security.SecureRandom
import java.util.UUID

sealed interface UserAdminResult<out T> {
    data class Ok<T>(val value: T) : UserAdminResult<T>
    data object NotFound : UserAdminResult<Nothing>
    data class Forbidden(val message: String) : UserAdminResult<Nothing>
    data class Invalid(val message: String) : UserAdminResult<Nothing>
    data class Conflict(val message: String) : UserAdminResult<Nothing>
}

/**
 * User management for admins (any user) and managers (workers on their own site).
 *
 * The caller's role and site are read from the database, not the token, so a
 * deactivated or demoted manager loses access straight away instead of
 * keeping it for the rest of their 24 h token.
 */
class UserAdminService(
    private val userRepository: UserRepository
) {

    suspend fun list(principal: AuthPrincipal, siteId: String?): UserAdminResult<List<UserSummary>> {
        val actor = resolveActor(principal) ?: return forbidden()

        val users = when (actor.roleEnum) {
            Role.ADMIN -> userRepository.list(siteId = siteId, role = null)
            /* A manager without a site manages nobody. */
            else -> actor.siteId?.let { userRepository.list(siteId = it, role = Role.WORKER.id) }
                ?: emptyList()
        }

        return UserAdminResult.Ok(users.map { it.toSummary() })
    }

    suspend fun create(principal: AuthPrincipal, request: CreateUserRequest): UserAdminResult<UserSummary> {
        val actor = resolveActor(principal) ?: return forbidden()

        val username = request.username.trim()
        if (username.length !in 3..64 || username.any { it.isWhitespace() }) {
            return UserAdminResult.Invalid("Username must be 3-64 characters with no spaces")
        }
        if (!isValidPin(request.pin)) {
            return UserAdminResult.Invalid("PIN must be exactly $PIN_LENGTH digits")
        }

        val role: Role
        val siteId: String?
        if (actor.roleEnum == Role.ADMIN) {
            role = if (request.role == null) Role.WORKER else Role.fromId(request.role)
                ?: return UserAdminResult.Invalid("Unknown role")
            siteId = request.siteId
        } else {
            val ownSite = actor.siteId
                ?: return UserAdminResult.Forbidden("Your account has no site")
            if (request.role != null && request.role != Role.WORKER.id) {
                return UserAdminResult.Forbidden("Managers can only create workers")
            }
            if (request.siteId != null && request.siteId != ownSite) {
                return UserAdminResult.Forbidden("Managers can only create users on their own site")
            }
            role = Role.WORKER
            siteId = ownSite
        }

        checkSite(role, siteId, requireActive = true)?.let { return it }

        if (userRepository.findByUsername(username) != null) {
            return UserAdminResult.Conflict("Username already taken")
        }

        val userId = UUID.randomUUID().toString()
        userRepository.insertUser(
            userId = userId,
            username = username,
            pinHash = PinHasher.hash(request.pin),
            role = role.id,
            siteId = siteId,
            audit = auditEntry(
                actor, AuditActions.USER_CREATE, userId, siteId,
                auditDetails("username" to username, "role" to role.id.toString(), "siteId" to (siteId ?: "none"))
            )
        )

        return UserAdminResult.Ok(userRepository.findById(userId)!!.toSummary())
    }

    suspend fun update(
        principal: AuthPrincipal,
        targetId: String,
        request: UpdateUserRequest
    ): UserAdminResult<UserSummary> {
        val actor = resolveActor(principal) ?: return forbidden()
        val target = findManageable(actor, targetId) ?: return UserAdminResult.NotFound

        if (actor.roleEnum != Role.ADMIN && (request.role != null || request.siteId != null)) {
            return UserAdminResult.Forbidden("Only an admin can change a role or site")
        }

        if (actor.userId == target.userId &&
            (request.active == false || (request.role != null && request.role != target.role))
        ) {
            return UserAdminResult.Conflict("You can't deactivate or change the role of your own account")
        }

        val role = if (request.role == null) Role.fromId(target.role) else Role.fromId(request.role)
            ?: return UserAdminResult.Invalid("Unknown role")
        val siteId = request.siteId ?: target.siteId

        if (request.role != null || request.siteId != null) {
            /* Only a newly assigned site has to be active, so a role change at an inactive site still works. */
            checkSite(role, siteId, requireActive = request.siteId != null && request.siteId != target.siteId)
                ?.let { return it }
        }

        val newRole = role?.id ?: target.role
        val newActive = request.active ?: target.active
        val changes = buildList {
            if (newRole != target.role) add("role" to "${target.role}->$newRole")
            if (siteId != target.siteId) add("siteId" to "${target.siteId ?: "none"}->${siteId ?: "none"}")
            if (newActive != target.active) add("active" to "${target.active}->$newActive")
        }

        userRepository.updateAccount(
            userId = target.userId,
            role = newRole,
            siteId = siteId,
            active = newActive,
            /* A call that changes nothing leaves no audit row. */
            audit = changes.takeIf { it.isNotEmpty() }?.let {
                auditEntry(actor, AuditActions.USER_UPDATE, target.userId, siteId, auditDetails(*it.toTypedArray()))
            }
        )

        return UserAdminResult.Ok(userRepository.findById(target.userId)!!.toSummary())
    }

    suspend fun resetPin(
        principal: AuthPrincipal,
        targetId: String,
        requestedPin: String?
    ): UserAdminResult<ResetPinResponse> {
        val actor = resolveActor(principal) ?: return forbidden()
        val target = findManageable(actor, targetId) ?: return UserAdminResult.NotFound

        if (requestedPin != null && !isValidPin(requestedPin)) {
            return UserAdminResult.Invalid("PIN must be exactly $PIN_LENGTH digits")
        }

        val pin = requestedPin ?: generatePin()
        /* No details: the new PIN must never reach the log. */
        userRepository.updatePinHash(
            target.userId,
            PinHasher.hash(pin),
            auditEntry(actor, AuditActions.USER_RESET_PIN, target.userId, target.siteId)
        )

        return UserAdminResult.Ok(ResetPinResponse(pin))
    }

    suspend fun unbindDevice(principal: AuthPrincipal, targetId: String): UserAdminResult<UserSummary> {
        val actor = resolveActor(principal) ?: return forbidden()
        val target = findManageable(actor, targetId) ?: return UserAdminResult.NotFound

        userRepository.clearDevice(
            target.userId,
            auditEntry(
                actor, AuditActions.USER_UNBIND_DEVICE, target.userId, target.siteId,
                auditDetails("device" to "${target.deviceAssignedId ?: "none"}->none")
            )
        )

        return UserAdminResult.Ok(userRepository.findById(target.userId)!!.toSummary())
    }

    private suspend fun resolveActor(principal: AuthPrincipal): ActorRecord? {
        val record = userRepository.findById(principal.userId) ?: return null
        val role = Role.fromId(record.role) ?: return null
        if (!record.active || role == Role.WORKER) return null
        return ActorRecord(record.userId, record.username, role, record.siteId)
    }

    /* Out-of-scope targets look like missing ones, as with scoped record reads. */
    private suspend fun findManageable(actor: ActorRecord, targetId: String): UserRecord? {
        val target = userRepository.findById(targetId) ?: return null
        if (actor.roleEnum == Role.ADMIN) return target
        val inScope = target.role == Role.WORKER.id &&
            actor.siteId != null &&
            target.siteId == actor.siteId
        return target.takeIf { inScope }
    }

    private suspend fun checkSite(role: Role?, siteId: String?, requireActive: Boolean): UserAdminResult<Nothing>? {
        if (siteId != null) {
            val active = userRepository.siteActive(siteId) ?: return UserAdminResult.Invalid("Unknown site")
            if (requireActive && !active) return UserAdminResult.Invalid("That site is inactive")
        }
        if (role != Role.ADMIN && siteId == null) {
            return UserAdminResult.Invalid("Managers and workers need a site")
        }
        return null
    }

    /* The audit row's site is the affected user's, so a manager can read it for their own site. */
    private fun auditEntry(
        actor: ActorRecord,
        action: String,
        targetUserId: String,
        siteId: String?,
        details: JsonObject? = null
    ) = AuditEntry(
        action = action,
        entityType = AuditActions.ENTITY_USER,
        entityId = targetUserId,
        actorUserId = actor.userId,
        actorUsername = actor.username,
        actorRole = actor.roleEnum.id,
        siteId = siteId,
        details = details
    )

    private fun forbidden() = UserAdminResult.Forbidden("Forbidden")

    private fun isValidPin(pin: String) = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    private fun generatePin(): String =
        (1..PIN_LENGTH).joinToString("") { random.nextInt(10).toString() }

    private fun UserRecord.toSummary() = UserSummary(
        userId = userId,
        username = username,
        role = role,
        siteId = siteId,
        active = active,
        deviceAssignedId = deviceAssignedId,
        deviceLastSync = deviceLastSync
    )

    private class ActorRecord(val userId: String, val username: String, val roleEnum: Role, val siteId: String?)

    private companion object {
        const val PIN_LENGTH = 5
        val random = SecureRandom()
    }
}
