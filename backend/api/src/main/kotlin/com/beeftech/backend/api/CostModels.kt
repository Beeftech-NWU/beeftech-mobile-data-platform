package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class CostSyncRequest(
    val deviceId: String,
    val records: List<CostSyncRecord>,
    /* [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]; absent from app versions that predate it. */
    val batchName: String? = null
)

@Serializable
data class CostSyncRecord(
    val animalId: String,
    val costType: String,
    val amount: Double,
    val description: String = "",
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0,
    val timestamp: Long,
    val sourceEntity: String? = null,
    val sourceRecordId: String? = null,
    val deviceId: String = "",
    val recordguid: String
)

@Serializable
data class CostSyncResponse(
    val results: List<CostSyncResult>
)

@Serializable
data class CostSyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class CostDto(
    val animalId: String,
    val costType: String,
    val amount: Double,
    val description: String,
    val gpsLat: Double,
    val gpsLng: Double,
    val timestamp: Long,
    val sourceEntity: String? = null,
    val sourceRecordId: String? = null,
    val deviceId: String,
    val recordguid: String,
    val syncStatus: String,
    val syncedAt: Long? = null
)
