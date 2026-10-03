package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

sealed interface DashboardResult {
    data class Ok(val summary: DashboardSummary) : DashboardResult
    data class Forbidden(val message: String) : DashboardResult
}

class DashboardService(
    private val now: () -> Long = System::currentTimeMillis
) {

    /**
     * A manager always gets their own site. An admin gets every site, or one site
     * when [requestedSiteId] is given. Workers have no dashboard.
     */
    suspend fun summary(principal: AuthPrincipal, requestedSiteId: String?): DashboardResult {
        val scope: RecordScope = when (principal.roleEnum) {
            Role.ADMIN ->
                if (requestedSiteId == null) RecordScope.All else RecordScope.Site(requestedSiteId)
            Role.MANAGER -> {
                if (requestedSiteId != null && requestedSiteId != principal.siteId) {
                    return DashboardResult.Forbidden("Managers can only view their own site")
                }
                RecordScope.Site(principal.siteId)
            }
            else -> return DashboardResult.Forbidden("Forbidden")
        }

        val generatedAt = now()
        val weekAgo = generatedAt - WEEK_MS
        val staleBefore = generatedAt - STALE_SYNC_MS

        return newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val calfScope = scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId)
            val treatmentScope = scope.predicate(TreatmentTable.submittedByUserId, TreatmentTable.siteId)
            val farmerScope = scope.predicate(FarmerTable.submittedByUserId, FarmerTable.siteId)

            val calves = RecordCount(
                total = CalfRegistrationTable.selectAll().where { calfScope }.count(),
                last7Days = CalfRegistrationTable.selectAll()
                    .where { calfScope and (CalfRegistrationTable.captureAt greaterEq weekAgo) }
                    .count()
            )

            val cost = TreatmentTable.cost.sum()
            val treatments = TreatmentCount(
                total = TreatmentTable.selectAll().where { treatmentScope }.count(),
                last7Days = TreatmentTable.selectAll()
                    .where { treatmentScope and (TreatmentTable.timestamp greaterEq weekAgo) }
                    .count(),
                totalCost = TreatmentTable.select(cost).where { treatmentScope }
                    .singleOrNull()?.get(cost) ?: 0.0
            )

            /* Farmers carry no capture time, so "recent" is when they reached the server. */
            val farmers = RecordCount(
                total = FarmerTable.selectAll().where { farmerScope }.count(),
                last7Days = FarmerTable.selectAll()
                    .where { farmerScope and (FarmerTable.syncedAt greaterEq weekAgo) }
                    .count()
            )

            val workers = UsersTable.selectAll()
                .where { workerFilter(scope) }
                .map { it }

            val team = TeamCount(
                activeWorkers = workers.count { it[UsersTable.active] }.toLong(),
                inactiveWorkers = workers.count { !it[UsersTable.active] }.toLong()
            )

            val alerts = workers
                .filter { it[UsersTable.active] && it[UsersTable.deviceAssignedId] != null }
                .filter { (it[UsersTable.deviceLastSync] ?: 0L) < staleBefore }
                .sortedBy { it[UsersTable.username] }
                .map {
                    val last = it[UsersTable.deviceLastSync]
                    DashboardAlert(
                        type = "STALE_SYNC",
                        message = if (last == null) "${it[UsersTable.username]} has never synced"
                        else "${it[UsersTable.username]} hasn't synced in over 48 hours",
                        username = it[UsersTable.username],
                        lastSyncAt = last
                    )
                }

            DashboardResult.Ok(
                DashboardSummary(
                    siteId = (scope as? RecordScope.Site)?.siteId,
                    generatedAt = generatedAt,
                    calves = calves,
                    treatments = treatments,
                    farmers = farmers,
                    team = team,
                    alerts = alerts
                )
            )
        }
    }

    private fun workerFilter(scope: RecordScope): Op<Boolean> {
        val isWorker = UsersTable.role eq Role.WORKER.id
        return when (scope) {
            is RecordScope.Site ->
                if (scope.siteId == null) Op.FALSE else isWorker and (UsersTable.siteId eq scope.siteId)
            else -> isWorker
        }
    }

    private companion object {
        const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
        const val STALE_SYNC_MS = 48 * 60 * 60 * 1000L
    }
}
