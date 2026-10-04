package com.beeftech.backend.api.auth

import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.requireRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/*
 * Admin and manager may list phones (a manager only their own site's); revoking and
 * reinstating a phone, and reading login events and lockouts, are admin only. The service
 * enforces that, so a manager gets a clear 403.
 */
fun Route.deviceAdminRoutes(
    jwtService: JwtService,
    service: DeviceAdminService
) {
    get("/api/devices") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get
        val query = call.request.queryParameters

        call.respondResult(service.listDevices(principal, query["siteId"], query["status"]), "Devices loaded")
    }

    post("/api/devices/{id}/revoke") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post
        val request = call.receive<DeviceReasonRequest>()

        call.respondResult(
            service.revoke(principal, call.parameters["id"].orEmpty(), request.reason),
            "Device revoked"
        )
    }

    post("/api/devices/{id}/reinstate") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post
        val request = call.receive<DeviceReasonRequest>()

        call.respondResult(
            service.reinstate(principal, call.parameters["id"].orEmpty(), request.reason),
            "Device reinstated"
        )
    }

    /* Page with before=<id of the last event you have>. */
    get("/api/login-events") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get
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
            service.loginEvents(
                principal, query["siteId"], query["userId"], query["outcome"], before, query["limit"]?.toIntOrNull()
            ),
            "Login events loaded"
        )
    }

    get("/api/login-security/lockouts") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get

        call.respondResult(service.lockouts(principal), "Lockouts loaded")
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: DeviceAdminResult<T>,
    successMessage: String
) {
    when (result) {
        is DeviceAdminResult.Ok -> respond(
            ApiResponse(success = true, message = successMessage, data = result.value)
        )
        DeviceAdminResult.NotFound -> respond(
            HttpStatusCode.NotFound,
            ApiResponse<String>(success = false, message = "Device not found")
        )
        is DeviceAdminResult.Forbidden -> respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(success = false, message = result.message)
        )
        is DeviceAdminResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(success = false, message = result.message)
        )
        is DeviceAdminResult.Conflict -> respond(
            HttpStatusCode.Conflict,
            ApiResponse<String>(success = false, message = result.message)
        )
    }
}
