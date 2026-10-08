package com.beeftech.backend.api

import com.beeftech.backend.api.common.FileNaming
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Instant
import java.time.ZoneOffset

fun Route.reportRoutes(
    jwtService: JwtService,
    reportService: ReportService
) {
    ReportType.entries.forEach { type ->
        get("/api/reports/${type.path}") {
            val principal = call.requireRole(jwtService, Role.ADMIN, Role.MANAGER) ?: return@get
            val query = call.request.queryParameters

            val format = query["format"] ?: "json"
            if (format !in setOf("json", "csv", "pdf")) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(success = false, message = "Invalid 'format'")
                )
                return@get
            }

            when (
                val result = reportService.report(
                    principal, type, query["siteId"], query["from"], query["to"], query["bucket"]
                )
            ) {
                is ReportResult.Ok -> {
                    val report = result.report
                    when (format) {
                        "json" -> call.respond(ApiResponse(success = true, message = "Report loaded", data = report))
                        "csv" -> {
                            call.attachment(fileName(report, "csv"))
                            call.respondBytes(ReportCsv.render(report).toByteArray(Charsets.UTF_8), ContentType.Text.CSV)
                        }
                        else -> {
                            call.attachment(fileName(report, "pdf"))
                            call.respondBytes(PdfGenerator.generateReport(report), ContentType.Application.Pdf)
                        }
                    }
                }
                is ReportResult.Forbidden -> call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<String>(success = false, message = result.message)
                )
                is ReportResult.BadRequest -> call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(success = false, message = result.message)
                )
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.attachment(fileName: String) {
    response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
    )
}

/** Farm code used for a report that covers every site, so it has no single farm to name it after. */
internal const val ALL_SITES_FARM_CODE = "ALLS"

/** [FarmCode]-REPORT-[YYYYMMDD]-[HHMMSS]-SERVER.ext, with the generated time in UTC. */
internal suspend fun fileName(report: ReportResponse, extension: String): String {
    val farmCode = report.siteId?.let { farmCodeOfSite(it) } ?: ALL_SITES_FARM_CODE
    return FileNaming.build(
        farmCode = farmCode,
        project = FileNaming.ProjectCode.REPORT,
        instant = Instant.ofEpochMilli(report.generatedAt),
        deviceId = "SERVER",
        extension = extension,
        zone = ZoneOffset.UTC
    )
}
