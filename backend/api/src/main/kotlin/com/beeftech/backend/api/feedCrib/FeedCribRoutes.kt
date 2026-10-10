package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.SyncUploadLog
import com.beeftech.backend.api.acceptBatch
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.common.FileNaming.ProjectCode
import com.beeftech.backend.api.requireAuthPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.feedCribRoutes(
    jwtService: JwtService,
    feedCribService: FeedCribService
) {

    /*
     * The caller's own site. An admin has no site, so they name one with ?siteId=. Entries are the
     * whole site's, not the caller's, so a worker's history shows other phones' readings too.
     */
    get("/api/feed-cribs") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@get

        val siteId =
            if (principal.roleEnum == Role.ADMIN) call.request.queryParameters["siteId"] else principal.siteId

        if (siteId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(
                    success = false,
                    message = if (principal.roleEnum == Role.ADMIN) "Name a site with ?siteId=" else "Your account has no site"
                )
            )
            return@get
        }

        val days = call.request.queryParameters["days"]?.toIntOrNull() ?: FeedCribService.DEFAULT_DAYS

        call.respond(
            ApiResponse(
                success = true,
                message = "Feed cribs loaded",
                data = feedCribService.load(siteId, days)
            )
        )
    }

    /* Submitter and site come from the token, never the body. */
    post("/api/feed-crib-entries/sync") {

        val principal = call.requireAuthPrincipal(jwtService) ?: return@post

        val request = call.receive<FeedCribEntrySyncRequest>()

        if (!call.acceptBatch(principal, ProjectCode.FEED_CRIB, request.batchName)) return@post

        val response = feedCribService.sync(request, principal.userId, principal.siteId)

        SyncUploadLog.record(principal, ProjectCode.FEED_CRIB, request.batchName, response.results.map { it.status })

        call.respond(
            ApiResponse(
                success = true,
                message = "Feed crib entries synchronized.",
                data = response
            )
        )
    }
}
