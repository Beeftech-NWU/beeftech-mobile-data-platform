package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

fun Route.calfRegistrationRoutes(
    jwtService: JwtService,
    service: CalfRegistrationService
) {

    post("/api/calf-registrations/sync") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@post

        val request = call.receive<CalfRegistrationSyncRequest>()

        val response = service.syncRecords(request, principal.userId, principal.siteId, principal.recordScope())

        call.respond(
            ApiResponse(
                success = true,
                message = "Sync complete",
                data = response
            )
        )
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

    /*
     * The raw JPEG is the request body (Content-Type: image/jpeg). The record must already
     * be synced, and the caller must be allowed to see it.
     */
    put("/api/calf-registrations/{tagNumber}/photo") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@put

        val tagNumber = call.parameters["tagNumber"]
        if (tagNumber.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "Missing tagNumber parameter")
            )
            return@put
        }

        val declaredLength = call.request.contentLength()
        if (declaredLength != null && declaredLength > CalfPhotoStore.MAX_BYTES) {
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                ApiResponse<String>(success = false, message = "Photo is larger than 5 MB")
            )
            return@put
        }

        val bytes = call.receive<ByteArray>()

        when (val outcome = service.storePhoto(tagNumber, bytes, principal.recordScope())) {
            is PhotoUploadOutcome.Stored ->
                call.respond(
                    ApiResponse(
                        success = true,
                        message = "Photo stored",
                        data = outcome.path
                    )
                )

            PhotoUploadOutcome.NotFound ->
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<String>(
                        success = false,
                        message = "Calf registration record not found for tagNumber: $tagNumber"
                    )
                )

            PhotoUploadOutcome.NotAJpeg ->
                call.respond(
                    HttpStatusCode.UnsupportedMediaType,
                    ApiResponse<String>(success = false, message = "Photo must be a JPEG image")
                )

            PhotoUploadOutcome.TooLarge ->
                call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    ApiResponse<String>(success = false, message = "Photo is larger than 5 MB")
                )
        }
    }

    get("/api/calf-registrations/{tagNumber}/photo") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        val tagNumber = call.parameters["tagNumber"]
        val bytes = tagNumber?.let { service.loadPhoto(it, principal.recordScope()) }

        if (bytes == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<String>(success = false, message = "Photo not found")
            )
            return@get
        }

        call.respondBytes(
            bytes = bytes,
            contentType = ContentType.Image.JPEG,
            status = HttpStatusCode.OK
        )
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
                "birth_certificate_${tagNumber}.pdf"
            ).toString()
        )

        call.respondBytes(
            bytes = pdfBytes,
            contentType = ContentType.Application.Pdf,
            status = HttpStatusCode.OK
        )
    }
}
