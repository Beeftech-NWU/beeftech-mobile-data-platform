package com.beeftech.farmtraceability.data

import com.beeftech.database.entity.Mortality
import com.beeftech.database.security.TokenProvider
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
data class MortalitySyncDto(
    val animalId: String,
    val causeOfDeath: String,
    val necropsyCodeId: String? = null,
    val responsibleWorker: String = "",
    val notes: String? = null,
    val timestamp: Long,
    val deviceId: String,
    val recordguid: String
)

@Serializable
data class MortalitySyncRequest(
    val deviceId: String,
    val records: List<MortalitySyncDto>
)

@Serializable
data class MortalitySyncResult(
    val recordguid: String,
    val animalId: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class MortalitySyncResponse(
    val results: List<MortalitySyncResult>
)

@Serializable
private data class MortalityApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

class MortalityApiClient(
    private val tokenProvider: TokenProvider,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun syncMortalities(
        records: List<Mortality>,
        deviceId: String
    ): Result<MortalitySyncResponse> {

        if (records.isEmpty()) {
            return Result.success(MortalitySyncResponse(results = emptyList()))
        }

        return try {

            val token = tokenProvider.token()
                ?: return Result.failure(
                    IllegalStateException("No authentication token available")
                )

            val response = httpClient.post("${baseUrl}api/mortalities/sync") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(
                    MortalitySyncRequest(
                        deviceId = deviceId,
                        records = records.map { it.toDto(deviceId) }
                    )
                )
            }

            if (!response.status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("Mortality sync failed with status ${response.status}")
                )
            }

            val body: MortalityApiResponse<MortalitySyncResponse> = response.body()

            val data = body.data
                ?: return Result.failure(
                    IllegalStateException("Mortality sync failed: ${body.message}")
                )

            Result.success(data)

        } catch (exception: Exception) {

            Result.failure(exception)
        }
    }

    private fun Mortality.toDto(deviceId: String) = MortalitySyncDto(
        animalId = animalId,
        causeOfDeath = causeOfDeath,
        necropsyCodeId = necropsyCodeId,
        responsibleWorker = responsibleWorker,
        notes = notes,
        timestamp = timestamp,
        deviceId = deviceId,
        recordguid = recordGuid
    )

    companion object {

        const val DEFAULT_BASE_URL =
            "https://beeftech-backend.onrender.com/"
    }
}
