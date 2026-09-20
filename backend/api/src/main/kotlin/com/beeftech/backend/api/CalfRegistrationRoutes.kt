package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
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

fun Route.calfRegistrationRoutes(
    jwtService: JwtService,
    service: CalfRegistrationService
) {

    post("/api/calf-registrations/sync") {

        call.requireBearerToken(jwtService) ?: return@post

        val request = call.receive<CalfRegistrationSyncRequest>()

        val response = service.syncRecords(request)

        call.respond(
            ApiResponse(
                success = true,
                message = "Sync complete",
                data = response
            )
        )
    }

    get("/api/calf-registrations") {

        call.requireBearerToken(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Calf registrations loaded",
                data = service.listAll()
            )
        )
    }

    get("/api/calf-registrations/{animalId}") {

        call.requireBearerToken(jwtService) ?: return@get

        val animalId = call.parameters["animalId"]

        val record = animalId?.let { service.findByAnimalId(it) }

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

    post("/api/calf-registrations/{animalId}/media") {

        call.requireBearerToken(jwtService) ?: return@post

        val animalId = call.parameters["animalId"]
        if (animalId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "Missing animalId parameter")
            )
            return@post
        }

        val photoPath = "/media/photos/calf_${animalId}.jpg"
        val updated = service.updateMedia(animalId, photoPath)

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
                    message = "Calf registration record not found for animalId: $animalId"
                )
            )
        }
    }

    get("/api/calf-registrations/{animalId}/certificate") {

        call.requireBearerToken(jwtService) ?: return@get

        val animalId = call.parameters["animalId"]
        if (animalId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "Missing animalId parameter")
            )
            return@get
        }

        val pdfBytes = service.generateCertificatePdf(animalId)
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
                "birth_certificate_${animalId}.pdf"
            ).toString()
        )

        call.respondBytes(
            bytes = pdfBytes,
            contentType = ContentType.Application.Pdf,
            status = HttpStatusCode.OK
        )
    }
}
