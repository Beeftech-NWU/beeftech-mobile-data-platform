package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class MortalitySyncRequest(
    val deviceId: String,
    val records: List<MortalitySyncRecord>,
    /* [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]; absent from app versions that predate it. */
    val batchName: String? = null
)

@Serializable
data class MortalitySyncRecord(
    val animalId: String,
    val causeOfDeath: String,
    val necropsyCodeId: String? = null,
    val responsibleWorker: String = "",
    val notes: String? = null,
    val timestamp: Long,
    val deviceId: String = "",
    val recordguid: String
)

@Serializable
data class MortalitySyncResponse(
    val results: List<MortalitySyncResult>
)

@Serializable
data class MortalitySyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class MortalityDto(
    val animalId: String,
    val causeOfDeath: String,
    val necropsyCodeId: String? = null,
    val responsibleWorker: String,
    val notes: String? = null,
    val timestamp: Long,
    val deviceId: String,
    val recordguid: String,
    val syncStatus: String,
    val syncedAt: Long? = null
)
