package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.SitesTable
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Explicit recovery of an original offline registration, never a re-registration or upsert. */
@Serializable
data class RestoreMissingParentRequest(
    val link: FarmerAnimalLinkUpload,
    val kind: String,
    val verificationReason: String,
    val farmer: FarmerDto? = null,
    val calf: CalfRegistrationDto? = null
)

@Serializable
data class RestoreMissingParentOutcome(val status: String, val message: String)

fun Route.missingParentRestoreRoutes(jwtService: JwtService) {
    post("/api/farmer-animal-links/restore-missing-parent") {
        val actor = call.requireAuthPrincipal(jwtService) ?: return@post
        // An admin has no site of their own. Restoration creates a registration *in the
        // manager's verified site*. Admins must use the existing site review for legacy data.
        val site = actor.siteId
        if (actor.roleEnum != Role.MANAGER || site.isNullOrBlank()) {
            call.respond(HttpStatusCode.Forbidden,
                ApiResponse<String>(success = false, message = "Sign in as the manager of the verified farm to restore missing registrations"))
            return@post
        }
        val request = try { call.receive<RestoreMissingParentRequest>() } catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Invalid recovery request"))
            return@post
        }
        val link = request.link
        val reason = request.verificationReason.trim()
        if (reason.length !in 15..500 || link.recordGuid.isBlank() || link.recordGuid.length > 64 ||
            link.farmerId.isBlank() || link.animalId.isBlank() || link.animalId.length > 64 ||
            link.farmerId.length > 255 || link.effectiveFrom <= 0 ||
            (request.kind != "FARMER" && request.kind != "CALF") ||
            (request.kind == "FARMER" && (request.farmer == null || request.calf != null || request.farmer.farmerId != link.farmerId)) ||
            (request.kind == "CALF" && (request.calf == null || request.farmer != null || request.calf.animalUuid != link.animalId))) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Original record IDs and a verification reason are required"))
            return@post
        }
        val outcome = try {
            transaction(DatabaseFactory.getDatabase()) {
                val activeSite = SitesTable.selectAll().where {
                    (SitesTable.siteId eq site) and (SitesTable.active eq true)
                }.any()
                if (!activeSite) return@transaction RestoreMissingParentOutcome("DENIED", "Assigned farm site is not active")
                val now = System.currentTimeMillis()
                if (request.kind == "FARMER") {
                    val original = request.farmer!!
                    val found = FarmerTable.selectAll().where { FarmerTable.farmerId eq original.farmerId }.singleOrNull()
                    if (found != null) return@transaction RestoreMissingParentOutcome("EXISTS", "Farmer already exists on the server; no data or site was changed")
                    // A different UUID with the same client code requires human reconciliation.
                    if (!original.clientCode.isNullOrBlank() && FarmerTable.selectAll().where {
                            FarmerTable.clientCode eq original.clientCode
                        }.any()) {
                        return@transaction RestoreMissingParentOutcome("CONFLICT", "Another farmer has this client code. Review before restoring")
                    }
                    FarmerRepository().save(original, now, actor.userId, site)
                    insertAuditRow(AuditEntry(
                        action = "REGISTRATION_RESTORE_MISSING", entityType = "FARMER",
                        entityId = original.farmerId, actorUserId = actor.userId,
                        actorUsername = actor.username, actorRole = Role.MANAGER.id,
                        siteId = site, reason = reason,
                        details = auditDetails("pendingLinkGuid" to link.recordGuid, "restoration" to "create-only")
                    ), now)
                    RestoreMissingParentOutcome("RESTORED", "Original farmer restored to your farm site")
                } else {
                    val original = request.calf!!
                    if (original.recordguid.isBlank() || original.tagNumber.isBlank() || original.breed.isBlank() ||
                        original.deviceId.isBlank() || original.captureAt <= 0 || original.birthdate <= 0 ||
                        original.gpsLat !in -90.0..90.0 || original.gpsLng !in -180.0..180.0) {
                        return@transaction RestoreMissingParentOutcome("INVALID", "Local calf record is incomplete; review its original capture")
                    }
                    val existsById = CalfRegistrationTable.selectAll().where { CalfRegistrationTable.animalUuid eq link.animalId }.any()
                    if (existsById) return@transaction RestoreMissingParentOutcome("EXISTS", "Calf already exists on the server; no data or site was changed")
                    val collision = CalfRegistrationTable.selectAll().where {
                        (CalfRegistrationTable.recordguid eq original.recordguid) or
                            (CalfRegistrationTable.tagNumber eq original.tagNumber)
                    }.any()
                    if (collision) return@transaction RestoreMissingParentOutcome("CONFLICT", "Calf tag or record GUID already exists; review before restoring")
                    CalfRegistrationTable.insert {
                        it[tagNumber] = original.tagNumber
                        it[animalUuid] = original.animalUuid
                        it[birthdate] = original.birthdate
                        it[breed] = original.breed
                        it[damTagNumber] = original.damTagNumber
                        it[sireTagNumber] = original.sireTagNumber
                        it[damAnimalUuid] = original.damAnimalUuid
                        it[sireAnimalUuid] = original.sireAnimalUuid
                        it[photoPath] = original.photoPath
                        it[videoPath] = original.videoPath
                        it[gpsLat] = original.gpsLat
                        it[gpsLng] = original.gpsLng
                        it[captureAt] = original.captureAt
                        it[deviceId] = original.deviceId
                        it[recordguid] = original.recordguid
                        it[syncStatus] = "SYNCED"
                        it[syncedAt] = now
                        it[submittedByUserId] = actor.userId
                        it[siteId] = site
                    }
                    insertAuditRow(AuditEntry(
                        action = "REGISTRATION_RESTORE_MISSING", entityType = "CALF",
                        entityId = link.animalId, actorUserId = actor.userId,
                        actorUsername = actor.username, actorRole = Role.MANAGER.id,
                        siteId = site, reason = reason,
                        details = auditDetails("pendingLinkGuid" to link.recordGuid, "originalRecordGuid" to original.recordguid)
                    ), now)
                    RestoreMissingParentOutcome("RESTORED", "Original calf restored to your farm site")
                }
            }
        } catch (_: Exception) {
            RestoreMissingParentOutcome("ERROR", "Recovery failed safely; review server logs before retrying")
        }
        val status = if (outcome.status == "RESTORED") HttpStatusCode.OK else HttpStatusCode.Conflict
        call.respond(status, ApiResponse(success = outcome.status == "RESTORED", message = outcome.message, data = outcome))
    }
}
