package com.beeftech.backend.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.requireBearerToken(jwtService: JwtService): String? {

    val authHeader =
        request.headers["Authorization"]

    if (
        authHeader == null ||
        !authHeader.startsWith("Bearer ")
    ) {

        respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<String>(
                success = false,
                message = "Missing token"
            )
        )

        return null
    }

    val token =
        authHeader.removePrefix("Bearer ")

    val username =
        jwtService.validateToken(token)

    if (username == null) {

        respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<String>(
                success = false,
                message = "Invalid token"
            )
        )

        return null
    }

    return username
}
