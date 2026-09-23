package com.beeftech.backend.api.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val pin: String,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: Int?,
    @SerialName("pin_hash") val pinHash: String,
    @SerialName("device_assigned_id") val deviceAssignedId: String?
)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
    val user: UserProfile
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

@Serializable
data class ProfileResponse(
    val username: String
)
