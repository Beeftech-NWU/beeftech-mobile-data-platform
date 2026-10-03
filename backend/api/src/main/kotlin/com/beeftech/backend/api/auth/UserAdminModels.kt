package com.beeftech.backend.api.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* Never carries the pin hash. */
@Serializable
data class UserSummary(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: Int?,
    @SerialName("site_id") val siteId: String? = null,
    val active: Boolean,
    @SerialName("device_assigned_id") val deviceAssignedId: String? = null,
    @SerialName("device_last_sync") val deviceLastSync: Long? = null
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val pin: String,
    val role: Int? = null,
    @SerialName("site_id") val siteId: String? = null
)

/* A null field means "leave unchanged". */
@Serializable
data class UpdateUserRequest(
    val role: Int? = null,
    @SerialName("site_id") val siteId: String? = null,
    val active: Boolean? = null
)

/* Omit pin to have the server generate one. */
@Serializable
data class ResetPinRequest(
    val pin: String? = null
)

/* The new pin is returned once, so the manager can hand it to the worker. */
@Serializable
data class ResetPinResponse(
    val pin: String
)
