package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UserRepository

sealed interface ReferenceDataResult<out T> {
    data class Ok<T>(val value: T) : ReferenceDataResult<T>
    data object NotFound : ReferenceDataResult<Nothing>
    data class Forbidden(val message: String) : ReferenceDataResult<Nothing>
    data class Invalid(val message: String) : ReferenceDataResult<Nothing>
    data class Conflict(val message: String) : ReferenceDataResult<Nothing>
}

/**
 * Reference values (diseases, treatment types, cost types). Anyone signed in reads them; only
 * an admin adds one or turns one on or off. There is no rename and no delete: the app's records
 * point at a value by its name or code, so it can only be hidden from pickers.
 *
 * As elsewhere, the caller's role and active flag come from the database, not the token.
 */
class ReferenceDataService(
    private val userRepository: UserRepository,
    private val repository: ReferenceDataRepository,
    private val now: () -> Long = System::currentTimeMillis
) {

    /* [ifVersion] is the version the device already has; when it's current only the version comes back. */
    suspend fun snapshot(ifVersion: Long?): ReferenceDataResult<ReferenceDataSnapshot> {
        if (ifVersion != null && ifVersion == repository.version()) {
            return ReferenceDataResult.Ok(ReferenceDataSnapshot(version = ifVersion, unchanged = true))
        }
        return ReferenceDataResult.Ok(repository.snapshot())
    }

    suspend fun create(principal: AuthPrincipal, slug: String, request: CreateReferenceItemRequest): ReferenceDataResult<ReferenceChangeResponse> {
        val actor = resolveAdmin(principal) ?: return forbidden()
        val kind = ReferenceKind.fromSlug(slug) ?: return ReferenceDataResult.NotFound

        val outcome = if (kind == ReferenceKind.COST_TYPES) {
            val code = request.code?.trim().orEmpty()
            val displayName = request.displayName?.trim().orEmpty()
            if (!COST_CODE.matches(code)) {
                return ReferenceDataResult.Invalid("Code must be capital letters, digits and underscores, starting with a letter (2-64 characters)")
            }
            if (displayName.length !in 1..MAX_NAME_LENGTH) {
                return ReferenceDataResult.Invalid("Name must be 1-$MAX_NAME_LENGTH characters")
            }
            repository.createCostType(
                code, displayName, request.sortOrder,
                { entry -> auditEntry(actor, AuditActions.REFDATA_CREATE, kind, entry, auditDetails("code" to code, "name" to displayName)) },
                now()
            )
        } else {
            val name = request.name?.trim().orEmpty()
            if (name.length !in 1..MAX_NAME_LENGTH) {
                return ReferenceDataResult.Invalid("Name must be 1-$MAX_NAME_LENGTH characters")
            }
            repository.createNamed(
                kind, name,
                { entry -> auditEntry(actor, AuditActions.REFDATA_CREATE, kind, entry, auditDetails("name" to name)) },
                now()
            )
        }

        return when (outcome) {
            is ReferenceChangeOutcome.Changed -> ReferenceDataResult.Ok(ReferenceChangeResponse(outcome.version, outcome.entry))
            is ReferenceChangeOutcome.Unchanged -> ReferenceDataResult.Ok(ReferenceChangeResponse(outcome.version, outcome.entry))
            ReferenceChangeOutcome.Duplicate -> ReferenceDataResult.Conflict("That ${kind.label()} already exists")
            ReferenceChangeOutcome.NotFound -> ReferenceDataResult.NotFound
        }
    }

    suspend fun setActive(principal: AuthPrincipal, slug: String, id: String, active: Boolean): ReferenceDataResult<ReferenceChangeResponse> {
        val actor = resolveAdmin(principal) ?: return forbidden()
        val kind = ReferenceKind.fromSlug(slug) ?: return ReferenceDataResult.NotFound

        /* Saving a treatment writes a TREATMENT cost, so that type has to stay on. */
        if (kind == ReferenceKind.COST_TYPES && id == PROTECTED_COST_CODE && !active) {
            return ReferenceDataResult.Conflict("The Treatment cost type can't be turned off: every treatment records a cost with it")
        }

        val action = if (active) AuditActions.REFDATA_ACTIVATE else AuditActions.REFDATA_DEACTIVATE
        val outcome = repository.setActive(
            kind, id, active,
            { entry -> auditEntry(actor, action, kind, entry, auditDetails("name" to entry.name, "active" to "${!active}->$active")) },
            now()
        )

        return when (outcome) {
            is ReferenceChangeOutcome.Changed -> ReferenceDataResult.Ok(ReferenceChangeResponse(outcome.version, outcome.entry))
            is ReferenceChangeOutcome.Unchanged -> ReferenceDataResult.Ok(ReferenceChangeResponse(outcome.version, outcome.entry))
            ReferenceChangeOutcome.NotFound -> ReferenceDataResult.NotFound
            ReferenceChangeOutcome.Duplicate -> ReferenceDataResult.Conflict("Conflict")
        }
    }

    private class Actor(val userId: String, val username: String)

    private suspend fun resolveAdmin(principal: AuthPrincipal): Actor? {
        val record = userRepository.findById(principal.userId) ?: return null
        if (!record.active || Role.fromId(record.role) != Role.ADMIN) return null
        return Actor(record.userId, record.username)
    }

    /* Reference data has no site, so only admins see these rows in the audit log. */
    private fun auditEntry(
        actor: Actor,
        action: String,
        kind: ReferenceKind,
        entry: ReferenceEntryDto,
        details: kotlinx.serialization.json.JsonObject
    ) = AuditEntry(
        action = action,
        entityType = kind.entityType,
        entityId = entry.id,
        actorUserId = actor.userId,
        actorUsername = actor.username,
        actorRole = Role.ADMIN.id,
        siteId = null,
        details = details
    )

    private fun ReferenceKind.label() = when (this) {
        ReferenceKind.DISEASES -> "disease"
        ReferenceKind.TREATMENT_TYPES -> "treatment type"
        ReferenceKind.COST_TYPES -> "cost type"
    }

    private fun forbidden() = ReferenceDataResult.Forbidden("Only an admin can change reference data")

    private companion object {
        const val MAX_NAME_LENGTH = 100
        const val PROTECTED_COST_CODE = "TREATMENT"
        val COST_CODE = Regex("^[A-Z][A-Z0-9_]{1,63}$")
    }
}
