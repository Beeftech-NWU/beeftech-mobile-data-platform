package com.beeftech.farmtraceability.data

import android.util.Log
import com.beeftech.database.BackendConfig
import com.beeftech.database.entity.FarmerAnimalLink
import com.beeftech.database.security.TokenProvider
import com.beeftech.database.security.reportUnauthorized
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class FarmerAnimalLinkPayload(
    val recordGuid: String,
    val linkId: String,
    val farmerId: String,
    val animalId: String,
    val effectiveFrom: Long,
    val effectiveTo: Long?
)

@Serializable
internal data class FarmerAnimalLinkAck(
    val recordGuid: String,
    val stored: Boolean,
    val localHistory: Boolean = false
)

internal enum class LinkUploadResult { STORED, LOCAL_HISTORY, DEFERRED }

@Serializable
internal data class FarmerAnimalLinkApiResponse(
    val success: Boolean,
    val message: String,
    val data: FarmerAnimalLinkAck? = null
)

/** Server acknowledgment, not HTTP success alone, is required before updating local status. */
internal class FarmerAnimalLinkApiClient(
    private val tokenProvider: TokenProvider,
    private val baseUrl: String = BackendConfig.baseUrl,
    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
) {
    suspend fun upload(link: FarmerAnimalLink): LinkUploadResult {
        val token = tokenProvider.token()
        if (token == null) {
            Log.w("FarmerAnimalLinkApi", "Upload deferred: authentication token unavailable")
            return LinkUploadResult.DEFERRED
        }
        val response = client.post(baseUrl.trimEnd('/') + "/api/farmer-animal-links/sync") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(FarmerAnimalLinkPayload(
                recordGuid = link.recordGuid,
                linkId = link.linkId,
                farmerId = link.farmerId,
                animalId = link.animalId,
                effectiveFrom = link.effectiveFrom,
                effectiveTo = link.effectiveTo
            ))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            Log.w("FarmerAnimalLinkApi", "Upload rejected: HTTP 401 (unauthorized)")
            tokenProvider.reportUnauthorized(null)
            return LinkUploadResult.DEFERRED
        }
        if (!response.status.isSuccess()) {
            val reason = when (response.status.value) {
                400 -> "invalid assignment payload"
                403 -> "access forbidden"
                404 -> "endpoint not found"
                409 -> "assignment conflict"
                422 -> "farmer or calf missing, or inaccessible"
                in 500..599 -> "backend server error"
                else -> "unexpected HTTP response"
            }
            Log.w("FarmerAnimalLinkApi", "Upload rejected: HTTP ${response.status.value} ($reason)")
            return LinkUploadResult.DEFERRED
        }
        val body = response.body<FarmerAnimalLinkApiResponse>()
        val ack = body.data
        if (!body.success || ack == null || ack.recordGuid != link.recordGuid) {
            Log.w("FarmerAnimalLinkApi", "HTTP 2xx received without a valid assignment acknowledgement")
            return LinkUploadResult.DEFERRED
        }
        return when {
            ack.stored && !ack.localHistory -> LinkUploadResult.STORED
            !ack.stored && ack.localHistory && link.effectiveTo != null -> {
                Log.i("FarmerAnimalLinkApi", "Ended assignment confirmed absent on this server; retaining local history")
                LinkUploadResult.LOCAL_HISTORY
            }
            else -> {
                Log.w("FarmerAnimalLinkApi", "Invalid or contradictory assignment acknowledgement")
                LinkUploadResult.DEFERRED
            }
        }
    }
}

