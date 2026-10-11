package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class FarmerAddressDto(
    val addressId: String,
    val farmerId: String,
    val addressType: String? = null,
    val addressLine1: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null
)

@Serializable
data class FarmerRoleDto(
    val farmerRoleId: String,
    val farmerId: String,
    val roleId: String
)

@Serializable
data class FarmerDto(
    val farmerId: String,
    val clientCode: String? = null,
    val organisationName: String? = null,
    val vatNumber: String? = null,
    val emailAddress: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val syncStatus: String = "PENDING",
    val addresses: List<FarmerAddressDto> = emptyList(),
    val roles: List<FarmerRoleDto> = emptyList()
)

@Serializable
data class FarmerSyncRequest(
    val deviceId: String,
    val records: List<FarmerDto>
)

@Serializable
data class FarmerSyncResult(
    val farmerId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class FarmerSyncResponse(
    val results: List<FarmerSyncResult>
)