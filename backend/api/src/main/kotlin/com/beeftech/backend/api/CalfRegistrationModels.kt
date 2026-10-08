package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class CalfRegistrationDto(
    val tagNumber: String,
    val animalUuid: String? = null,
    val birthdate: Long,
    val breed: String,
    val gender: String? = null,
    val hideColour: String? = null,
    val brandMark: String? = null,
    val birthWeightKg: Double? = null,
    val ageClass: String? = null,
    val bodyCondition: String? = null,
    val conformity: String? = null,
    val oldTagNumber: String? = null,
    val referenceNumber: String? = null,
    val processProof: String? = null,
    val implantProof: String? = null,
    val damTagNumber: String? = null,
    val sireTagNumber: String? = null,
    val damAnimalUuid: String? = null,
    val sireAnimalUuid: String? = null,
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
    val tagNumber: String,
    val status: String, // "SYNCED" or "ERROR"
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class CalfRegistrationSyncResponse(
    val results: List<CalfRegistrationSyncResult>
)
