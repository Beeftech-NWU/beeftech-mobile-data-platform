package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.requireBearerToken
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.feedCribRoutes(
    jwtService: JwtService,
    feedCribService: FeedCribService
) {

    post("/api/feed-crib") {

        call.requireBearerToken(jwtService)
            ?: return@post

        val request =
            call.receive<FeedCribRequest>()

        val response =
            feedCribService.saveReading(
                request
            )

        call.respond(
            ApiResponse(
                success = true,
                message = "Feed crib reading saved",
                data = response
            )
        )
    }

    get("/api/feed-crib") {

        call.requireBearerToken(jwtService)
            ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Feed crib records loaded",
                data = feedCribService.getAll()
            )
        )
    }

    get("/api/feed-crib/{penName}") {

        call.requireBearerToken(jwtService)
            ?: return@get

        val penName =
            call.parameters["penName"]
                ?: return@get

        call.respond(
            ApiResponse(
                success = true,
                message = "Feed crib records loaded",
                data = feedCribService.getByPenName(
                    penName
                )
            )
        )
    }
}