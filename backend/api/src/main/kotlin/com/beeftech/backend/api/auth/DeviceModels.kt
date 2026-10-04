package com.beeftech.backend.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val deviceId: String,
    val model: String? = null,
    val appVersion: String? = null,
    /* ACTIVE or REVOKED. */
    val status: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val lastUserId: String? = null,
    val lastUsername: String? = null,
    val siteId: String? = null,
    val revokedAt: Long? = null,
    val revokedByUserId: String? = null,
    val revokeReason: String? = null,
    /* Users whose assigned phone this is. */
    val boundUsernames: List<String> = emptyList()
)

/* Used to revoke and to reinstate; the reason is required for both. */
@Serializable
data class DeviceReasonRequest(
    val reason: String = ""
)

@Serializable
data class LoginEventDto(
    val id: Long,
    val createdAt: Long,
    /* Whatever was typed, including names that don't exist. */
    val usernameAttempted: String,
    val userId: String? = null,
    val deviceId: String,
    val outcome: String,
    val siteId: String? = null,
    val appVersion: String? = null
)

@Serializable
data class LockoutDto(
    val username: String,
    val failedAttempts: Int,
    val lockedUntil: Long
)
