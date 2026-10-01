package com.beeftech.farmerregistration.data

import android.content.Context
import android.provider.Settings
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerRoleEntity
import com.beeftech.database.security.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class FarmerAddressPayload(
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
data class FarmerRolePayload(
    val farmerRoleId: String,
    val farmerId: String,
    val roleId: String
)

@Serializable
data class FarmerPayload(
    val farmerId: String,
    val clientCode: String? = null,
    val organisationName: String? = null,
    val vatNumber: String? = null,
    val emailAddress: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val syncStatus: String = "PENDING",
    val addresses: List<FarmerAddressPayload> = emptyList(),
    val roles: List<FarmerRolePayload> = emptyList()
)

@Serializable
data class FarmerSyncRequest(
    val deviceId: String,
    val records: List<FarmerPayload>
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

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

class FarmerApiClient(
    private val context: Context,
    private val tokenProvider: TokenProvider,
    private val client: HttpClient,
    private val baseUrl: String = "https://beeftech-backend.onrender.com"
) {

    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-device"
    }

    suspend fun syncFarmer(
        farmer: FarmerEntity,
        addresses: List<FarmerAddressEntity>,
        roles: List<FarmerRoleEntity>
    ): Result<FarmerSyncResult> = runCatching {
        val token = tokenProvider.token()
            ?: error("Authentication token unavailable.")

        val payload = farmer.toPayload(addresses, roles)
        val request = FarmerSyncRequest(
            deviceId = getDeviceId(),
            records = listOf(payload)
        )

        val response = client.post("$baseUrl/api/farmers/sync") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            error("HTTP error: ${response.status}")
        }

        val body = response.body<ApiResponse<FarmerSyncResponse>>()
        if (!body.success || body.data == null) {
            error(body.message.ifEmpty { "Sync failed on server" })
        }

        body.data.results.firstOrNull { it.farmerId == farmer.farmer_id }
            ?: error("No sync result returned for farmer ID ${farmer.farmer_id}")
    }

    private fun FarmerEntity.toPayload(
        addresses: List<FarmerAddressEntity>,
        roles: List<FarmerRoleEntity>
    ) = FarmerPayload(
        farmerId = farmer_id,
        clientCode = client_code,
        organisationName = organisation_name,
        vatNumber = vat_number,
        emailAddress = email_address,
        gpsLatitude = gps_latitude,
        gpsLongitude = gps_longitude,
        syncStatus = sync_status,
        addresses = addresses.map { it.toPayload() },
        roles = roles.map { it.toPayload() }
    )

    private fun FarmerAddressEntity.toPayload() = FarmerAddressPayload(
        addressId = address_id,
        farmerId = farmer_id,
        addressType = address_type,
        addressLine1 = address_line_1,
        province = province,
        postalCode = postal_code,
        gpsLatitude = gps_latitude,
        gpsLongitude = gps_longitude
    )

    private fun FarmerRoleEntity.toPayload() = FarmerRolePayload(
        farmerRoleId = farmer_role_id,
        farmerId = farmer_id,
        roleId = role_id.toString()
    )
}