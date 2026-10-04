package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/*
 * Admin and manager only; the service narrows a manager to their own site.
 * Filters are all optional. Page with before=<id of the last row you have>.
 */
fun Route.auditRoutes(
    jwtService: JwtService,
    auditService: AuditService
) {

    get("/api/audit-log") {

        val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get
        val query = call.request.queryParameters

        val numbers = listOf("from", "to", "before").associateWith { name ->
            query[name]?.let { it.toLongOrNull() ?: Long.MIN_VALUE }
        }
        numbers.entries.firstOrNull { it.value == Long.MIN_VALUE }?.let {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "${it.key} must be a number")
            )
            return@get
        }

        val filter = AuditFilter(
            action = query["action"],
            entityType = query["entityType"],
            entityId = query["entityId"],
            actorUserId = query["actorUserId"],
            siteId = query["siteId"],
            from = numbers["from"],
            to = numbers["to"],
            before = numbers["before"]
        )

        when (val result = auditService.list(principal, filter, query["limit"]?.toIntOrNull())) {
            is AuditResult.Ok -> call.respond(
                ApiResponse(success = true, message = "Audit log loaded", data = result.value)
            )
            is AuditResult.Forbidden -> call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<String>(success = false, message = result.message)
            )
        }
    }
}
