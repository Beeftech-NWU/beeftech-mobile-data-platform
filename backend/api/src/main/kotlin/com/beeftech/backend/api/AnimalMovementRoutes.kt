package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.animalMovementRoutes(
    jwtService: JwtService,
    animalMovementService:
    AnimalMovementService
) {

    post(
        "/api/animal-movements/sync"
    ) {

        val principal =
            call.requireAuthPrincipal(
                jwtService
            )
                ?: return@post

        try {

            val request =
                call.receive<
                        AnimalMovementSyncRequest
                        >()

            val response =
                animalMovementService.sync(
                    request,
                    principal.userId,
                    principal.siteId
                )

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = response,
                    message =
                        "Animal movements synchronized."
                )
            )

        } catch (
            exception: Exception
        ) {

            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<
                        AnimalMovementSyncResponse
                        >(
                    success = false,
                    data = null,
                    message =
                        exception.message
                            ?: "Unable to synchronize animal movements."
                )
            )
        }
    }

    get("/api/animal-movements") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Animal movements loaded",
                data = animalMovementService.list(principal.recordScope())
            )
        )
    }

    get("/api/animal-movements/{animalId}") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        val animalId = call.parameters["animalId"].orEmpty()

        call.respond(
            ApiResponse(
                success = true,
                message = "Animal movements loaded",
                data = animalMovementService.listForAnimal(animalId, principal.recordScope())
            )
        )
    }
}
