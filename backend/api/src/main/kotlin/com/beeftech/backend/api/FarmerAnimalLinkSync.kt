package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.SitesTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** An immutable assignment event, including the closure of an earlier assignment. */
object FarmerAnimalLinksTable : Table("farmer_animal_links") {
    val recordGuid = varchar("record_guid", 64)
    val linkId = varchar("link_id", 64)
    val farmerId = varchar("farmer_id", 255)
    val animalId = varchar("animal_id", 64)
    val effectiveFrom = long("effective_from")
    val effectiveTo = long("effective_to").nullable()
    val submittedByUserId = varchar("submitted_by_user_id", 64)
    val siteId = varchar("site_id", 64).nullable()
    override val primaryKey = PrimaryKey(recordGuid)
}

@Serializable
data class FarmerAnimalLinkUpload(
    val recordGuid: String,
    val linkId: String,
    val farmerId: String,
    val animalId: String,
    val effectiveFrom: Long,
    val effectiveTo: Long? = null
)

@Serializable
data class FarmerAnimalLinkAcknowledgement(
    val recordGuid: String,
    val stored: Boolean,
    val localHistory: Boolean = false
)

@Serializable
data class PendingAssignmentDiagnosisRequest(val links: List<FarmerAnimalLinkUpload>)

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

/**
 * Upload individual versioned link records, not just the current owner. Replays are safe.
 * The Android client must await an acknowledgement before marking the local row SYNCED.
 */
