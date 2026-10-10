package com.beeftech.farmtraceability.data

import com.beeftech.database.BackendConfig
import com.beeftech.database.dao.VerifiedCalfImport
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.database.security.reportUnauthorized
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ServerCalf(
    val tagNumber: String,
    val animalUuid: String? = null,
    val birthdate: Long,
    val breed: String,
    val damAnimalUuid: String? = null,
    val sireAnimalUuid: String? = null,
    val gpsLat: Double,
    val gpsLng: Double,
    val captureAt: Long,
    val deviceId: String,
    val recordguid: String,
    val syncedAt: Long? = null
) {
    fun verifiedImport() = VerifiedCalfImport(
        animalId = animalUuid, tagNumber = tagNumber, birthdate = birthdate,
        breed = breed, damAnimalId = damAnimalUuid, sireAnimalId = sireAnimalUuid,
        gpsLat = gpsLat, gpsLng = gpsLng, captureAt = captureAt,
        deviceId = deviceId, recordGuid = recordguid, syncedAt = syncedAt
    )
}

@Serializable
private data class ServerEnvelope<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

/** Explicit, authenticated read only; local adoption is done in a Room transaction. */
internal object SiteCalfDownloadClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(farmerId: String): List<VerifiedCalfImport> {
        val provider = TokenProviderRegistry.get()
            ?: error("Sign in before downloading farm calves")
        val token = provider.token() ?: error("Your session is unavailable; sign in again")
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
        }
        try {
            val response = client.get("${BackendConfig.baseUrl}api/calf-registrations/assignment-candidates") {
                bearerAuth(token)
                parameter("farmerId", farmerId)
            }
            if (response.status == HttpStatusCode.Unauthorized) {
                provider.reportUnauthorized(null)
                error("Your session expired. Sign in again.")
            }
            if (!response.status.isSuccess()) {
                error("Calves could not be downloaded (${response.status}). Check the farmer's GauFarm site assignment and your manager login.")
            }
            val body: ServerEnvelope<List<ServerCalf>> = response.body()
            if (!body.success) error(body.message)
            return body.data.orEmpty().map { it.verifiedImport() }
        } finally {
            client.close()
        }
    }
}
