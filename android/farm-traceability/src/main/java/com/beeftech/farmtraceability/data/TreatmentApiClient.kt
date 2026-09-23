package com.beeftech.farmtraceability.data

import com.beeftech.database.entity.Treatment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

/*
 * Reference/master data returned by:
 *
 * GET /api/treatments/reference-data
 */
@Serializable
data class TreatmentReferenceDataDto(
    val diseases: List<String> = emptyList(),
    val treatmentTypes: List<String> = emptyList()
)

@Serializable
private data class TreatmentLoginRequest(
    val username: String,
    val password: String
)

@Serializable
private data class TreatmentLoginResponse(
    val token: String
)

@Serializable
private data class TreatmentApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

class TreatmentApiClient(
    private val baseUrl: String =
        DEFAULT_BASE_URL,

    private val demoUsername: String =
        DEFAULT_DEMO_USERNAME,

    private val demoPassword: String =
        DEFAULT_DEMO_PASSWORD
) {

    /*
     * Keep HttpClient private to the farm-traceability module.
     *
     * This prevents modules such as demoapp from needing Ktor's
     * HttpClient type on their own compile classpath simply to
     * construct TreatmentApiClient().
     */
    private val httpClient: HttpClient =
        HttpClient(OkHttp) {

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
        }

    @Volatile
    private var cachedToken: String? = null

    private val loginMutex =
        Mutex()

    private suspend fun login(): String {

        val response =
            httpClient.post(
                "${baseUrl}api/auth/login"
            ) {

                contentType(
                    ContentType.Application.Json
                )

                setBody(
                    TreatmentLoginRequest(
                        username = demoUsername,
                        password = demoPassword
                    )
                )
            }

        if (!response.status.isSuccess()) {

            throw IllegalStateException(
                "Login failed with status ${response.status}"
            )
        }

        val body:
                TreatmentApiResponse<TreatmentLoginResponse> =
            response.body()

        val token =
            body.data?.token
                ?: throw IllegalStateException(
                    "Login failed: ${body.message}"
                )

        cachedToken = token

        return token
    }

    private suspend fun ensureLoggedIn(): String {

        cachedToken?.let {
            return it
        }

        return loginMutex.withLock {

            cachedToken
                ?: login()
        }
    }

    private suspend fun forceRelogin(): String =
        loginMutex.withLock {

            login()
        }

    /*
     * Load Disease and Treatment Type reference data
     * from the backend database.
     */
    suspend fun getReferenceData():
            Result<TreatmentReferenceDataDto> {

        return try {

            var token =
                ensureLoggedIn()

            var response =
                getReferenceDataRequest(
                    token = token
                )

            /*
             * Token may have expired.
             * Log in again once and retry.
             */
            if (
                response.status ==
                HttpStatusCode.Unauthorized
            ) {

                token =
                    forceRelogin()

                response =
                    getReferenceDataRequest(
                        token = token
                    )
            }

            if (!response.status.isSuccess()) {

                return Result.failure(
                    IllegalStateException(
                        "Treatment reference-data request failed with status ${response.status}"
                    )
                )
            }

            val body:
                    TreatmentApiResponse<TreatmentReferenceDataDto> =
                response.body()

            val data =
                body.data
                    ?: return Result.failure(
                        IllegalStateException(
                            "Unable to load treatment reference data: ${body.message}"
                        )
                    )

            Result.success(
                data
            )

        } catch (exception: Exception) {

            Result.failure(
                exception
            )
        }
    }

    private suspend fun getReferenceDataRequest(
        token: String
    ): HttpResponse {

        return httpClient.get(
            "${baseUrl}api/treatments/reference-data"
        ) {

            header(
                "Authorization",
                "Bearer $token"
            )
        }
    }

    /*
     * Synchronize locally pending Treatment records.
     */
    suspend fun syncTreatments(
        records: List<Treatment>,
        deviceId: String
    ): Result<TreatmentSyncResponse> {

        return try {

            if (records.isEmpty()) {

                return Result.success(
                    TreatmentSyncResponse(
                        results = emptyList()
                    )
                )
            }

            val dtoRecords =
                records.map { treatment ->

                    TreatmentDto(
                        animalId =
                            treatment.animalId,

                        disease =
                            treatment.disease,

                        treatmentName =
                            treatment.treatmentName,

                        batchNumber =
                            treatment.batchNumber,

                        volumeUsed =
                            treatment.volumeUsed,

                        cost =
                            treatment.cost,

                        timestamp =
                            treatment.timestamp,

                        deviceId =
                            treatment.deviceId,

                        recordguid =
                            treatment.recordguid,

                        syncStatus =
                            treatment.syncStatus,

                        syncedAt =
                            treatment.syncedAt
                    )
                }

            var token =
                ensureLoggedIn()

            var response =
                postSync(
                    records = dtoRecords,
                    deviceId = deviceId,
                    token = token
                )

            /*
             * Authentication token may have expired.
             * Re-authenticate once and retry.
             */
            if (
                response.status ==
                HttpStatusCode.Unauthorized
            ) {

                token =
                    forceRelogin()

                response =
                    postSync(
                        records = dtoRecords,
                        deviceId = deviceId,
                        token = token
                    )
            }

            if (!response.status.isSuccess()) {

                return Result.failure(
                    IllegalStateException(
                        "Treatment sync failed with status ${response.status}"
                    )
                )
            }

            val body:
                    TreatmentApiResponse<TreatmentSyncResponse> =
                response.body()

            val data =
                body.data
                    ?: return Result.failure(
                        IllegalStateException(
                            "Treatment sync failed: ${body.message}"
                        )
                    )

            Result.success(
                data
            )

        } catch (exception: Exception) {

            Result.failure(
                exception
            )
        }
    }

    private suspend fun postSync(
        records: List<TreatmentDto>,
        deviceId: String,
        token: String
    ): HttpResponse {

        return httpClient.post(
            "${baseUrl}api/treatments/sync"
        ) {

            contentType(
                ContentType.Application.Json
            )

            header(
                "Authorization",
                "Bearer $token"
            )

            setBody(
                TreatmentSyncRequest(
                    deviceId = deviceId,
                    records = records
                )
            )
        }
    }

    companion object {

        const val DEFAULT_BASE_URL =
            "http://10.0.2.2:8081/"

        const val DEFAULT_DEMO_USERNAME =
            "admin"

        const val DEFAULT_DEMO_PASSWORD =
            "admin123"
    }
}