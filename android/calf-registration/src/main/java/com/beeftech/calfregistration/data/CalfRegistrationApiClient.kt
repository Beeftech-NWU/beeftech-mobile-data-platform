package com.beeftech.calfregistration.data

import com.beeftech.database.entity.CalfRegistrationEntity
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
import kotlinx.serialization.json.Json

/**
 * Thin wrapper around a Ktor [HttpClient] that talks to the `backend:api`
 * calf-registration sync endpoint.
 */
class CalfRegistrationApiClient(
    private val tokenProvider: TokenProvider,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun syncCalves(
        records: List<CalfRegistrationEntity>,
        deviceId: String
    ): Result<CalfRegistrationSyncResponse> {
        return try {
            val token = tokenProvider.token()
                ?: return Result.failure(
                    IllegalStateException("No authentication token available")
                )

            val dtoRecords = records.map { CalfRegistrationMappers.toDto(it) }

            val response = postSync(dtoRecords, deviceId, token)

            if (!response.status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("Sync failed with status ${response.status}")
                )
            }

            val body: ApiResponse<CalfRegistrationSyncResponse> = response.body()

            val data = body.data
                ?: return Result.failure(IllegalStateException("Sync failed: ${body.message}"))

            Result.success(data)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun postSync(
        records: List<CalfRegistrationDto>,
        deviceId: String,
        token: String
    ): HttpResponse {
        return httpClient.post("${baseUrl}api/calf-registrations/sync") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CalfRegistrationSyncRequest(deviceId = deviceId, records = records))
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8081/"
    }
}
