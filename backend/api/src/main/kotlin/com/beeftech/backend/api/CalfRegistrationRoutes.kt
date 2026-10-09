package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.calfRegistrationRoutes(
    jwtService: JwtService,
    service: CalfRegistrationService
) {

    post("/api/calf-registrations/sync") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@post

        val request = call.receive<CalfRegistrationSyncRequest>()

        val response = service.syncRecords(request, principal.userId, principal.siteId)

        call.respond(
            ApiResponse(
                success = true,
                message = "Sync complete",
                data = response
            )
        )
    }

    // Unlike the admin review list, this endpoint is limited to the current manager's
    // assigned site AND verifies that the selected farmer belongs to that same site.
    // It is safe to offer these records for *local* farmer-animal assignment.
    get("/api/calf-registrations/assignment-candidates") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@get
        if (principal.roleEnum != Role.MANAGER || principal.siteId.isNullOrBlank()) {
            call.respond(HttpStatusCode.Forbidden, ApiResponse<String>(false, "A site-assigned farm manager is required"))
            return@get
        }
        val farmerId = call.request.queryParameters["farmerId"].orEmpty()
        if (farmerId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(false, "Missing farmerId"))
            return@get
        }
        val accessibleFarmer = transaction {
            FarmerTable.selectAll().where {
                (FarmerTable.farmerId eq farmerId) and
                    principal.recordScope().predicate(
                        FarmerTable.submittedByUserId, FarmerTable.siteId, FarmerTable.voidedAt
                    )
            }.any()
        }
        if (!accessibleFarmer) {
            call.respond(HttpStatusCode.NotFound, ApiResponse<String>(false, "Farmer unavailable on your site"))
            return@get
        }
        call.respond(ApiResponse(
            success = true, message = "Site calves loaded",
            data = service.listAll(principal.recordScope())
        ))
    }

    get("/api/calf-registrations") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Calf registrations loaded",
                data = service.listAll(principal.recordScope())
            )
        )
    }

    get("/api/calf-registrations/{tagNumber}") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        val tagNumber = call.parameters["tagNumber"]

        val record = tagNumber?.let { service.findByTagNumber(it, principal.recordScope()) }

        if (record == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<String>(
                    success = false,
                    message = "Calf registration not found"
                )
            )
            return@get
        }

        call.respond(
            ApiResponse(
                success = true,
                message = "Calf registration loaded",
                data = record
            )
        )
    }

    post("/api/calf-registrations/{tagNumber}/media") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@post

        val tagNumber = call.parameters["tagNumber"]
        if (tagNumber.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "Missing tagNumber parameter")
            )
            return@post
        }

        // Never interpolate untrusted tag text into a server filesystem-style path.
        val safeTag = tagNumber.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val photoPath = "/media/photos/calf_${safeTag}.jpg"
        val updated = service.updateMedia(tagNumber, photoPath, principal.recordScope())

        if (updated) {
            call.respond(
                ApiResponse(
                    success = true,
                    message = "Media attachment updated successfully",
                    data = photoPath
                )
            )
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<String>(
                    success = false,
                    message = "Calf registration record not found for tagNumber: $tagNumber"
                )
            )
        }
    }

    get("/api/calf-registrations/{tagNumber}/certificate") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        val tagNumber = call.parameters["tagNumber"]
        if (tagNumber.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "Missing tagNumber parameter")
            )
            return@get
        }

        val pdfBytes = service.generateCertificatePdf(tagNumber, principal.recordScope())
        if (pdfBytes == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<String>(
                    success = false,
                    message = "Calf registration record not found"
                )
            )
            return@get
        }

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Inline.withParameter(
                ContentDisposition.Parameters.FileName,
                "birth_certificate_${tagNumber.replace(Regex("[^A-Za-z0-9._-]"), "_")}.pdf"
            ).toString()
        )

        call.respondBytes(
            bytes = pdfBytes,
            contentType = ContentType.Application.Pdf,
            status = HttpStatusCode.OK
        )
    }
}
