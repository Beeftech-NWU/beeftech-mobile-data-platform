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
