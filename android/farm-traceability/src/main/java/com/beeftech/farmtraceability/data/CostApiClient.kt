package com.beeftech.farmtraceability.data

import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.security.TokenProvider
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.bodyAsText
import com.beeftech.database.security.reportUnauthorized
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CostSyncDto(
    val animalId: String,
    val costType: String,
    val amount: Double,
    val description: String = "",
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0,
    val timestamp: Long,
    val sourceEntity: String? = null,
    val sourceRecordId: String? = null,
    val deviceId: String,
    val recordguid: String
)

@Serializable
data class CostSyncRequest(
    val deviceId: String,
    val records: List<CostSyncDto>
)

@Serializable
data class CostSyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class CostSyncResponse(
    val results: List<CostSyncResult>
)

@Serializable
private data class CostApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

class CostApiClient(
    private val tokenProvider: TokenProvider,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun syncCosts(
        records: List<AnimalCost>,
        deviceId: String
    ): Result<CostSyncResponse> {

        if (records.isEmpty()) {
            return Result.success(CostSyncResponse(results = emptyList()))
        }

        return try {

            val token = tokenProvider.token()
                ?: return Result.failure(
                    IllegalStateException("No authentication token available")
                )

            val response = httpClient.post("${baseUrl}api/costs/sync") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(
                    CostSyncRequest(
                        deviceId = deviceId,
                        records = records.map { it.toDto(deviceId) }
                    )
                )
            }

            /* A 401 means sign in again; the session store decides what that does. Queued records are untouched. */
            if (response.status == HttpStatusCode.Unauthorized) {
                tokenProvider.reportUnauthorized(runCatching { response.bodyAsText() }.getOrNull())
            }

            if (!response.status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("Cost sync failed with status ${response.status}")
                )
            }

            val body: CostApiResponse<CostSyncResponse> = response.body()

            val data = body.data
                ?: return Result.failure(
                    IllegalStateException("Cost sync failed: ${body.message}")
                )

            Result.success(data)

        } catch (exception: Exception) {

            Result.failure(exception)
        }
    }

    private fun AnimalCost.toDto(deviceId: String) = CostSyncDto(
        animalId = animalId,
        costType = costType,
        amount = amount,
        description = description,
        gpsLat = gpsLat,
        gpsLng = gpsLng,
        timestamp = timestamp,
        sourceEntity = sourceEntity,
        sourceRecordId = sourceRecordId,
        deviceId = deviceId,
        recordguid = recordGuid
    )

    companion object {

        const val DEFAULT_BASE_URL =
            "https://beeftech-backend.onrender.com/"
    }
}
