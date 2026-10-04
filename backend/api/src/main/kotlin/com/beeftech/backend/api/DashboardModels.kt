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
 * Counts for one site (or every site, for an admin). Mortalities, costs, movements and
 * feed are not here yet: the backend has no sync path for them.
 */
@Serializable
data class DashboardSummary(
    val siteId: String? = null,
    val generatedAt: Long,
    val calves: RecordCount,
    val treatments: TreatmentCount,
    val farmers: RecordCount,
    val team: TeamCount,
    val alerts: List<DashboardAlert>
)
