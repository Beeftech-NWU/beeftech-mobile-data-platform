package com.beeftech.backend.api

import kotlinx.serialization.Serializable

enum class ReportType(val path: String, val title: String) {
    MORTALITY("mortality", "Mortality report"),
    TREATMENT_COST("treatment-cost", "Treatment cost report"),
    COST_PER_ANIMAL("cost-per-animal", "Cost per animal report"),
    CALF_REGISTRATIONS("calf-registrations", "Calf registrations report"),
    WORKER_PRODUCTIVITY("worker-productivity", "Worker productivity report")
}

@Serializable
data class ReportFigure(
    val label: String,
    val value: String
)

/*
 * Every report has the same shape (headline figures plus a table), so JSON, CSV,
 * PDF and the app screen all render from one model.
 */
@Serializable
data class ReportResponse(
    val report: String,
    val title: String,
    val siteId: String? = null,
    val siteName: String? = null,
    val from: Long,
    val to: Long,
    val generatedAt: Long,
    val summary: List<ReportFigure>,
    val columns: List<String>,
    val rows: List<List<String>>,
    val footer: String? = null
)
