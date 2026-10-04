package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/*
 * Any signed-in user uploads their own phone's events. Reading them, listing locked accounts and
 * clearing a lock are admin only, which the service enforces so others get a clear 403.
 */
fun Route.syncSecurityRoutes(
    jwtService: JwtService,
    service: SyncSecurityService
) {
    post("/api/sync-security-events") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@post
        val request = call.receive<SyncSecurityEventsRequest>()

        call.respondResult(service.upload(principal, request), "Events received")
    }

    /* Page with before=<id of the last event you have>. */
    get("/api/sync-security-events") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@get
        val query = call.request.queryParameters

        val before = query["before"]?.let { value ->
            value.toLongOrNull() ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(success = false, message = "before must be a number")
                )
                return@get
            }
        }

        call.respondResult(
            service.events(
                principal, query["siteId"], query["userId"], query["eventType"], before, query["limit"]?.toIntOrNull()
            ),
            "Events loaded"
        )
    }

    get("/api/sync-security-events/locked") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        call.respondResult(service.locked(principal, call.request.queryParameters["siteId"]), "Locked accounts loaded")
    }

    post("/api/users/{id}/clear-sync-lock") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@post

        call.respondResult(
            service.clearLock(principal, call.parameters["id"].orEmpty()),
            "Sync lock cleared"
        )
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: SyncSecurityResult<T>,
    successMessage: String
) {
    when (result) {
        is SyncSecurityResult.Ok -> respond(
            ApiResponse(success = true, message = successMessage, data = result.value)
        )
        SyncSecurityResult.NotFound -> respond(
            HttpStatusCode.NotFound,
            ApiResponse<String>(success = false, message = "User not found")
        )
        is SyncSecurityResult.Forbidden -> respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(success = false, message = result.message)
        )
        is SyncSecurityResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(success = false, message = result.message)
        )
    }
}
