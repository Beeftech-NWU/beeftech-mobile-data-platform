package com.beeftech.backend.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FarmerSalesNotificationPayload(
    val farmerId: String,
    val clientCode: String? = null,
    val organisationName: String? = null,
    val emailAddress: String? = null,
    val vatNumber: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val addresses: List<FarmerAddressDto> = emptyList(),
    val roles: List<FarmerRoleDto> = emptyList(),
    val deviceId: String,
    val submittedByUserId: String,
    val submittedByUsername: String,
    val submittedByRole: Int? = null,
    val registrationStatus: String = "REGISTERED",
    val serverSyncedAt: Long
)

interface FarmerSalesNotificationService {

    fun notifyRegistration(
        payload: FarmerSalesNotificationPayload
    )
}

class LoggingFarmerSalesNotificationService :
    FarmerSalesNotificationService {

    private val json =
        Json {
            prettyPrint = true
            explicitNulls = false
            encodeDefaults = true
        }

    override fun notifyRegistration(
        payload: FarmerSalesNotificationPayload
    ) {

        val summaryJson =
            json.encodeToString(payload)

        /*
         * Development delivery adapter.
         *
         * This generates the actual structured JSON summary.
         * SMTP delivery will replace this adapter once the
         * assigned sales representative source is defined.
         */
        println(
            "Farmer registration sales notification prepared:`n" +
                summaryJson
        )
    }
}