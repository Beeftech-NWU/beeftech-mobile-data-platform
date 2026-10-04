package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class VoidRequest(
    val reason: String = ""
)

@Serializable
data class VoidResponse(
    val entityType: String,
    val entityId: String,
    val voidedAt: Long
)

@Serializable
data class ReviewRecordDto(
    val type: String,
    val id: String,
    val label: String,
    val capturedAt: Long? = null,
    val submittedByUserId: String? = null,
    /* Filled in by the service from the users table; null if the user is gone. */
    val submittedByUsername: String? = null,
    val siteId: String? = null,
    val voidedAt: Long? = null,
    val voidedByUserId: String? = null,
    val voidReason: String? = null
)

@Serializable
data class AuditLogEntryDto(
    val id: Long,
    val action: String,
    val entityType: String,
    val entityId: String,
    val reason: String,
    val actorUserId: String,
    val actorUsername: String,
    val actorRole: Int,
    val siteId: String? = null,
    val createdAt: Long
)
