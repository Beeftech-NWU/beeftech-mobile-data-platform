package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.dashboardRoutes(
    jwtService: JwtService,
    dashboardService: DashboardService
) {
    get("/api/dashboard/summary") {
        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get

        when (val result = dashboardService.summary(principal, call.request.queryParameters["siteId"])) {
            is DashboardResult.Ok -> call.respond(
                ApiResponse(success = true, message = "Dashboard loaded", data = result.summary)
            )
            is DashboardResult.Forbidden -> call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<String>(success = false, message = result.message)
            )
        }
    }
}
