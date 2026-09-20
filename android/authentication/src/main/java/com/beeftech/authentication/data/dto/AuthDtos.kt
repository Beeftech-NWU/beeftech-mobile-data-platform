package com.beeftech.authentication.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTOs mirroring the `backend:api` auth contract
 * (`POST /api/auth/login`). Field names use @SerialName to map to the
 * backend's snake_case JSON (per the login implementation plan's sample
 * response) - CONFIRM THIS WITH THE BACKEND DEV, since the rest of this
 * codebase (CalfRegistrationDto) uses camelCase-on-wire with no mapping.
 */

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val pin: String,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
    val user: UserProfileDto
)

@Serializable
data class UserProfileDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: Long,
    @SerialName("pin_hash") val pinHash: String,
    @SerialName("device_assigned_id") val deviceAssignedId: String?
)

