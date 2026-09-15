package com.beeftech.backend.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*


fun main() {
    embeddedServer(
        factory = Netty,
        port = 8081,
        host = "0.0.0.0"
    ) {
        module()
    }.start(wait = true)
}

fun Application.module() {

    val jdbcUrl =
        System.getProperty("beeftech.db.url")
            ?: "jdbc:sqlite:./data/beeftech-backend.db"

    DatabaseFactory.init(jdbcUrl)

    install(ContentNegotiation) {
        json()
    }

    val jwtService = JwtService()
    val authService = AuthService(jwtService)

    val calfRegistrationRepository = CalfRegistrationRepository()
    val calfRegistrationService = CalfRegistrationService(calfRegistrationRepository)

    routing {

        calfRegistrationRoutes(jwtService, calfRegistrationService)

        get("/") {
            call.respondText("BeefTech Backend API is running")
        }

        get("/api/farm-traceability") {
            call.respondText("Farm Traceability API is running")
        }

        post("/api/auth/login") {

            val request = call.receive<LoginRequest>()

            val token = authService.login(
                request.username,
                request.password
            )

            if (token != null) {

                call.respond(
                    ApiResponse(
                        success = true,
                        message = "Login successful",
                        data = LoginResponse(token)
                    )
                )

            } else {

                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<String>(
                        success = false,
                        message = "Invalid credentials"
                    )
                )
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
}