package com.beeftech.backend.api

import com.beeftech.backend.api.common.FileNaming.ProjectCode
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.farmerRoutes(
    jwtService: JwtService,
    service: FarmerService
) {

    /*
     * Synchronize locally stored farmer registrations
     * with the backend.
     */
    post("/api/farmers/sync") {

        val principal =
            call.requireAuthPrincipal(jwtService)
                ?: return@post

        val request =
            call.receive<FarmerSyncRequest>()

        if (!call.acceptBatch(principal, ProjectCode.FARMER_REG, request.batchName)) return@post

        val response =
            service.syncRecords(
                request = request,
                principal = principal
            )

        SyncUploadLog.record(principal, ProjectCode.FARMER_REG, request.batchName, response.results.map { it.status })

        call.respond(
            ApiResponse(
                success = true,
                data = response,
                message = "Farmer synchronization completed."
            )
        )
    }

    /*
     * Load all farmer registrations.
     */
    get("/api/farmers") {

        val principal =
            call.requireAuthPrincipal(jwtService)
                ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                data = service.findAll(principal.recordScope()),
                message = "Farmers loaded successfully."
            )
        )
    }

    /*
     * Load one farmer registration.
     */
    get("/api/farmers/{farmerId}") {

        val principal =
            call.requireAuthPrincipal(jwtService)
                ?: return@get

        val farmerId =
            call.parameters["farmerId"]
                ?: return@get call.respond(
                    ApiResponse<FarmerDto>(
                        success = false,
                        data = null,
                        message = "Farmer ID is required."
                    )
                )

        val farmer =
            service.findById(
                farmerId,
                principal.recordScope()
            )

        if (farmer == null) {

            call.respond(
                ApiResponse<FarmerDto>(
                    success = false,
                    data = null,
                    message = "Farmer not found."
                )
            )

            return@get
        }

        call.respond(
            ApiResponse(
                success = true,
                data = farmer,
                message = "Farmer loaded successfully."
            )
        )
    }
}