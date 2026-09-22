package com.beeftech.authentication.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val pin: String,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class UserProfileDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: Int?,
    @SerialName("pin_hash") val pinHash: String,
    @SerialName("device_assigned_id") val deviceAssignedId: String? = null
)

@Serializable
data class LoginResponseDto(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
    val user: UserProfileDto
)