fun Route.farmerAnimalLinkRoutes(
    jwtService: JwtService,
    notificationService: FarmerSalesNotificationService? = null
) {
    missingParentRestoreRoutes(jwtService)
    post("/api/farmer-animal-links/diagnose") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@post
        val request = try { call.receive<PendingAssignmentDiagnosisRequest>() } catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(false, "Invalid diagnostic request"))
            return@post
        }
        if (request.links.size > 50 || request.links.any {
                it.recordGuid.isBlank() || it.recordGuid.length > 64 ||
                    it.farmerId.isBlank() || it.farmerId.length > 255 ||
                    it.animalId.isBlank() || it.animalId.length > 64
            }) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(false, "Invalid diagnostic references"))
            return@post
        }
        val admin = principal.roleEnum == Role.ADMIN
        val report = transaction {
            val scope = principal.recordScope()
            request.links.map { link ->
                // For non-admins the existence and location of other sites' records
                // must never be disclosed. Only inspect records visible to this account.
                val farmer = if (admin) {
                    val row = FarmerTable.selectAll().where { FarmerTable.farmerId eq link.farmerId }.singleOrNull()
                    when {
                        row == null -> "Missing on server"
                        row[FarmerTable.voidedAt] != null -> "Voided"
                        else -> "Active; site: ${row[FarmerTable.siteId] ?: "unassigned"}"
                    }
                } else {
                    val visible = FarmerTable.selectAll().where {
                        (FarmerTable.farmerId eq link.farmerId) and
                            scope.predicate(FarmerTable.submittedByUserId, FarmerTable.siteId, FarmerTable.voidedAt)
                    }.any()
                    if (visible) "Accessible" else "Missing or inaccessible"
                }
                val calf = if (admin) {
                    val row = CalfRegistrationTable.selectAll().where {
                        CalfRegistrationTable.animalUuid eq link.animalId
                    }.singleOrNull()
                    when {
                        row == null -> "Missing on server"
                        row[CalfRegistrationTable.voidedAt] != null -> "Voided"
                        else -> "Active; site: ${row[CalfRegistrationTable.siteId] ?: "unassigned"}"
                    }
                } else {
                    val visible = CalfRegistrationTable.selectAll().where {
                        (CalfRegistrationTable.animalUuid eq link.animalId) and
                            scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt)
                    }.any()
                    if (visible) "Accessible" else "Missing or inaccessible"
                }
                val issue = when {
                    admin && (farmer.startsWith("Missing") || calf.startsWith("Missing")) ->
                        "Registration missing on server; verify original capture and sync. Do not recreate blindly."
                    admin && (farmer == "Voided" || calf == "Voided") ->
                        "A referenced registration is voided; review the original record."
                    admin && (farmer.endsWith("unassigned") || calf.endsWith("unassigned")) ->
                        "Verify the correct farm and assign its site using Admin Records Review."
                    admin && farmer.substringAfter("site: ", "") != calf.substringAfter("site: ", "") ->
                        "Farmer and calf are assigned to different sites; verify both registrations."
                    farmer == "Accessible" && calf == "Accessible" ->
                        "Both registrations are accessible. Check for a conflicting assignment or retry after server update."
                    !admin -> "Review registration and site access with an administrator."
                    else -> "Both registrations are active. Check for an assignment conflict or stale session."
                }
                PendingAssignmentDiagnosis(
                    link.recordGuid, link.farmerId, link.animalId, farmer, calf, issue,
                    canRestore = principal.roleEnum == Role.MANAGER && !principal.siteId.isNullOrBlank()
                )
            }
        }
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, message = "Assignment check completed", data = report))
    }

    post("/api/farmer-animal-links/sync") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@post
        val link = try { call.receive<FarmerAnimalLinkUpload>() } catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Invalid assignment payload"))
            return@post
        }
        if (listOf(link.recordGuid, link.linkId, link.farmerId, link.animalId).any { it.isBlank() } ||
            link.recordGuid.length > 64 || link.linkId.length > 64 || link.animalId.length > 64 ||
            link.farmerId.length > 255 || link.effectiveFrom <= 0 ||
            (link.effectiveTo != null && link.effectiveTo < link.effectiveFrom)) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Invalid assignment values"))
            return@post
        }
        val result = transaction {
            val scope = principal.recordScope()
            // First check for the original saved link. A legitimate transfer may be closing
            // a server link whose *former* farmer has since moved, been voided, or disappeared.
            val previous = FarmerAnimalLinksTable.selectAll().where {
                FarmerAnimalLinksTable.recordGuid eq link.recordGuid
            }.singleOrNull()
            if (previous != null) {
                // Authorize against the saved link's provenance/site, not the former farmer.
                val canEditSaved = when (principal.roleEnum) {
                    Role.ADMIN -> true
                    Role.MANAGER -> !principal.siteId.isNullOrBlank() &&
                        previous[FarmerAnimalLinksTable.siteId] == principal.siteId
                    else -> previous[FarmerAnimalLinksTable.submittedByUserId] == principal.userId
                }
                if (!canEditSaved) return@transaction "FORBIDDEN"
                val sameIdentity = previous[FarmerAnimalLinksTable.linkId] == link.linkId &&
                    previous[FarmerAnimalLinksTable.farmerId] == link.farmerId &&
                    previous[FarmerAnimalLinksTable.animalId] == link.animalId &&
                    previous[FarmerAnimalLinksTable.effectiveFrom] == link.effectiveFrom
                if (!sameIdentity) return@transaction "CONFLICT"
                val existingEnd = previous[FarmerAnimalLinksTable.effectiveTo]
                if (existingEnd == link.effectiveTo) return@transaction "OK"
                if (existingEnd == null && link.effectiveTo != null) {
                    FarmerAnimalLinksTable.update({ FarmerAnimalLinksTable.recordGuid eq link.recordGuid }) {
                        it[effectiveTo] = link.effectiveTo
                    }
                    return@transaction "OK"
                }
                // Never reopen a closed historical link or change its identity.
                return@transaction "CONFLICT"
            }

            val calfAccessible = CalfRegistrationTable.selectAll().where {
                (CalfRegistrationTable.animalUuid eq link.animalId) and
                    scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt)
            }.any()
            val farmerAccessible = FarmerTable.selectAll().where {
                (FarmerTable.farmerId eq link.farmerId) and
                    scope.predicate(FarmerTable.submittedByUserId, FarmerTable.siteId, FarmerTable.voidedAt)
            }.any()

            if (link.effectiveTo != null && (!farmerAccessible || !calfAccessible)) {
                // No saved link matched this record GUID. With an inaccessible/missing parent,
                // never manufacture historical ownership on the server. Keep the ended record
                // locally as explicitly unsent history (not SYNCED and not silently deleted).
                // A link already stored under this GUID was handled and authorized above.
                return@transaction "LOCAL_HISTORY"
            }
            if (!farmerAccessible || !calfAccessible) return@transaction "MISSING"

            // A never-uploaded ended link with both parents accessible is valid history:
            // store it as ended, without reopening any current owner.
            if (link.effectiveTo == null) {
                val conflicting = FarmerAnimalLinksTable.selectAll().where {
                    (FarmerAnimalLinksTable.animalId eq link.animalId) and
                        FarmerAnimalLinksTable.effectiveTo.isNull()
                }.any()
                if (conflicting) return@transaction "CONFLICT"
            }
            FarmerAnimalLinksTable.insert {
                it[recordGuid] = link.recordGuid
                it[linkId] = link.linkId
                it[farmerId] = link.farmerId
                it[animalId] = link.animalId
                it[effectiveFrom] = link.effectiveFrom
                it[effectiveTo] = link.effectiveTo
                it[submittedByUserId] = principal.userId
                it[siteId] = principal.siteId
            }
            if (link.effectiveTo == null) "CREATED" else "OK"
        }
        when (result) {
            "CREATED" -> {
                // The assignment transaction has committed. Send a single complete receipt for
                // this newly accepted, active assignment. Replays get OK and do not send twice.
                try {
                    val receipt = transaction {
                        val calf = CalfRegistrationTable.selectAll().where {
                            CalfRegistrationTable.animalUuid eq link.animalId
                        }.singleOrNull() ?: return@transaction null
                        val farmer = FarmerTable.selectAll().where {
                            FarmerTable.farmerId eq link.farmerId
                        }.singleOrNull() ?: return@transaction null
                        if (calf[CalfRegistrationTable.voidedAt] != null ||
                            farmer[FarmerTable.voidedAt] != null ||
                            calf[CalfRegistrationTable.siteId] != farmer[FarmerTable.siteId]) {
                            // Never send a cross-site or voided farmer's details in an email.
                            return@transaction null
                        }
                        val siteId = calf[CalfRegistrationTable.siteId]
                        val site = siteId?.let { id ->
                            SitesTable.selectAll().where { SitesTable.siteId eq id }.singleOrNull()
                        }
                        CalfRegistrationNotificationPayload(
                            eventType = "CALF_ASSIGNED_TO_FARMER",
                            recordGuid = calf[CalfRegistrationTable.recordguid],
                            animalUuid = calf[CalfRegistrationTable.animalUuid],
                            tagNumber = calf[CalfRegistrationTable.tagNumber],
                            breed = calf[CalfRegistrationTable.breed],
                            birthdate = calf[CalfRegistrationTable.birthdate],
                            captureAt = calf[CalfRegistrationTable.captureAt],
                            deviceId = calf[CalfRegistrationTable.deviceId],
                            siteId = siteId,
                            siteName = site?.get(SitesTable.name),
                            submittedByUserId = calf[CalfRegistrationTable.submittedByUserId],
                            serverSyncedAt = calf[CalfRegistrationTable.syncedAt] ?: System.currentTimeMillis(),
                            assignedSalesmanEmail = site?.get(SitesTable.salesRepEmail),
                            damTagNumber = calf[CalfRegistrationTable.damTagNumber],
                            sireTagNumber = calf[CalfRegistrationTable.sireTagNumber],
                            gpsLatitude = calf[CalfRegistrationTable.gpsLat],
                            gpsLongitude = calf[CalfRegistrationTable.gpsLng],
                            assignment = CalfFarmerAssignmentDetails(
                                assignmentRecordGuid = link.recordGuid,
                                farmerId = farmer[FarmerTable.farmerId],
                                farmerClientCode = farmer[FarmerTable.clientCode],
                                farmerOrganisationName = farmer[FarmerTable.organisationName],
                                effectiveFrom = link.effectiveFrom,
                                assignedAtServer = System.currentTimeMillis()
                            )
                        )
                    }
                    if (receipt == null) {
                        System.err.println("Calf assignment stored; JSON email skipped due to site/record mismatch")
                    } else {
                        // Fetch the complete saved registration, not just the summary fields.
                        val fullCalf = CalfRegistrationRepository().findByTagNumber(
                            receipt.tagNumber, RecordScope.All
                        )
                        if (fullCalf != null) {
                            notificationService?.notifyCalfRegistration(receipt.copy(calfDetails = fullCalf))
                        } else {
                            System.err.println("Calf assignment stored; JSON email skipped because calf details are unavailable")
                        }
                    }
                } catch (_: Exception) {
                    // Email errors must not turn a persisted assignment back into pending.
                    System.err.println("Calf assignment stored; JSON email delivery failed")
                }
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    data = FarmerAnimalLinkAcknowledgement(link.recordGuid, true),
                    message = "Assignment stored"
                ))
            }
            "OK" -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = FarmerAnimalLinkAcknowledgement(link.recordGuid, true), message = "Assignment stored"))
            "LOCAL_HISTORY" -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = FarmerAnimalLinkAcknowledgement(link.recordGuid, false, localHistory = true), message = "Ended assignment absent on this server; retain local history"))
            "MISSING" -> call.respond(HttpStatusCode.UnprocessableEntity, ApiResponse<String>(success = false, message = "Farmer or calf is not synchronized or not accessible"))
            "FORBIDDEN" -> call.respond(HttpStatusCode.Forbidden, ApiResponse<String>(success = false, message = "Not authorized to change this saved assignment"))
            else -> call.respond(HttpStatusCode.Conflict, ApiResponse<String>(success = false, message = "Assignment conflicts with a saved record"))
        }
    }
}

