package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

/*
 * Anyone signed in may read; only an admin writes (the service enforces that, so a manager or
 * worker gets a clear 403). The older GET /api/treatments/reference-data is unchanged for old apps.
 */
fun Route.referenceDataRoutes(
    jwtService: JwtService,
    service: ReferenceDataService
) {
    /* ?ifVersion=N returns just {version, unchanged:true} when the device is already up to date. */
    get("/api/reference-data") {
        call.requireAuthPrincipal(jwtService) ?: return@get

        val raw = call.request.queryParameters["ifVersion"]
        val ifVersion = raw?.let {
            it.toLongOrNull() ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(success = false, message = "ifVersion must be a number")
                )
                return@get
            }
        }

        call.respondResult(service.snapshot(ifVersion), "Reference data loaded")
    }

    post("/api/reference-data/{kind}") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@post
        val request = call.receive<CreateReferenceItemRequest>()

        call.respondResult(
            service.create(principal, call.parameters["kind"].orEmpty(), request),
            "Reference value added",
            HttpStatusCode.Created
        )
    }

    patch("/api/reference-data/{kind}/{id}") {
        val principal = call.requireAuthPrincipal(jwtService) ?: return@patch
        val request = call.receive<SetReferenceActiveRequest>()

        call.respondResult(
            service.setActive(principal, call.parameters["kind"].orEmpty(), call.parameters["id"].orEmpty(), request.active),
            "Reference value updated"
        )
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: ReferenceDataResult<T>,
    successMessage: String,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    when (result) {
        is ReferenceDataResult.Ok -> respond(
            successStatus,
            ApiResponse(success = true, message = successMessage, data = result.value)
        )
        ReferenceDataResult.NotFound -> respond(
            HttpStatusCode.NotFound,
            ApiResponse<String>(success = false, message = "Reference value not found")
        )
        is ReferenceDataResult.Forbidden -> respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(success = false, message = result.message)
        )
        is ReferenceDataResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(success = false, message = result.message)
        )
        is ReferenceDataResult.Conflict -> respond(
            HttpStatusCode.Conflict,
            ApiResponse<String>(success = false, message = result.message)
        )
    }
}
