package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/*
 * Admin and manager only; the service narrows a manager to their own site.
 * Void is the only correction: there are no edit endpoints.
 */
fun Route.voidRoutes(
    jwtService: JwtService,
    voidService: VoidService
) {

    post("/api/records/{type}/{id}/void") {

        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post
        val request = call.receive<VoidRequest>()

        call.respondResult(
            voidService.void(
                principal,
                call.parameters["type"].orEmpty(),
                call.parameters["id"].orEmpty(),
                request
            ),
            "Record voided"
        )
    }

    /* Voided records are included unless includeVoided=false. */
    get("/api/records/{type}") {

        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get

        call.respondResult(
            voidService.review(
                principal,
                call.parameters["type"].orEmpty(),
                call.request.queryParameters["includeVoided"] != "false",
                call.request.queryParameters["limit"]?.toIntOrNull()
            ),
            "Records loaded"
        )
    }

    get("/api/audit-log") {

        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get

        call.respondResult(
            voidService.auditLog(principal, call.request.queryParameters["limit"]?.toIntOrNull()),
            "Audit log loaded"
        )
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: VoidResult<T>,
    successMessage: String
) {
    when (result) {
        is VoidResult.Ok -> respond(
            ApiResponse(success = true, message = successMessage, data = result.value)
        )
        VoidResult.NotFound -> respond(
            HttpStatusCode.NotFound,
            ApiResponse<String>(success = false, message = "Record not found")
        )
        is VoidResult.Forbidden -> respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(success = false, message = result.message)
        )
        is VoidResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(success = false, message = result.message)
        )
        is VoidResult.Conflict -> respond(
            HttpStatusCode.Conflict,
            ApiResponse<String>(success = false, message = result.message)
        )
    }
}
