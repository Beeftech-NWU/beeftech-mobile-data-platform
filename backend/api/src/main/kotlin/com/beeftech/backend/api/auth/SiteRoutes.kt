package com.beeftech.backend.api.auth

import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.requireRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

/*
 * Admin and manager may read (a manager only their own site); only an admin writes.
 * The service enforces that, so a manager gets a clear 403 rather than a 404.
 */
fun Route.siteRoutes(
    jwtService: JwtService,
    siteService: SiteService
) {
    get("/api/sites") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get

        call.respondResult(siteService.list(principal), "Sites loaded")
    }

    post("/api/sites") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post
        val request = call.receive<CreateSiteRequest>()

        call.respondResult(siteService.create(principal, request), "Site created", HttpStatusCode.Created)
    }

    patch("/api/sites/{id}") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@patch
        val request = call.receive<UpdateSiteRequest>()

        call.respondResult(
            siteService.update(principal, call.parameters["id"].orEmpty(), request),
            "Site updated"
        )
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: SiteResult<T>,
    successMessage: String,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    when (result) {
        is SiteResult.Ok -> respond(
            successStatus,
            ApiResponse(success = true, message = successMessage, data = result.value)
        )
        SiteResult.NotFound -> respond(
            HttpStatusCode.NotFound,
            ApiResponse<String>(success = false, message = "Site not found")
        )
        is SiteResult.Forbidden -> respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(success = false, message = result.message)
        )
        is SiteResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(success = false, message = result.message)
        )
        is SiteResult.Conflict -> respond(
            HttpStatusCode.Conflict,
            ApiResponse<String>(success = false, message = result.message)
        )
    }
}
