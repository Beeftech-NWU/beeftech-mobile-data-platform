package com.beeftech.management.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* Role ids match the backend: ADMIN = 1, MANAGER = 2, WORKER = 3. */
fun roleLabel(role: Int?): String =
    when (role) {
        1 -> "Admin"
        2 -> "Manager"
        3 -> "Worker"
        else -> "Unknown"
    }

@Serializable
data class TeamMember(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: Int? = null,
    @SerialName("site_id") val siteId: String? = null,
    val active: Boolean = true,
    @SerialName("device_assigned_id") val deviceAssignedId: String? = null,
    @SerialName("device_last_sync") val deviceLastSync: Long? = null
)

@Serializable
data class CreateUserBody(
    val username: String,
    val pin: String,
    val role: Int? = null,
    @SerialName("site_id") val siteId: String? = null
)

@Serializable
data class UpdateUserBody(
    val role: Int? = null,
    @SerialName("site_id") val siteId: String? = null,
    val active: Boolean? = null
)

@Serializable
data class ResetPinBody(
    val pin: String? = null
)

@Serializable
data class ResetPinResult(
    val pin: String
)

@Serializable
data class RecordCount(
    val total: Long = 0,
    val last7Days: Long = 0
)

@Serializable
data class TreatmentCount(
    val total: Long = 0,
    val last7Days: Long = 0,
    val totalCost: Double = 0.0
)

@Serializable
data class TeamCount(
    val activeWorkers: Long = 0,
    val inactiveWorkers: Long = 0
)

@Serializable
data class DashboardAlert(
    val type: String,
    val message: String,
    val username: String? = null,
    val lastSyncAt: Long? = null
)

@Serializable
data class DashboardSummary(
    val siteId: String? = null,
    /* The site's name when the summary is for one site; null for all sites or an older server. */
    val siteName: String? = null,
    val generatedAt: Long = 0,
    val calves: RecordCount = RecordCount(),
    val treatments: TreatmentCount = TreatmentCount(),
    val farmers: RecordCount = RecordCount(),
    val team: TeamCount = TeamCount(),
    val alerts: List<DashboardAlert> = emptyList()
)

/* The record types a manager can review and void; slugs match the backend's /api/records/{type}. */
val REVIEW_TYPES: List<Pair<String, String>> = listOf(
    "calf-registrations" to "Calves",
    "treatments" to "Treatments",
    "farmers" to "Farmers",
    "animal-movements" to "Movements",
    "mortalities" to "Mortalities"
)

@Serializable
data class ReviewRecord(
    val type: String,
    val id: String,
    val label: String,
    val capturedAt: Long? = null,
    val submittedByUserId: String? = null,
    val submittedByUsername: String? = null,
    val siteId: String? = null,
    val voidedAt: Long? = null,
    val voidedByUserId: String? = null,
    val voidReason: String? = null
) {
    val isVoided: Boolean get() = voidedAt != null
}

@Serializable
data class VoidBody(
    val reason: String
)

@Serializable
data class VoidResult(
    val entityType: String,
    val entityId: String,
    val voidedAt: Long
)

@Serializable
data class Site(
    val siteId: String,
    val name: String,
    val active: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long? = null,
    /* Active users of any role on this site. */
    val activeUserCount: Long = 0
)

@Serializable
data class CreateSiteBody(
    val name: String
)

/* A null field is left out of the request and means "leave unchanged". */
@Serializable
data class UpdateSiteBody(
    val name: String? = null,
    val active: Boolean? = null
)

@Serializable
data class Device(
    val deviceId: String,
    val model: String? = null,
    val appVersion: String? = null,
    /* ACTIVE or REVOKED. */
    val status: String = "ACTIVE",
    val firstSeenAt: Long = 0,
    val lastSeenAt: Long = 0,
    val lastUserId: String? = null,
    val lastUsername: String? = null,
    val siteId: String? = null,
    val revokedAt: Long? = null,
    val revokedByUserId: String? = null,
    val revokeReason: String? = null,
    /* Users whose assigned phone this is. */
    val boundUsernames: List<String> = emptyList()
) {
    val isRevoked: Boolean get() = status == "REVOKED"
}

/* Used to block (revoke) and to unblock (reinstate) a phone; the reason is required for both. */
@Serializable
data class DeviceReasonBody(
    val reason: String
)

@Serializable
data class LoginEvent(
    val id: Long,
    val createdAt: Long,
    /* Whatever was typed, including names that don't exist. */
    val usernameAttempted: String = "",
    val userId: String? = null,
    val deviceId: String = "",
    val outcome: String,
    val siteId: String? = null,
    val appVersion: String? = null
)

