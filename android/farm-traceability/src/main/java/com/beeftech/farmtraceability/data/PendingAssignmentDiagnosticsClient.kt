package com.beeftech.farmtraceability.data

import com.beeftech.database.BackendConfig
import com.beeftech.database.entity.FarmerAnimalLink
import com.beeftech.database.security.TokenProviderRegistry
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
data class PendingAssignmentDiagnosis(
    val recordGuid: String,
    val farmerId: String,
    val animalId: String,
    val farmerStatus: String,
    val calfStatus: String,
    val advice: String,
    val canRestore: Boolean = false
)

@Serializable
private data class DiagnosticUpload(
    val recordGuid: String,
    val linkId: String,
    val farmerId: String,
    val animalId: String,
    val effectiveFrom: Long,
    val effectiveTo: Long?
)

@Serializable
private data class DiagnosticRequest(val links: List<DiagnosticUpload>)

@Serializable
private data class DiagnosticEnvelope(
    val success: Boolean,
    val message: String,
    val data: List<PendingAssignmentDiagnosis>? = null
)

/** Checks the backend without syncing, changing or discarding offline assignments. */
object PendingAssignmentDiagnosticsClient {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = false
    }

    suspend fun check(links: List<FarmerAnimalLink>): List<PendingAssignmentDiagnosis> {
        if (links.isEmpty()) return emptyList()
        val token = TokenProviderRegistry.get()?.token()
            ?: error("Please sign in to check pending assignments")
        val request = DiagnosticRequest(links.take(50).map {
            DiagnosticUpload(it.recordGuid, it.linkId, it.farmerId, it.animalId, it.effectiveFrom, it.effectiveTo)
        })
        val response = client.post(BackendConfig.baseUrl.trimEnd('/') + "/api/farmer-animal-links/diagnose") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            error(if (response.status.value == 404) {
                "Diagnostic endpoint unavailable. Restart the updated backend server."
            } else {
                "Backend returned HTTP ${response.status.value}; check your login and backend version."
            })
        }
        val body = response.body<DiagnosticEnvelope>()
        check(body.success) { body.message }
        return body.data ?: error("Backend did not return an assignment report")
    }
}
