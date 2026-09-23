package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.treatmentRoutes(
    jwtService: JwtService,
    service: TreatmentService,
    referenceRepository: TreatmentReferenceRepository
) {

    /*
     * Synchronize treatment records from Android.
     */
    post("/api/treatments/sync") {

        call.requireBearerToken(jwtService)
            ?: return@post

        val request =
            call.receive<TreatmentSyncRequest>()

        val response =
            service.syncRecords(request)

        call.respond(
            ApiResponse(
                success = true,
                data = response,
                message = "Treatment synchronization completed."
            )
        )
    }

    /*
     * Load treatment reference/master data.
     *
     * The Disease and Treatment Type dropdowns on Android
     * will obtain their values from this endpoint.
     */
    get("/api/treatments/reference-data") {

        call.requireBearerToken(jwtService)
            ?: return@get

        val referenceData =
            referenceRepository.getReferenceData()

        call.respond(
            ApiResponse(
                success = true,
                data = referenceData,
                message = "Treatment reference data loaded successfully."
            )
        )
    }

    /*
     * Load all treatment records.
     */
    get("/api/treatments") {

        call.requireBearerToken(jwtService)
            ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                data = service.findAll(),
                message = "Treatments loaded successfully."
            )
        )
    }

    /*
     * Load treatment records for one animal.
     */
    get("/api/treatments/{animalId}") {

        call.requireBearerToken(jwtService)
            ?: return@get

        val animalId =
            call.parameters["animalId"]
                ?: return@get call.respond(
                    ApiResponse<List<TreatmentDto>>(
                        success = false,
                        data = null,
                        message = "Animal ID is required."
                    )
                )

        call.respond(
            ApiResponse(
                success = true,
                data = service.findByAnimalId(
                    animalId
                ),
                message = "Treatments loaded successfully."
            )
        )
    }
}