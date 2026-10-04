package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UserRepository

sealed interface VoidResult<out T> {
    data class Ok<T>(val value: T) : VoidResult<T>
    data object NotFound : VoidResult<Nothing>
    data class Forbidden(val message: String) : VoidResult<Nothing>
    data class Invalid(val message: String) : VoidResult<Nothing>
    data class Conflict(val message: String) : VoidResult<Nothing>
}

/**
 * Voiding and reviewing records for admins (any site) and managers (their own site).
 *
 * As in UserAdminService, the caller's role, site and active flag come from the
 * database rather than the token, so a deactivated or demoted manager loses
 * access immediately.
 */
class VoidService(
    private val userRepository: UserRepository,
    private val repository: VoidRepository
) {

    suspend fun void(
        principal: AuthPrincipal,
        slug: String,
        id: String,
        request: VoidRequest
    ): VoidResult<VoidResponse> {

        val actor = resolveActor(principal) ?: return VoidResult.Forbidden("Forbidden")

        val target = VOID_TARGETS.firstOrNull { it.slug == slug } ?: return VoidResult.NotFound

        val reason = request.reason.trim()
        if (reason.isEmpty()) return VoidResult.Invalid("A reason is required")
        if (reason.length > MAX_REASON_LENGTH) {
            return VoidResult.Invalid("The reason must be at most $MAX_REASON_LENGTH characters")
        }

        val siteScope = siteScope(actor) ?: return VoidResult.NotFound

        return when (
            val outcome = repository.void(
                target = target,
                id = id,
                reason = reason,
                actorUserId = actor.userId,
                actorUsername = actor.username,
                actorRole = actor.role.id,
                siteScope = siteScope
            )
        ) {
            is VoidOutcome.Voided -> VoidResult.Ok(VoidResponse(target.entityType, id, outcome.voidedAt))
            VoidOutcome.NotFound -> VoidResult.NotFound
            VoidOutcome.AlreadyVoided -> VoidResult.Conflict("Record is already voided")
        }
    }

    suspend fun review(
        principal: AuthPrincipal,
        slug: String,
        includeVoided: Boolean,
        limit: Int?
    ): VoidResult<List<ReviewRecordDto>> {

        val actor = resolveActor(principal) ?: return VoidResult.Forbidden("Forbidden")

        val target = VOID_TARGETS.firstOrNull { it.slug == slug } ?: return VoidResult.NotFound

        /* A manager without a site sees no records, as with scoped record reads. */
        val siteScope = siteScope(actor) ?: return VoidResult.Ok(emptyList())

        val records = repository.review(
            target, siteScope, includeVoided, (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        )

        val usernames = records
            .mapNotNull { it.submittedByUserId }
            .distinct()
            .associateWith { userRepository.findById(it)?.username }

        return VoidResult.Ok(records.map { it.copy(submittedByUsername = usernames[it.submittedByUserId]) })
    }

    private class Actor(val userId: String, val username: String, val role: Role, val siteId: String?)

    private suspend fun resolveActor(principal: AuthPrincipal): Actor? {
        val record = userRepository.findById(principal.userId) ?: return null
        val role = Role.fromId(record.role) ?: return null
        if (!record.active || role == Role.WORKER) return null
        return Actor(record.userId, record.username, role, record.siteId)
    }

    /* Null for a manager without a site: they can act on nothing. */
    private fun siteScope(actor: Actor): SiteScope? =
        when (actor.role) {
            Role.ADMIN -> SiteScope.Any
            else -> actor.siteId?.let { SiteScope.Only(it) }
        }

    private companion object {
        const val MAX_REASON_LENGTH = 500
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 500
    }
}
