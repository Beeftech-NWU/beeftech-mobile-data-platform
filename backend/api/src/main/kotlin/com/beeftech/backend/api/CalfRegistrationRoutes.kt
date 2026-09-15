package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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

        val animalId =
            call.parameters["animalId"]

        val record =
            animalId?.let { service.findByAnimalId(it) }

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
}
