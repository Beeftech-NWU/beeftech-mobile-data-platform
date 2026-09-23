package com.beeftech.calfregistration.data

import kotlinx.serialization.Serializable

/**
 * Network DTOs mirroring the `backend:api` contract for calf registration
 * sync (`POST /api/calf-registrations/sync`). These are intentionally kept identical
 * in shape to `com.beeftech.backend.api.*` so that Kotlinx serialization round-trips
 * cleanly against the real server; the backend module is not a compile-time
 * dependency of this Android module, so the shapes are duplicated here.
 */

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class CalfRegistrationDto(
    val animalId: String,
    val birthdate: Long,
    val breed: String,
    val damId: String? = null,
    val sireId: String? = null,
    val photoPath: String? = null,
    val videoPath: String? = null,
    val gpsLat: Double,
    val gpsLng: Double,
    val captureAt: Long,
    val deviceId: String,
    val recordguid: String,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)

@Serializable
data class CalfRegistrationSyncRequest(
    val deviceId: String,
    val records: List<CalfRegistrationDto>
)

@Serializable
data class CalfRegistrationSyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String, // "SYNCED" or "ERROR"
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class CalfRegistrationSyncResponse(
    val results: List<CalfRegistrationSyncResult>
)
