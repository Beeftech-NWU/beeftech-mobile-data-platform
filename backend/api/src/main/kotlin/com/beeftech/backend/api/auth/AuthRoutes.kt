package com.beeftech.backend.api.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.requireBearerToken
import io.ktor.server.response.*
import io.ktor.server.routing.get

fun Route.authRoutes(
    authService: AuthService,
    jwtService: JwtService
) {
    post("/api/auth/login") {

        val request = call.receive<LoginRequest>()

        val result = authService.login(
            request.username,
            request.password
        )

        when (result) {

            is LoginResult.Success -> {

                call.respond(
                    ApiResponse(
                        success = true,
                        message = "Login successful",
                        data = LoginResponse(
                            result.token
                        )
                    )
                )
            }

            is LoginResult.Failure -> {

                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<String>(
                        success = false,
                        message = result.message
                    )
                )
            }

            is LoginResult.Locked -> {

                call.respond(
                    HttpStatusCode.Locked,
                    ApiResponse(
                        success = false,
                        message = "Account locked",
                        data = mapOf(
                            "remainingSeconds" to result.remainingSeconds
                        )
                    )
                )
            }
        }
    }

    post("/api/auth/register") {

        val request = call.receive<RegisterRequest>()

        val registered = authService.register(
            request.username,
            request.password
        )

        if (registered) {

            call.respond(
                ApiResponse<String>(
                    success = true,
                    message = "User registered successfully"
                )
            )
        }
    }

    get("/api/profile") {

        val username =
            call.requireBearerToken(jwtService) ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Profile loaded",
                data = ProfileResponse(
                    username = username
                )
            )
        )
    }

}