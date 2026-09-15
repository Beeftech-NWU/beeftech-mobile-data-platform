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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    // TODO: demo-only credentials until a real login flow/credential store
    // exists. Exposed as constructor parameters (rather than inlined in
    // [login]) so a real credential source can be plugged in later without
    // touching the login/sync logic itself.
    private val demoUsername: String = DEFAULT_DEMO_USERNAME,
    private val demoPassword: String = DEFAULT_DEMO_PASSWORD,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    // In-memory token cache, guarded by [loginMutex] so concurrent callers
    // (e.g. a manual "retry sync" tap racing with an in-flight save) can't
    // both decide the cache is empty and log in twice at once. @Volatile
    // ensures the fast, lock-free read path in [ensureLoggedIn] sees writes
    // made under the lock from any thread.
    @Volatile
    private var cachedToken: String? = null
    private val loginMutex = Mutex()

    private suspend fun login(): String {
        val response = httpClient.post("${baseUrl}api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = demoUsername, password = demoPassword))
        }

        // Check the status before attempting to parse the body: a failed
        // login may return a non-JSON error page (or a differently-shaped
        // body), which would otherwise surface as a confusing deserialization
        // exception instead of a clear "login failed" error.
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Login failed with status ${response.status}")
        }

        val body: ApiResponse<LoginResponse> = response.body()

        val token = body.data?.token
            ?: throw IllegalStateException("Login failed: ${body.message}")

        cachedToken = token
        return token
    }

    /** Logs in only if nothing else has already populated [cachedToken]. */
    private suspend fun ensureLoggedIn(): String {
        cachedToken?.let { return it }

        return loginMutex.withLock {
            // Double-checked: another coroutine may have logged in while we
            // were waiting for the lock.
            cachedToken ?: login()
        }
    }

    /** Always performs a fresh login (e.g. after the server rejects the cached token). */
    private suspend fun forceRelogin(): String = loginMutex.withLock { login() }

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
                token = forceRelogin()
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
        const val DEFAULT_DEMO_USERNAME = "admin"
        const val DEFAULT_DEMO_PASSWORD = "admin123"
    }
}
