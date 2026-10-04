package com.beeftech.management.data

import com.beeftech.database.security.TokenProvider
import com.beeftech.database.security.reportUnauthorized
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

class ManagementApiClient(
    private val tokenProvider: TokenProvider,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(JSON)
        }
    }
) {

    /* A manager always gets their own site; an admin may pass siteId to narrow to one. */
    suspend fun dashboardSummary(siteId: String? = null): ManagementResult<DashboardSummary> =
        call(
            decode = {
                JSON.decodeFromString<Envelope<DashboardSummary>>(it).data
                    ?: error("Missing summary in response")
            }
        ) { token ->
            httpClient.get("${baseUrl}api/dashboard/summary") {
                bearerAuth(token)
                if (siteId != null) parameter("siteId", siteId)
            }
        }

    suspend fun listUsers(siteId: String? = null): ManagementResult<List<TeamMember>> =
        call(
            decode = { JSON.decodeFromString<Envelope<List<TeamMember>>>(it).data.orEmpty() }
        ) { token ->
            httpClient.get("${baseUrl}api/users") {
                bearerAuth(token)
                if (siteId != null) parameter("siteId", siteId)
            }
        }

    suspend fun createUser(body: CreateUserBody): ManagementResult<TeamMember> =
        call(decode = { decodeMember(it) }) { token ->
            httpClient.post("${baseUrl}api/users") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    suspend fun updateUser(userId: String, body: UpdateUserBody): ManagementResult<TeamMember> =
        call(decode = { decodeMember(it) }) { token ->
            httpClient.patch("${baseUrl}api/users/$userId") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    /* Leave pin null to have the server generate one; it comes back once in the result. */
    suspend fun resetPin(userId: String, pin: String? = null): ManagementResult<ResetPinResult> =
        call(
            decode = {
                JSON.decodeFromString<Envelope<ResetPinResult>>(it).data
                    ?: error("Missing PIN in response")
            }
        ) { token ->
            httpClient.post("${baseUrl}api/users/$userId/reset-pin") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(ResetPinBody(pin))
            }
        }

    suspend fun unbindDevice(userId: String): ManagementResult<TeamMember> =
        call(decode = { decodeMember(it) }) { token ->
            httpClient.post("${baseUrl}api/users/$userId/unbind-device") {
                bearerAuth(token)
            }
        }

    /* Voided records are included by default so a manager can see what was voided and why. */
    suspend fun reviewRecords(
        type: String,
        includeVoided: Boolean = true
    ): ManagementResult<List<ReviewRecord>> =
        call(
            decode = { JSON.decodeFromString<Envelope<List<ReviewRecord>>>(it).data.orEmpty() }
        ) { token ->
            httpClient.get("${baseUrl}api/records/$type") {
                bearerAuth(token)
                parameter("includeVoided", includeVoided)
            }
        }

    /* Corrections are void-only: a worker re-captures the record. The reason is required. */
    suspend fun voidRecord(type: String, id: String, reason: String): ManagementResult<VoidResult> =
        call(
            decode = {
                JSON.decodeFromString<Envelope<VoidResult>>(it).data
                    ?: error("Missing result in response")
            }
        ) { token ->
            httpClient.post("${baseUrl}api/records/$type/$id/void") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(VoidBody(reason))
            }
        }

    /* An admin gets every phone; a manager only their own site's. [status] is ACTIVE or REVOKED. */
    suspend fun devices(status: String? = null): ManagementResult<List<Device>> =
        call(
            decode = { JSON.decodeFromString<Envelope<List<Device>>>(it).data.orEmpty() }
        ) { token ->
            httpClient.get("${baseUrl}api/devices") {
                bearerAuth(token)
                if (status != null) parameter("status", status)
            }
        }

    /* Admin only. A blocked phone can't sign in or sync until it is reinstated. */
    suspend fun revokeDevice(deviceId: String, reason: String): ManagementResult<Device> =
        call(decode = { decodeDevice(it) }) { token ->
            httpClient.post("${baseUrl}api/devices/$deviceId/revoke") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(DeviceReasonBody(reason))
            }
        }

    suspend fun reinstateDevice(deviceId: String, reason: String): ManagementResult<Device> =
        call(decode = { decodeDevice(it) }) { token ->
            httpClient.post("${baseUrl}api/devices/$deviceId/reinstate") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(DeviceReasonBody(reason))
            }
        }

    /* Admin only. Newest first; pass the id of the last event you have as [before] for the next page. */
    suspend fun loginEvents(
        outcome: String? = null,
        before: Long? = null,
        limit: Int = AUDIT_PAGE_SIZE
    ): ManagementResult<List<LoginEvent>> =
        call(
            decode = { JSON.decodeFromString<Envelope<List<LoginEvent>>>(it).data.orEmpty() }
        ) { token ->
            httpClient.get("${baseUrl}api/login-events") {
                bearerAuth(token)
                if (outcome != null) parameter("outcome", outcome)
                if (before != null) parameter("before", before)
                parameter("limit", limit)
            }
        }

    /* Admin only: who is locked out of signing in right now. */
    suspend fun lockouts(): ManagementResult<List<Lockout>> =
        call(
            decode = { JSON.decodeFromString<Envelope<List<Lockout>>>(it).data.orEmpty() }
        ) { token ->
            httpClient.get("${baseUrl}api/login-security/lockouts") { bearerAuth(token) }
        }

    /* Lifts a sign-in lockout without changing the PIN. Admin, or a manager for workers on their site. */
    suspend fun unlockLogin(userId: String): ManagementResult<TeamMember> =
        call(decode = { decodeMember(it) }) { token ->
            httpClient.post("${baseUrl}api/users/$userId/unlock-login") { bearerAuth(token) }
        }

    /* An admin gets every site; a manager gets only their own. */
    suspend fun listSites(): ManagementResult<List<Site>> =
        call(
            decode = { JSON.decodeFromString<Envelope<List<Site>>>(it).data.orEmpty() }
        ) { token ->
            httpClient.get("${baseUrl}api/sites") { bearerAuth(token) }
        }

    suspend fun createSite(name: String): ManagementResult<Site> =
        call(decode = { decodeSite(it) }) { token ->
            httpClient.post("${baseUrl}api/sites") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(CreateSiteBody(name))
            }
        }

    /* Leave a field null to keep it. Deactivating a site that still has active users is a 409. */
    suspend fun updateSite(siteId: String, name: String? = null, active: Boolean? = null): ManagementResult<Site> =
        call(decode = { decodeSite(it) }) { token ->
            httpClient.patch("${baseUrl}api/sites/$siteId") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(UpdateSiteBody(name, active))
            }
        }

    /*
     * Newest first. Pass the id of the last entry you have as [before] to get the next page.
     * [from] is a time in epoch milliseconds.
     */
    suspend fun auditLog(
        action: String? = null,
        from: Long? = null,
        before: Long? = null,
        limit: Int = AUDIT_PAGE_SIZE
    ): ManagementResult<List<AuditLogEntry>> =
        call(
            decode = { JSON.decodeFromString<Envelope<List<AuditLogEntry>>>(it).data.orEmpty() }
        ) { token ->
            httpClient.get("${baseUrl}api/audit-log") {
                bearerAuth(token)
                if (action != null) parameter("action", action)
                if (from != null) parameter("from", from)
                if (before != null) parameter("before", before)
                parameter("limit", limit)
            }
        }

    private fun decodeDevice(body: String): Device =
        JSON.decodeFromString<Envelope<Device>>(body).data
            ?: error("Missing device in response")

    private fun decodeSite(body: String): Site =
        JSON.decodeFromString<Envelope<Site>>(body).data
            ?: error("Missing site in response")

    private fun decodeMember(body: String): TeamMember =
        JSON.decodeFromString<Envelope<TeamMember>>(body).data
            ?: error("Missing user in response")

    private suspend fun <T> call(
        decode: (String) -> T,
        request: suspend (token: String) -> HttpResponse
    ): ManagementResult<T> {
        val token = tokenProvider.token() ?: return ManagementResult.Unauthorized

        return try {
            val response = request(token)
            val body = response.bodyAsText()

            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created ->
                    ManagementResult.Success(decode(body))
                HttpStatusCode.Unauthorized -> {
                    tokenProvider.reportUnauthorized(body)
                    ManagementResult.Unauthorized
                }
                HttpStatusCode.Forbidden -> ManagementResult.Forbidden(messageOf(body, "Forbidden"))
                HttpStatusCode.NotFound -> ManagementResult.NotFound
                HttpStatusCode.BadRequest, HttpStatusCode.Conflict ->
                    ManagementResult.Rejected(messageOf(body, "Request rejected"))
                else -> ManagementResult.Error("Server error: ${response.status.value}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is IOException || e is UnresolvedAddressException || e.cause is IOException) {
                ManagementResult.NoConnection
            } else {
                ManagementResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun messageOf(body: String, fallback: String): String =
        try {
            JSON.decodeFromString<Envelope<String>>(body).message.ifBlank { fallback }
        } catch (e: Exception) {
            fallback
        }

    companion object {
        const val AUDIT_PAGE_SIZE = 50

        const val DEFAULT_BASE_URL = "https://beeftech-backend.onrender.com/"

        private val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    }
}
