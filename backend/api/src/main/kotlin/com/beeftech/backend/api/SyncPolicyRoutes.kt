package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

/*
 * Anyone signed in may read (phones pull it); only an admin writes, which the service enforces
 * so a manager or worker gets a clear 403.
 */
fun Route.syncPolicyRoutes(
    jwtService: JwtService,
    service: SyncPolicyService
) {
    get("/api/sync-policy") {
        call.requireAuthPrincipal(jwtService) ?: return@get

        call.respondResult(service.get(), "Sync policy loaded")
    }

    put("/api/sync-policy") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@put
        val request = call.receive<UpdateSyncPolicyRequest>()

        call.respondResult(service.update(principal, request), "Sync policy saved")
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: SyncPolicyResult<T>,
    successMessage: String
) {
    when (result) {
        is SyncPolicyResult.Ok -> respond(
            ApiResponse(success = true, message = successMessage, data = result.value)
        )
        is SyncPolicyResult.Forbidden -> respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(success = false, message = result.message)
        )
        is SyncPolicyResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(success = false, message = result.message)
        )
    }
}
