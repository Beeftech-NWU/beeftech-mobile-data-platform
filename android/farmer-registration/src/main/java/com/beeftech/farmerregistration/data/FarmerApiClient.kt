package com.beeftech.farmerregistration.data

import android.content.Context
import android.provider.Settings
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerRoleEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
private data class LoginData(
    val token: String
)

@Serializable
private data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData? = null
)

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
    private val baseUrl: String = "http://10.0.2.2:8081"
) {

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private val client =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
        }

    private var cachedToken: String? = null

    private suspend fun login(): String? {

        val response =
            client.post(
                "$baseUrl/api/auth/login"
            ) {
                contentType(ContentType.Application.Json)

                setBody(
                    LoginRequest(
                        username = "admin",
                        password = "admin123"
                    )
                )
            }

        if (response.status != HttpStatusCode.OK) {
            return null
        }

        val body =
            response.body<LoginResponse>()

        val token =
            body.data?.token

        cachedToken = token

        return token
    }

    private suspend fun getToken(): String? {

        return cachedToken
            ?: login()
    }

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
    ): FarmerSyncResult? {

        val payload =
            FarmerPayload(
                farmerId =
                    farmer.farmer_id,

                clientCode =
                    farmer.client_code,

                organisationName =
                    farmer.organisation_name,

                vatNumber =
                    farmer.vat_number,

                emailAddress =
                    farmer.email_address,

                gpsLatitude =
                    farmer.gps_latitude,

                gpsLongitude =
                    farmer.gps_longitude,

                syncStatus =
                    farmer.sync_status,

                addresses =
                    addresses.map { address ->

                        FarmerAddressPayload(
                            addressId =
                                address.address_id,

                            farmerId =
                                address.farmer_id,

                            addressType =
                                address.address_type,

                            addressLine1 =
                                address.address_line_1,

                            province =
                                address.province,

                            postalCode =
                                address.postal_code,

                            gpsLatitude =
                                address.gps_latitude,

                            gpsLongitude =
                                address.gps_longitude
                        )
                    },

                roles =
                    roles.map { role ->

                        FarmerRolePayload(
                            farmerRoleId =
                                role.farmer_role_id,

                            farmerId =
                                role.farmer_id,

                            roleId =
                                role.role_id
                        )
                    }
            )

        val request =
            FarmerSyncRequest(
                deviceId = getDeviceId(),
                records = listOf(payload)
            )

        var token =
            getToken()
                ?: return null

        var response =
            client.post(
                "$baseUrl/api/farmers/sync"
            ) {

                bearerAuth(token)

                contentType(
                    ContentType.Application.Json
                )

                setBody(request)
            }

        /*
         * Token may have expired.
         * Clear it, authenticate once more and retry.
         */
        if (response.status == HttpStatusCode.Unauthorized) {

            cachedToken = null

            token =
                login()
                    ?: return null

            response =
                client.post(
                    "$baseUrl/api/farmers/sync"
                ) {

                    bearerAuth(token)

                    contentType(
                        ContentType.Application.Json
                    )

                    setBody(request)
                }
        }

        if (response.status != HttpStatusCode.OK) {
            return null
        }

        val body =
            response.body<
                    ApiResponse<FarmerSyncResponse>
                    >()

        if (!body.success) {
            return null
        }

        return body.data
            ?.results
            ?.firstOrNull {
                it.farmerId == farmer.farmer_id
            }
    }
}