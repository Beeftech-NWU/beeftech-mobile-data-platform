package com.beeftech.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class RecordCount(
    val total: Long,
    val last7Days: Long
)

@Serializable
data class TreatmentCount(
    val total: Long,
    val last7Days: Long,
    val totalCost: Double
)

@Serializable
data class CostCount(
    val total: Long,
    val last7Days: Long,
    val totalAmount: Double
)

@Serializable
data class TeamCount(
    val activeWorkers: Long,
    val inactiveWorkers: Long
)

@Serializable
data class DashboardAlert(
    val type: String,
    val message: String,
    val username: String? = null,
    val lastSyncAt: Long? = null
)

/**
 * Counts for one site (or every site, for an admin). The newer sections default to null so
 * an old app reading a new server, or the reverse, still works.
 *
 * [costs] leaves out costs derived from treatments, which [treatments] already counts, so
 * a treatment's cost is never added twice. Voided records are left out everywhere.
 */
@Serializable
data class DashboardSummary(
    val siteId: String? = null,
    /* The site's name when the summary is for one site; null for all sites. */
    val siteName: String? = null,
    val generatedAt: Long,
    val calves: RecordCount,
    val treatments: TreatmentCount,
    val farmers: RecordCount,
    val mortalities: RecordCount? = null,
    val movements: RecordCount? = null,
    val costs: CostCount? = null,
    val feedReadings: RecordCount? = null,
    val team: TeamCount,
    val alerts: List<DashboardAlert>
)