@Serializable
data class Lockout(
    val username: String,
    /* Null when the locked name isn't a real user (someone guessing). */
    val userId: String? = null,
    val failedAttempts: Int = 0,
    val lockedUntil: Long = 0
)

/* Login outcomes the events can be filtered by: the backend's value to a label. */
val LOGIN_OUTCOMES = listOf(
    "SUCCESS" to "Signed in",
    "BAD_CREDENTIALS" to "Wrong PIN",
    "UNKNOWN_USER" to "Unknown user",
    "LOCKED" to "Locked out",
    "INACTIVE" to "Deactivated",
    "WRONG_DEVICE" to "Wrong phone",
    "DEVICE_REVOKED" to "Blocked phone"
)

fun loginOutcomeLabel(outcome: String): String =
    LOGIN_OUTCOMES.firstOrNull { it.first == outcome }?.second ?: outcome

@Serializable
data class AuditLogEntry(
    val id: Long,
    val action: String,
    val entityType: String,
    val entityId: String,
    val reason: String = "",
    val actorUserId: String = "",
    val actorUsername: String = "",
    val actorRole: Int? = null,
    val siteId: String? = null,
    val createdAt: Long,
    /* A JSON object of what changed, e.g. {"role":"3->2"}; null for a void and older rows. */
    val details: String? = null
)

/* A disease or treatment type in the server's reference-data snapshot. */
@Serializable
data class ReferenceValueDto(
    val id: Int,
    val name: String,
    val active: Boolean = true
)

@Serializable
data class CostTypeDto(
    val code: String,
    val displayName: String,
    val sortOrder: Int = 0,
    val active: Boolean = true
)

/*
 * Everything the server publishes, inactive values included. When the version the caller sent is
 * current, [unchanged] is true and the lists are null.
 */
@Serializable
data class ReferenceSnapshotDto(
    val version: Long,
    val unchanged: Boolean = false,
    val diseases: List<ReferenceValueDto>? = null,
    val treatmentTypes: List<ReferenceValueDto>? = null,
    val costTypes: List<CostTypeDto>? = null
)

/* One value as a flat entry, the same shape for every kind; [id] is the code for cost types. */
@Serializable
data class ReferenceEntry(
    val kind: String,
    val id: String,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int? = null
)

@Serializable
data class ReferenceChange(
    val version: Long,
    val item: ReferenceEntry
)

/* name for diseases and treatment types; code and displayName (and optional sortOrder) for cost types. */
@Serializable
data class CreateReferenceBody(
    val name: String? = null,
    val code: String? = null,
    val displayName: String? = null,
    val sortOrder: Int? = null
)

@Serializable
data class SetReferenceActiveBody(
    val active: Boolean
)

/* The kinds of reference value, with the path segment the API uses. */
val REFERENCE_KINDS = listOf(
    "diseases" to "Diseases",
    "treatment-types" to "Treatment types",
    "cost-types" to "Cost types"
)

/* Audit actions the log can be filtered by: the backend's AuditActions value to a label. */
val AUDIT_ACTIONS = listOf(
    "VOID" to "Voids",
    "USER_CREATE" to "User created",
    "USER_UPDATE" to "User changed",
    "USER_RESET_PIN" to "PIN reset",
    "USER_UNBIND_DEVICE" to "Phone unbound",
    "LOGIN_UNLOCK" to "Login unlocked",
    "SITE_CREATE" to "Site created",
    "SITE_UPDATE" to "Site changed",
    "DEVICE_REVOKE" to "Phone blocked",
    "DEVICE_REINSTATE" to "Phone unblocked",
    "REFDATA_CREATE" to "Value added",
    "REFDATA_ACTIVATE" to "Value turned on",
    "REFDATA_DEACTIVATE" to "Value turned off"
)

fun auditActionLabel(action: String): String =
    AUDIT_ACTIONS.firstOrNull { it.first == action }?.second ?: action

@Serializable
internal data class Envelope<T>(
    val success: Boolean = false,
    val message: String = "",
    val data: T? = null
)

sealed class ManagementResult<out T> {
    data class Success<T>(val value: T) : ManagementResult<T>()

    /* The screens are online-only, so this gets its own "needs connection" state. */
    data object NoConnection : ManagementResult<Nothing>()

    data object Unauthorized : ManagementResult<Nothing>()
    data class Forbidden(val message: String) : ManagementResult<Nothing>()
    data object NotFound : ManagementResult<Nothing>()
    data class Rejected(val message: String) : ManagementResult<Nothing>()
    data class Error(val message: String) : ManagementResult<Nothing>()
}
