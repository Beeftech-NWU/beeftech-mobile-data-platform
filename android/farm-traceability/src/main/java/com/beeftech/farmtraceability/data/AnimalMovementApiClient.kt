package com.beeftech.farmtraceability.data

import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.security.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AnimalMovementDto(
    val animalId: String,
    val movementType: String,
    val responsibleWorker: String,
    val timestamp: Long,
    val gpsLat: Double,
    val gpsLng: Double,
    val deviceId: String,
    val recordguid: String,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)

@Serializable
data class AnimalMovementSyncRequest(
    val deviceId: String,
    val records: List<AnimalMovementDto>
)

@Serializable
data class AnimalMovementSyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class AnimalMovementSyncResponse(
    val results: List<AnimalMovementSyncResult>
)

@Serializable
private data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

class AnimalMovementApiClient(
    private val tokenProvider: TokenProvider,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
) {

    suspend fun syncMovements(
        records: List<AnimalMovementEntity>,
        deviceId: String
    ): Result<AnimalMovementSyncResponse> {

        return try {

            if (records.isEmpty()) {

                return Result.success(
                    AnimalMovementSyncResponse(
                        results = emptyList()
                    )
                )
            }

            val token =
                tokenProvider.token()
                    ?: return Result.failure(
                        IllegalStateException(
                            "No authentication token available"
                        )
                    )

            val dtoRecords =
                records.map { movement ->

                    AnimalMovementDto(
                        animalId =
                            movement.animalId,

                        movementType =
                            movement.movementType,

                        responsibleWorker =
                            movement.responsibleWorker,

                        timestamp =
                            movement.timestamp,

                        gpsLat = 0.0,

                        gpsLng = 0.0,

                        deviceId = deviceId,

                        recordguid =
                            movement.movementId,

                        syncStatus = "PENDING",

                        syncedAt = null
                    )
                }

            val response =
                postSync(
                    records = dtoRecords,
                    deviceId = deviceId,
                    token = token
                )

            if (!response.status.isSuccess()) {

                return Result.failure(
                    IllegalStateException(
                        "Movement sync failed with status ${response.status}"
                    )
                )
            }

            val body:
                    ApiResponse<AnimalMovementSyncResponse> =
                response.body()

            val data =
                body.data
                    ?: return Result.failure(
                        IllegalStateException(
                            "Movement sync failed: ${body.message}"
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
        records: List<AnimalMovementDto>,
        deviceId: String,
        token: String
    ): HttpResponse {

        return httpClient.post(
            "${baseUrl}api/animal-movements/sync"
        ) {

            contentType(
                ContentType.Application.Json
            )

            header(
                "Authorization",
                "Bearer $token"
            )

            setBody(
                AnimalMovementSyncRequest(
                    deviceId = deviceId,
                    records = records
                )
            )
        }
    }

    companion object {

        const val DEFAULT_BASE_URL =
            "http://10.0.2.2:8081/"
    }
}
