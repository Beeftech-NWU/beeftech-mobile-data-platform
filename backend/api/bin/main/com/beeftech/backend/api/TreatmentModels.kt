package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class TreatmentDto(
    val animalId: String,
    val disease: String,
    val treatmentName: String,
    val batchNumber: String,
    val volumeUsed: String,
    val cost: Double,
    val timestamp: Long,
    val deviceId: String,
    val recordguid: String,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)

@Serializable
data class TreatmentSyncRequest(
    val deviceId: String,
    val records: List<TreatmentDto>
)

@Serializable
data class TreatmentSyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class TreatmentSyncResponse(
    val results: List<TreatmentSyncResult>
)
