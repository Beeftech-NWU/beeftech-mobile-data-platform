package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthCheck
import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.AuthStateRepository
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/* One instance is enough: it keeps no state of its own and reads the current database each call. */
private val authState = AuthStateRepository()

/* Same checks as [requireAuthPrincipal]; kept for routes that only need the username. */
suspend fun ApplicationCall.requireBearerToken(jwtService: JwtService): String? =
    requireAuthPrincipal(jwtService)?.username

/*
 * Verifies the token, then checks it against the database: the user must exist and be active,
 * the token must not predate a deactivation, unbind or PIN reset, and the phone must not be
 * revoked. The returned principal carries the role and site the database has now, not the
 * ones in the token, so a role or site change applies without a new login.
 */
suspend fun ApplicationCall.requireAuthPrincipal(jwtService: JwtService): AuthPrincipal? {

    val authHeader = request.headers["Authorization"]

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {

        respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<String>(
                success = false,
                message = "Missing token"
            )
        )

        return null
    }

    val token = authHeader.removePrefix("Bearer ")

    val principal = jwtService.decode(token)

    if (principal == null) {

        respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<String>(
                success = false,
                message = "Invalid token"
            )
        )

        return null
    }

    return when (val check = authState.check(principal)) {
        is AuthCheck.Ok -> principal.copy(role = check.role, siteId = check.siteId)
        is AuthCheck.Rejected -> {
            respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<String>(
                    success = false,
                    message = check.message
                )
            )
            null
        }
    }
}

suspend fun ApplicationCall.requireRole(
    jwtService: JwtService,
    vararg roles: Role
): AuthPrincipal? {

    val principal = requireAuthPrincipal(jwtService) ?: return null

    if (principal.roleEnum !in roles) {

        respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(
                success = false,
                message = "Forbidden"
            )
        )

        return null
    }

    return principal
}
