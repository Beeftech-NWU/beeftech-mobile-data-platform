package com.beeftech.calfregistration.data

import com.beeftech.database.BackendConfig
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.security.TokenProvider
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.bodyAsText
import com.beeftech.database.security.reportUnauthorized
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
    private val baseUrl: String = BackendConfig.baseUrl,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun syncCalves(
        records: List<CalfRegistrationView>,
        deviceId: String
    ): Result<CalfRegistrationSyncResponse> {
        return try {
            val token = tokenProvider.token()
                ?: return Result.failure(
                    IllegalStateException("No authentication token available")
                )

            val dtoRecords = records.map { CalfRegistrationMappers.toDto(it) }

            val response = postSync(dtoRecords, deviceId, token)

            /* A 401 means sign in again; the session store decides what that does. Queued records are untouched. */
            if (response.status == HttpStatusCode.Unauthorized) {
                tokenProvider.reportUnauthorized(runCatching { response.bodyAsText() }.getOrNull())
            }

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

    /**
     * Uploads the calf's JPEG. 4xx answers other than 401/408/429 mean the server will never
     * take this file ([PhotoUploadResult.Rejected]); anything else is worth another try.
     */
    suspend fun uploadPhoto(tagNumber: String, jpeg: ByteArray): PhotoUploadResult {
        return try {
            val token = tokenProvider.token()
                ?: return PhotoUploadResult.RetryLater("No authentication token available")

            val response = httpClient.put("${baseUrl}api/calf-registrations/$tagNumber/photo") {
                contentType(ContentType.Image.JPEG)
                header("Authorization", "Bearer $token")
                setBody(jpeg)
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                tokenProvider.reportUnauthorized(runCatching { response.bodyAsText() }.getOrNull())
            }

            val code = response.status.value
            when {
                response.status.isSuccess() -> PhotoUploadResult.Uploaded
                code in 400..499 && code !in RETRYABLE_CLIENT_ERRORS ->
                    PhotoUploadResult.Rejected("Photo upload refused with status ${response.status}")
                else -> PhotoUploadResult.RetryLater("Photo upload failed with status ${response.status}")
            }
        } catch (exception: Exception) {
            PhotoUploadResult.RetryLater(exception.message ?: "Photo upload failed")
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

    private companion object {
        val RETRYABLE_CLIENT_ERRORS = setOf(401, 408, 429)
    }
}

sealed interface PhotoUploadResult {
    data object Uploaded : PhotoUploadResult

    /** The server refused this file for good; do not retry. */
    data class Rejected(val message: String) : PhotoUploadResult

    /** Offline, server trouble, or sign-in needed: try again later. */
    data class RetryLater(val message: String) : PhotoUploadResult
}
