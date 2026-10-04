package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class SyncSecurityEventUpload(
    val eventKey: String,
    val eventType: String,
    val eventTime: Long,
    val warningDay: Int? = null,
    val pendingCount: Int = 0,
    val oldestPendingCreatedAt: Long? = null,
    val details: String? = null,
    /* Must be the caller's own id (or absent). Anyone else's events are rejected. */
    val userId: String? = null
)

@Serializable
data class SyncSecurityEventsRequest(
    val events: List<SyncSecurityEventUpload>
)

/* Every event is counted once: stored, already stored, or turned away. */
@Serializable
data class SyncSecurityUploadResult(
    val accepted: Int,
    val duplicates: Int,
    val rejected: Int
)

@Serializable
data class SyncSecurityEventDto(
    val id: Long,
    val deviceId: String,
    val userId: String,
    val username: String,
    val siteId: String? = null,
    val eventType: String,
    val eventTime: Long,
    val warningDay: Int? = null,
    val pendingCount: Int,
    val oldestPendingCreatedAt: Long? = null,
    val details: String? = null,
    val receivedAt: Long
)

/* An account whose phone reported a Day-7 lock that no admin has cleared since. */
@Serializable
data class LockedAccountDto(
    val userId: String,
    val username: String,
    val siteId: String? = null,
    val deviceId: String,
    val lockedAt: Long,
    val reason: String? = null
)

object SyncSecurityLimits {
    const val MAX_EVENTS_PER_REQUEST = 200
    const val DEFAULT_PAGE = 100
    const val MAX_PAGE = 500
}

@Serializable
data class LockedAccountCleared(
    val userId: String,
    val clearedAt: Long
)
