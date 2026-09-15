package com.beeftech.backend.api

import kotlinx.serialization.Serializable

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
