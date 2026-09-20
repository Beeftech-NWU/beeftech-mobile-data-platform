package com.beeftech.calfregistration.data

import com.beeftech.authentication.domain.TokenProvider
import com.beeftech.database.entity.CalfRegistration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Thin wrapper around a Ktor [HttpClient] that talks to the `backend:api`
 * calf-registration sync endpoint.
 *
 * Previously held its own hardcoded admin/admin123 login and token cache;
 * now reads the real session token from [tokenProvider] instead (backed by
 * android:authentication's SessionStore). This class no longer attempts a
 * login or a token refresh itself - on a 401 it simply fails (records stay
 * PENDING via CalfRegistrationRepository's existing retry bookkeeping),
 * since refresh tokens are explicitly out of scope for this build per the
 * login implementation plan.
 */
class CalfRegistrationApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val tokenProvider: TokenProvider,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun syncCalves(
        records: List<CalfRegistration>,
        deviceId: String
    ): Result<CalfRegistrationSyncResponse> {
        return try {
            val token = tokenProvider.token()
                ?: return Result.failure(IllegalStateException("Not logged in"))

            val dtoRecords = records.map { CalfRegistrationMappers.toDto(it) }

            val response = httpClient.post("${baseUrl}api/calf-registrations/sync") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(CalfRegistrationSyncRequest(deviceId = deviceId, records = dtoRecords))
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                return Result.failure(
                    IllegalStateException("Session expired - please log in again")
                )
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

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8081/"
    }
}