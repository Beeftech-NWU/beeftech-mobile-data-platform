package com.beeftech.backend.api

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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

    routing {

        get("/") {
            call.respondText("BeefTech Backend API is running")
        }

        get("/api/farm-traceability") {
            call.respondText("Farm Traceability API is running")
        }
    }
}