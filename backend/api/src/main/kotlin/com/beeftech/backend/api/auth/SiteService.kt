package com.beeftech.backend.api.auth

import com.beeftech.backend.api.AuditActions
import com.beeftech.backend.api.AuditEntry
import com.beeftech.backend.api.auditDetails
import java.util.UUID

sealed interface SiteResult<out T> {
    data class Ok<T>(val value: T) : SiteResult<T>
    data object NotFound : SiteResult<Nothing>
    data class Forbidden(val message: String) : SiteResult<Nothing>
    data class Invalid(val message: String) : SiteResult<Nothing>
    data class Conflict(val message: String) : SiteResult<Nothing>
}

/**
 * Site management. An admin lists, creates, renames and deactivates sites; a manager can
 * only read their own. There is no delete: users and records point at a site by id.
 *
 * As in UserAdminService, the caller's role, site and active flag come from the database,
 * not the token, so a deactivated or demoted user loses access immediately.
 */
class SiteService(
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun list(principal: AuthPrincipal): SiteResult<List<SiteDto>> {
        val actor = resolveActor(principal) ?: return forbidden()

        return when (actor.role) {
            Role.ADMIN -> SiteResult.Ok(siteRepository.list())
            /* A manager without a site sees nothing, as with scoped record reads. */
            else -> SiteResult.Ok(actor.siteId?.let { siteRepository.list(it) } ?: emptyList())
        }
    }

    suspend fun create(principal: AuthPrincipal, request: CreateSiteRequest): SiteResult<SiteDto> {
        val actor = resolveActor(principal) ?: return forbidden()
        if (actor.role != Role.ADMIN) return SiteResult.Forbidden("Only an admin can manage sites")

        val name = request.name.trim()
        validateName(name)?.let { return it }
        if (siteRepository.nameTaken(name)) return SiteResult.Conflict("A site with that name already exists")

        /* The server picks the id: hand-typed ids were the old way in (future-checks #27). */
        val siteId = generateSiteId()
        siteRepository.insert(
            siteId = siteId,
            name = name,
            now = now(),
            audit = auditEntry(actor, AuditActions.SITE_CREATE, siteId, auditDetails("name" to name))
        )

        return SiteResult.Ok(siteRepository.find(siteId)!!)
    }

    suspend fun update(principal: AuthPrincipal, siteId: String, request: UpdateSiteRequest): SiteResult<SiteDto> {
        val actor = resolveActor(principal) ?: return forbidden()
        if (actor.role != Role.ADMIN) return SiteResult.Forbidden("Only an admin can manage sites")

        val site = siteRepository.find(siteId) ?: return SiteResult.NotFound

        val name = request.name?.trim() ?: site.name
        val active = request.active ?: site.active

        if (name != site.name) {
            validateName(name)?.let { return it }
            if (siteRepository.nameTaken(name, exceptSiteId = site.siteId)) {
                return SiteResult.Conflict("A site with that name already exists")
            }
        }
        if (!active && site.active && site.activeUserCount > 0) {
            return SiteResult.Conflict(
                "This site still has ${site.activeUserCount} active user(s). Move or deactivate them first."
            )
        }

        val changes = buildList {
            if (name != site.name) add("name" to "${site.name}->$name")
            if (active != site.active) add("active" to "${site.active}->$active")
        }

        /* A call that changes nothing leaves no audit row. */
        if (changes.isNotEmpty()) {
            siteRepository.update(
                siteId = site.siteId,
                name = name,
                active = active,
                now = now(),
                audit = auditEntry(actor, AuditActions.SITE_UPDATE, site.siteId, auditDetails(*changes.toTypedArray()))
            )
        }

        return SiteResult.Ok(siteRepository.find(site.siteId)!!)
    }

    private fun validateName(name: String): SiteResult.Invalid? =
        if (name.length !in 1..MAX_NAME_LENGTH) {
            SiteResult.Invalid("Site name must be 1-$MAX_NAME_LENGTH characters")
        } else {
            null
        }

    private suspend fun generateSiteId(): String {
        repeat(5) {
            val id = "site-" + UUID.randomUUID().toString().replace("-", "").take(8)
            if (siteRepository.find(id) == null) return id
        }
        error("Could not generate a unique site id")
    }

    private class Actor(val userId: String, val username: String, val role: Role, val siteId: String?)

    private suspend fun resolveActor(principal: AuthPrincipal): Actor? {
        val record = userRepository.findById(principal.userId) ?: return null
        val role = Role.fromId(record.role) ?: return null
        if (!record.active || role == Role.WORKER) return null
        return Actor(record.userId, record.username, role, record.siteId)
    }

    /* The audit row's site is the site itself, so a manager can read it for their own site. */
    private fun auditEntry(actor: Actor, action: String, siteId: String, details: kotlinx.serialization.json.JsonObject) =
        AuditEntry(
            action = action,
            entityType = AuditActions.ENTITY_SITE,
            entityId = siteId,
            actorUserId = actor.userId,
            actorUsername = actor.username,
            actorRole = actor.role.id,
            siteId = siteId,
            details = details
        )

    private fun forbidden() = SiteResult.Forbidden("Forbidden")

    private companion object {
        const val MAX_NAME_LENGTH = 100
    }
}
