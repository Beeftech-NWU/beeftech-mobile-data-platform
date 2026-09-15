package com.beeftech.calfregistration.data

import com.beeftech.database.entity.CalfRegistration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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
 * The [httpClient] is injectable so tests can supply a
 * `MockEngine`-based client instead of the real OkHttp engine.
 */
class CalfRegistrationApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    // Simple in-memory token cache. Not thread-safe-hardened - fine for
    // this demo scope (mirrors other demo-only shortcuts in this codebase,
    // e.g. MainActivity's hardcoded passphrase comment).
    private var cachedToken: String? = null

    private suspend fun login(): String {
        val response = httpClient.post("${baseUrl}api/auth/login") {
            contentType(ContentType.Application.Json)
            // TODO: demo-only credentials until a real login flow exists
            setBody(LoginRequest(username = "admin", password = "admin123"))
        }

        val body: ApiResponse<LoginResponse> = response.body()

        val token = body.data?.token
            ?: throw IllegalStateException("Login failed: ${body.message}")

        cachedToken = token
        return token
    }

    private suspend fun ensureLoggedIn(): String {
        return cachedToken ?: login()
    }

    /**
     * Uploads [records] to `POST /api/calf-registrations/sync`, logging in
     * first if there is no cached token yet, and re-logging in (once) if the
     * server reports the cached token as unauthorized.
     */
    suspend fun syncCalves(
        records: List<CalfRegistration>,
        deviceId: String
    ): Result<CalfRegistrationSyncResponse> {
        return try {
            val dtoRecords = records.map { CalfRegistrationMappers.toDto(it) }

            var token = ensureLoggedIn()
            var response = postSync(dtoRecords, deviceId, token)

            if (response.status == HttpStatusCode.Unauthorized) {
                token = login()
                response = postSync(dtoRecords, deviceId, token)
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
