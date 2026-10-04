package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UserRepository

sealed interface SyncPolicyResult<out T> {
    data class Ok<T>(val value: T) : SyncPolicyResult<T>
    data class Forbidden(val message: String) : SyncPolicyResult<Nothing>
    data class Invalid(val message: String) : SyncPolicyResult<Nothing>
}

/**
 * The sync policy the phones follow. Anyone signed in reads it (phones pull it); only an admin
 * changes it, and only the warning days and the stale-sync alert. The wipe day is not a setting.
 *
 * As elsewhere, the caller's role and active flag come from the database, not the token.
 */
class SyncPolicyService(
    private val userRepository: UserRepository,
    private val repository: SyncPolicyRepository,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun get(): SyncPolicyResult<SyncPolicyDto> = SyncPolicyResult.Ok(repository.get())

    suspend fun update(principal: AuthPrincipal, request: UpdateSyncPolicyRequest): SyncPolicyResult<SyncPolicyDto> {
        val record = userRepository.findById(principal.userId)
        if (record == null || !record.active || Role.fromId(record.role) != Role.ADMIN) {
            return SyncPolicyResult.Forbidden("Only an admin can change the sync policy")
        }

        SyncPolicyValidation.warningDaysError(request.warningDays)?.let { return SyncPolicyResult.Invalid(it) }
        SyncPolicyValidation.staleHoursError(request.staleSyncAlertHours)?.let { return SyncPolicyResult.Invalid(it) }

        val outcome = repository.update(
            warningDays = request.warningDays,
            staleSyncAlertHours = request.staleSyncAlertHours,
            updatedByUserId = record.userId,
            audit = { before, after ->
                AuditEntry(
                    action = AuditActions.SYNC_POLICY_UPDATE,
                    entityType = AuditActions.ENTITY_SYNC_POLICY,
                    entityId = "sync",
                    actorUserId = record.userId,
                    actorUsername = record.username,
                    actorRole = Role.ADMIN.id,
                    /* The policy is system-wide, so only admins see this row in the audit log. */
                    siteId = null,
                    details = auditDetails(
                        "warningDays" to "${before.warningDays.joinToString(",")}->${after.warningDays.joinToString(",")}",
                        "staleSyncAlertHours" to "${before.staleSyncAlertHours}->${after.staleSyncAlertHours}"
                    )
                )
            },
            now = now()
        )

        return when (outcome) {
            is SyncPolicyUpdateOutcome.Changed -> SyncPolicyResult.Ok(outcome.policy)
            is SyncPolicyUpdateOutcome.Unchanged -> SyncPolicyResult.Ok(outcome.policy)
        }
    }
}
