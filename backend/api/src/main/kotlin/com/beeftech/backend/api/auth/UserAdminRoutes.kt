package com.beeftech.backend.api.auth

import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.requireRole
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

/*
 * Admin and manager only; the service narrows a manager to workers on their own site.
 */
fun Route.userAdminRoutes(
    jwtService: JwtService,
    userAdminService: UserAdminService
) {
    get("/api/users") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get

        call.respondResult(
            userAdminService.list(principal, call.request.queryParameters["siteId"]),
            "Users loaded"
        )
    }

    post("/api/users") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post
        val request = call.receive<CreateUserRequest>()

        call.respondResult(
            userAdminService.create(principal, request),
            "User created",
            HttpStatusCode.Created
        )
    }

    patch("/api/users/{id}") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@patch
        val request = call.receive<UpdateUserRequest>()

        call.respondResult(
            userAdminService.update(principal, call.parameters["id"].orEmpty(), request),
            "User updated"
        )
    }

    post("/api/users/{id}/reset-pin") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post
        /* The body is optional: with no content type there is nothing to deserialize. */
        val request =
            if (call.request.headers[HttpHeaders.ContentType] == null) ResetPinRequest()
            else call.receiveNullable<ResetPinRequest>() ?: ResetPinRequest()

        call.respondResult(
            userAdminService.resetPin(principal, call.parameters["id"].orEmpty(), request.pin),
            "PIN reset"
        )
    }

    post("/api/users/{id}/unlock-login") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post

        call.respondResult(
            userAdminService.unlockLogin(principal, call.parameters["id"].orEmpty()),
            "Login unlocked"
        )
    }

    post("/api/users/{id}/unbind-device") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@post

        call.respondResult(
            userAdminService.unbindDevice(principal, call.parameters["id"].orEmpty()),
            "Device unbound"
        )
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: UserAdminResult<T>,
    successMessage: String,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    when (result) {
        is UserAdminResult.Ok -> respond(
            successStatus,
            ApiResponse(success = true, message = successMessage, data = result.value)
        )
        UserAdminResult.NotFound -> respond(
            HttpStatusCode.NotFound,
            ApiResponse<String>(success = false, message = "User not found")
        )
        is UserAdminResult.Forbidden -> respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(success = false, message = result.message)
        )
        is UserAdminResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(success = false, message = result.message)
        )
        is UserAdminResult.Conflict -> respond(
            HttpStatusCode.Conflict,
            ApiResponse<String>(success = false, message = result.message)
        )
    }
}
