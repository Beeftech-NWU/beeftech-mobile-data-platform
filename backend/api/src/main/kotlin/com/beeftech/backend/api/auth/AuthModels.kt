package com.beeftech.backend.api.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val pin: String,
    @SerialName("device_id") val deviceId: String,
    /* Sent by newer apps so the admin's device list can say which phone it is; absent from older ones. */
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("app_version") val appVersion: String? = null
)

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: Int?,
    @SerialName("pin_hash") val pinHash: String,
    @SerialName("device_assigned_id") val deviceAssignedId: String?,
    @SerialName("site_id") val siteId: String? = null,
    /* The site's four-character farm code, used to name files and sync batches. */
    @SerialName("farm_code") val farmCode: String? = null
)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
    val user: UserProfile
)

@Serializable
data class ProfileResponse(
    val username: String
)
