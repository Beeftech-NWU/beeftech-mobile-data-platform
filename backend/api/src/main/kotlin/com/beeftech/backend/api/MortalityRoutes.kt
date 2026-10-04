package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.mortalityRoutes(
    jwtService: JwtService,
    mortalityService: MortalityService
) {

    /* Submitter and site come from the token, never the body. */
    post("/api/mortalities/sync") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@post

        val request = call.receive<MortalitySyncRequest>()

        call.respond(
            ApiResponse(
                success = true,
                message = "Mortalities synchronized.",
                data = mortalityService.sync(request, principal.userId, principal.siteId)
            )
        )
    }

    get("/api/mortalities") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Mortalities loaded",
                data = mortalityService.list(principal.recordScope())
            )
        )
    }

    get("/api/mortalities/{animalId}") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Mortalities loaded",
                data = mortalityService.listForAnimal(
                    call.parameters["animalId"].orEmpty(),
                    principal.recordScope()
                )
            )
        )
    }
}
