package com.beeftech.backend.api

import com.beeftech.backend.api.common.FileNaming.ProjectCode
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.costRoutes(
    jwtService: JwtService,
    costService: CostService
) {

    /* Submitter and site come from the token, never the body. */
    post("/api/costs/sync") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@post

        val request = call.receive<CostSyncRequest>()

        if (!call.acceptBatch(principal, ProjectCode.COST, request.batchName)) return@post

        val response = costService.sync(request, principal.userId, principal.siteId)

        SyncUploadLog.record(principal, ProjectCode.COST, request.batchName, response.results.map { it.status })

        call.respond(
            ApiResponse(
                success = true,
                message = "Costs synchronized.",
                data = response
            )
        )
    }

    get("/api/costs") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Costs loaded",
                data = costService.list(principal.recordScope())
            )
        )
    }

    get("/api/costs/{animalId}") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Costs loaded",
                data = costService.listForAnimal(
                    call.parameters["animalId"].orEmpty(),
                    principal.recordScope()
                )
            )
        )
    }
}
