package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UsersTable
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Resending is a deliberate administrative action, never a second sync or registration. */
@Serializable
data class RegistrationReceiptResendRequest(val reason: String)

private const val REQUEST_AUDIT_ACTION = "REG_RECEIPT_RESEND_REQUEST"
private const val RESULT_AUDIT_ACTION = "REG_RECEIPT_RESEND_RESULT"
private const val RESEND_COOLDOWN_MS = 120_000L

private data class ReceiptRecordInfo(
    val siteId: String?,
    val submittedByUserId: String?,
    val originalUsername: String?,
    val originalRole: Int?,
    val syncedAt: Long?
)

private fun originalSubmitterInfo(siteId: String?, userId: String?, syncedAt: Long?): ReceiptRecordInfo {
    val user = userId?.let { id ->
        UsersTable.selectAll().where { UsersTable.userId eq id }.singleOrNull()
    }
    return ReceiptRecordInfo(
        siteId = siteId,
        submittedByUserId = userId,
        originalUsername = user?.get(UsersTable.username),
        originalRole = user?.get(UsersTable.role),
        syncedAt = syncedAt
    )
}

private fun farmerReceiptInfo(farmerId: String): ReceiptRecordInfo? =
    transaction(DatabaseFactory.getDatabase()) {
        val row = FarmerTable.selectAll().where {
            FarmerTable.farmerId eq farmerId
        }.singleOrNull() ?: return@transaction null
        if (row[FarmerTable.voidedAt] != null || row[FarmerTable.syncStatus] != "SYNCED") {
            return@transaction null
        }
        originalSubmitterInfo(
            row[FarmerTable.siteId],
            row[FarmerTable.submittedByUserId],
            row[FarmerTable.syncedAt]
        )
    }

private fun calfReceiptInfo(tagNumber: String): ReceiptRecordInfo? =
    transaction(DatabaseFactory.getDatabase()) {
        val row = CalfRegistrationTable.selectAll().where {
            CalfRegistrationTable.tagNumber eq tagNumber
        }.singleOrNull() ?: return@transaction null
        if (row[CalfRegistrationTable.voidedAt] != null || row[CalfRegistrationTable.syncStatus] != "SYNCED") {
            return@transaction null
        }
        originalSubmitterInfo(
            row[CalfRegistrationTable.siteId],
            row[CalfRegistrationTable.submittedByUserId],
            row[CalfRegistrationTable.syncedAt]
        )
    }

/**
 * Reserve one attempt in the existing audit table. A short database-backed cooldown guards
 * against duplicate taps and continues to work after Render restarts. Never send inside a DB
 * transaction: SMTP is a slow external operation.
 */
private fun reserveReceiptResend(
    principal: com.beeftech.backend.api.auth.AuthPrincipal,
    kind: String,
    id: String,
    siteId: String?,
    reason: String,
    now: Long
): Boolean = transaction(DatabaseFactory.getDatabase()) {
    val recent = AuditLogTable.selectAll().where {
        (AuditLogTable.action eq REQUEST_AUDIT_ACTION) and
            (AuditLogTable.entityType eq kind) and
            (AuditLogTable.entityId eq id) and
            (AuditLogTable.createdAt greaterEq (now - RESEND_COOLDOWN_MS))
    }.any()
    if (recent) return@transaction false

    insertAuditRow(
        AuditEntry(
            action = REQUEST_AUDIT_ACTION,
            entityType = kind,
            entityId = id,
            reason = reason,
            actorUserId = principal.userId,
            actorUsername = principal.username,
            actorRole = principal.role ?: Role.ADMIN.id,
            siteId = siteId
        ), now
    )
    true
}

private fun auditReceiptResendResult(
    principal: com.beeftech.backend.api.auth.AuthPrincipal,
    kind: String,
    id: String,
    siteId: String?,
    status: String
) = transaction(DatabaseFactory.getDatabase()) {
    insertAuditRow(
        AuditEntry(
            action = RESULT_AUDIT_ACTION,
            entityType = kind,
            entityId = id,
            actorUserId = principal.userId,
            actorUsername = principal.username,
            actorRole = principal.role ?: Role.ADMIN.id,
            siteId = siteId,
            details = auditDetails("result" to status)
        )
    )
}

/**
 * Admin-only POST /api/admin/registration-receipts/{kind}/{id}/resend
 * kind = farmer (id is farmerId) or calf (id is tagNumber).
 * Uses the saved, non-voided, synchronized server record and its original site recipient.
 * Never changes registration, farmer ownership, or sync flags. Never accepts an email address.
 */
