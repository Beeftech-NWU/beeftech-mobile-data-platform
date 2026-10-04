package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthCheck
import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.AuthStateRepository
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UserRepository
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/*
 * Stateless helpers backed by the live database.
 */
private val authState =
    AuthStateRepository()

private val liveUsers =
    UserRepository()


/*
 * Same security checks as requireAuthPrincipal.
 * Kept for routes that only need the username.
 */
suspend fun ApplicationCall.requireBearerToken(
    jwtService: JwtService
): String? =
    requireAuthPrincipal(
        jwtService
    )
        ?.username


/*
 * Security order:
 *
 * 1. Decode and validate the JWT.
 * 2. Check current backend account/device security state.
 * 3. Refresh mutable authorization fields from the live user record.
 *
 * This means:
 *
 * - deactivation applies immediately
 * - PIN reset/revocation applies immediately
 * - role changes apply immediately
 * - site changes apply immediately
 * - older tokens still receive the current user/site/device identity
 */
suspend fun ApplicationCall.requireAuthPrincipal(
    jwtService: JwtService
): AuthPrincipal? {

    val authHeader =
        request.headers[
            "Authorization"
        ]


    if (
        authHeader == null ||
        !authHeader.startsWith(
            "Bearer "
        )
    ) {

        respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<String>(
                success =
                    false,
                message =
                    "Missing token"
            )
        )

        return null
    }


    val token =
        authHeader.removePrefix(
            "Bearer "
        )


    val principal =
        jwtService.decode(
            token
        )


    if (
        principal == null
    ) {

        respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<String>(
                success =
                    false,
                message =
                    "Invalid token"
            )
        )

        return null
    }


    return when (
        val check =
            authState.check(
                principal
            )
    ) {

        is AuthCheck.Ok -> {

            val currentUser =
                liveUsers
                    .findByUsername(
                        principal.username
                    )


            if (
                currentUser == null
            ) {

                respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<String>(
                        success =
                            false,
                        message =
                            "Authenticated user no longer exists"
                    )
                )

                null

            } else {

                principal.copy(
                    userId =
                        currentUser.userId,

                    role =
                        check.role,

                    siteId =
                        check.siteId,

                    deviceId =
                        currentUser
                            .deviceAssignedId
                            ?: principal.deviceId
                )
            }
        }


        is AuthCheck.Rejected -> {

            respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<String>(
                    success =
                        false,
                    message =
                        check.message
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

    val principal =
        requireAuthPrincipal(
            jwtService
        )
            ?: return null


    if (
        principal.roleEnum !in
        roles
    ) {

        respond(
            HttpStatusCode.Forbidden,
            ApiResponse<String>(
                success =
                    false,
                message =
                    "Forbidden"
            )
        )

        return null
    }


    return principal
}
