package com.beeftech.farmtraceability.data

import com.beeftech.calfregistration.data.CalfRegistrationDto
import com.beeftech.calfregistration.data.CalfRegistrationMappers
import com.beeftech.database.BackendConfig
import com.beeftech.database.BeefTechDatabase
import com.beeftech.database.entity.FarmerAnimalLink
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.farmerregistration.data.FarmerAddressPayload
import com.beeftech.farmerregistration.data.FarmerPayload
import com.beeftech.farmerregistration.data.FarmerRolePayload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class OriginalAssignmentRef(
    val recordGuid: String, val linkId: String, val farmerId: String,
    val animalId: String, val effectiveFrom: Long, val effectiveTo: Long? = null
)

@Serializable
private data class RestoreRequest(
    val link: OriginalAssignmentRef,
    val kind: String,
    val verificationReason: String,
    val farmer: FarmerPayload? = null,
    val calf: CalfRegistrationDto? = null
)

@Serializable
private data class RestoreOutcome(val status: String, val message: String)

@Serializable
private data class RestoreEnvelope(
    val success: Boolean,
    val message: String,
    val data: RestoreOutcome? = null
)

/** Explicit, user-confirmed, create-if-missing repair. Never modifies local sync flags. */
object MissingParentRestoreClient {
    suspend fun restore(db: BeefTechDatabase, link: FarmerAnimalLink, kind: String, reason: String): String {
        require(kind == "FARMER" || kind == "CALF") { "Unknown registration type" }
        require(reason.trim().length in 15..500) { "Enter a verification reason (at least 15 characters)" }
        val token = TokenProviderRegistry.get()?.token() ?: error("Sign in as the GauFarm manager first")
        val farmerPayload = if (kind == "FARMER") {
            val farmer = db.farmerDao().getFarmerById(link.farmerId)
                ?: error("Original farmer was not found on this device. Nothing was changed.")
            FarmerPayload(
                farmerId = farmer.farmer_id,
                clientCode = farmer.client_code,
                organisationName = farmer.organisation_name,
                vatNumber = farmer.vat_number,
                emailAddress = farmer.email_address,
                gpsLatitude = farmer.gps_latitude,
                gpsLongitude = farmer.gps_longitude,
                syncStatus = farmer.sync_status,
                coRegIdNo = farmer.co_reg_id_no,
                landOwnership = farmer.land_ownership,
                faCodeRmis = farmer.fa_code_rmis,
                glnNumber = farmer.gln_number,
                addresses = db.farmerDao().getAddressesForFarmer(link.farmerId).map {
                    FarmerAddressPayload(
                        addressId = it.address_id, farmerId = it.farmer_id,
                        addressType = it.address_type, addressLine1 = it.address_line_1,
                        province = it.province, postalCode = it.postal_code,
                        gpsLatitude = it.gps_latitude, gpsLongitude = it.gps_longitude,
                        streetCode = it.street_code, postalAddress = it.postal_address,
                        country = it.country
                    )
                },
                roles = db.farmerDao().getRolesForFarmer(link.farmerId).map {
                    FarmerRolePayload(it.farmer_role_id, it.farmer_id, it.role_id.toString())
                }
            )
        } else null
        val calfPayload = if (kind == "CALF") {
            val original = db.calfRegistrationDao().getAllRegistrationViews().first()
                .singleOrNull { it.animalId == link.animalId }
                ?: error("Original calf registration is not on this device. Nothing was changed.")
            CalfRegistrationMappers.toDto(original)
        } else null
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = false
        }
        try {
            val response = client.post(BackendConfig.baseUrl.trimEnd('/') + "/api/farmer-animal-links/restore-missing-parent") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(RestoreRequest(
                    link = OriginalAssignmentRef(link.recordGuid, link.linkId, link.farmerId,
                        link.animalId, link.effectiveFrom, link.effectiveTo),
                    kind = kind, verificationReason = reason.trim(), farmer = farmerPayload, calf = calfPayload
                ))
            }
            val body = response.body<RestoreEnvelope>()
            if (!body.success || body.data?.status != "RESTORED") {
                error(body.data?.message ?: body.message)
            }
            return body.data.message
        } finally {
            client.close()
        }
    }
}