fun Route.registrationReceiptResendRoutes(
    jwtService: JwtService,
    farmerService: FarmerService,
    calfService: CalfRegistrationService,
    notifications: FarmerSalesNotificationService
) {
    post("/api/admin/registration-receipts/{kind}/{id}/resend") {
        val principal = call.requireRole(jwtService, Role.ADMIN) ?: return@post
        val kind = call.parameters["kind"].orEmpty().lowercase()
        val id = call.parameters["id"].orEmpty()
        if (kind !in setOf("farmer", "calf") || id.isBlank() || id.length > 255) {
            call.respond(HttpStatusCode.BadRequest,
                ApiResponse<String>(false, "Specify a valid farmer or calf registration ID"))
            return@post
        }
        val request = try { call.receive<RegistrationReceiptResendRequest>() } catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(false, "A JSON reason is required"))
            return@post
        }
        val reason = request.reason.trim()
        if (reason.length !in 10..250) {
            call.respond(HttpStatusCode.BadRequest,
                ApiResponse<String>(false, "Provide a reason between 10 and 250 characters"))
            return@post
        }

        val info = if (kind == "farmer") farmerReceiptInfo(id) else calfReceiptInfo(id)
        if (info == null) {
            call.respond(HttpStatusCode.NotFound,
                ApiResponse<String>(false, "Synchronized registration not found"))
            return@post
        }
        val now = System.currentTimeMillis()
        if (!reserveReceiptResend(principal, kind.uppercase(), id, info.siteId, reason, now)) {
            call.respond(HttpStatusCode.TooManyRequests,
                ApiResponse<String>(false, "Receipt already requested recently; try again in two minutes"))
            return@post
        }

        val sent = try {
            withContext(Dispatchers.IO) {
                if (kind == "farmer") {
                    val farmer = farmerService.findById(id, RecordScope.All)
                        ?: throw IllegalStateException("Registration no longer exists")
                    notifications.notifyRegistration(
                        FarmerSalesNotificationPayload(
                            farmerId = farmer.farmerId,
                            clientCode = farmer.clientCode,
                            organisationName = farmer.organisationName,
                            emailAddress = farmer.emailAddress,
                            vatNumber = farmer.vatNumber,
                            coRegIdNo = farmer.coRegIdNo,
                            landOwnership = farmer.landOwnership,
                            faCodeRmis = farmer.faCodeRmis,
                            glnNumber = farmer.glnNumber,
                            herdCapacity = farmer.herdCapacity,
                            interestStatus = farmer.interestStatus,
                            contactName = farmer.contactName,
                            contactNumber = farmer.contactNumber,
                            farmSizeHa = farmer.farmSizeHa,
                            headCount = farmer.headCount,
                            primaryBreed = farmer.primaryBreed,
                            gpsLatitude = farmer.gpsLatitude,
                            gpsLongitude = farmer.gpsLongitude,
                            addresses = farmer.addresses,
                            roles = farmer.roles,
                            deviceId = "SERVER_RESEND", // Original farmer device is not stored server-side.
                            farmCode = info.siteId?.let { farmCodeOfSite(it) },
                            assignedSalesmanEmail = info.siteId?.let { salesRepEmailOfSite(it) },
                            submittedByUserId = info.submittedByUserId ?: "UNKNOWN_LEGACY",
                            submittedByUsername = info.originalUsername ?: "UNKNOWN_LEGACY",
                            submittedByRole = info.originalRole,
                            serverSyncedAt = info.syncedAt ?: now,
                            resentAt = now
                        )
                    )
                } else {
                    val calf = calfService.findByTagNumber(id, RecordScope.All)
                        ?: throw IllegalStateException("Registration no longer exists")
                    notifications.resendCalfRegistration(
                        CalfRegistrationNotificationPayload(
                            recordGuid = calf.recordguid,
                            animalUuid = calf.animalUuid,
                            tagNumber = calf.tagNumber,
                            breed = calf.breed,
                            birthdate = calf.birthdate,
                            captureAt = calf.captureAt,
                            deviceId = calf.deviceId,
                            siteId = info.siteId,
                            submittedByUserId = info.submittedByUserId,
                            serverSyncedAt = info.syncedAt ?: now,
                            assignedSalesmanEmail = info.siteId?.let { salesRepEmailOfSite(it) },
                            resentAt = now
                        )
                    )
                }
            }
        } catch (_: Exception) {
            false // Do not echo SMTP errors, passwords, or registration data in the API response.
        }

        val outcome = if (sent) "SENT" else "NOT_SENT"
        auditReceiptResendResult(principal, kind.uppercase(), id, info.siteId, outcome)
        if (sent) {
            call.respond(ApiResponse(success = true,
                message = "Registration JSON email accepted by SMTP transport", data = "SENT"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable,
                ApiResponse<String>(false,
                    "JSON receipt was not sent. Check the backend SMTP configuration and Render logs."))
        }
    }
}
