package com.beeftech.feedcrib.data

import kotlinx.serialization.Serializable

@Serializable
data class CribSite(val id: String, val name: String)
@Serializable
data class CribPen(
    val id: String, val siteId: String, val description: String,
    val animalsBegin: Int? = null, val animalsIn: Int? = null,
    val animalsOut: Int? = null, val animalsClose: Int? = null,
    val requiredKg: Double? = null, val active: Boolean = true
)
@Serializable
data class CribRation(val id: String, val name: String, val active: Boolean = true)
@Serializable
data class CribSetup(
    val sites: List<CribSite> = emptyList(), val pens: List<CribPen> = emptyList(),
    val rations: List<CribRation> = emptyList(),
    val methods: List<String> = emptyList(),
    val adiRules: CribAdiRules = CribAdiRules()
)
@Serializable
data class CribAdiRules(
    val units: String? = null, val minimum: Double = 0.0,
    val maximum: Double? = null, val adjustmentStep: Double = 0.1,
    val temporary: Boolean = true
)
@Serializable
data class CribRegistration(
    val cribUuid: String, val cribNumber: String, val siteId: String,
    val penId: String, val rationId: String, val initialAdi: Double,
    val description: String = "", val method: String = "", val rfid: String? = null,
    val createdAt: Long, val deviceId: String,
    val gpsLat: Double? = null, val gpsLng: Double? = null
)
@Serializable
data class CribRegistrationRequest(val records: List<CribRegistration>)
@Serializable
data class CribRegistrationResult(
    val cribUuid: String, val status: String, val message: String? = null
)
@Serializable
data class CribRegistrationResponse(val results: List<CribRegistrationResult>)

