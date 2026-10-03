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
