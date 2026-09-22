package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class AnimalMovementSyncRequest(
    val deviceId: String,
    val records: List<AnimalMovementSyncRecord>
)

@Serializable
data class AnimalMovementSyncRecord(
    val animalId: String,
    val movementType: String,
    val responsibleWorker: String,
    val timestamp: Long,
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0,
    val deviceId: String = "",
    val recordguid: String
)

@Serializable
data class AnimalMovementSyncResponse(
    val results: List<AnimalMovementSyncResult>
)

@Serializable
data class AnimalMovementSyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)