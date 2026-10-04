package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.SitesTable
import com.beeftech.backend.api.auth.UserRepository
import com.beeftech.backend.api.auth.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.IsoFields
import java.util.Locale

sealed interface ReportResult {
    data class Ok(val report: ReportResponse) : ReportResult
    data class Forbidden(val message: String) : ReportResult
    data class BadRequest(val message: String) : ReportResult
}

class ReportService(
    private val now: () -> Long = System::currentTimeMillis,
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Same scoping as the dashboard: a manager always gets their own site, an admin
     * gets every site or one site, and workers get nothing. Voided records are left out.
     *
     * Costs: treatment cost comes from the treatments table only, and the cost
     * reports leave out animal_costs rows derived from treatments. Otherwise a treatment
     * would be counted twice (future-checks #37), and voiding a treatment would not
     * void its derived cost row (#42).
     */
    suspend fun report(
        principal: AuthPrincipal,
        type: ReportType,
        requestedSiteId: String?,
        fromParam: String?,
        toParam: String?,
        bucketParam: String?
    ): ReportResult {

        val record = userRepository.findById(principal.userId)
        val role = Role.fromId(record?.role)
        if (record == null || !record.active) return ReportResult.Forbidden("Forbidden")

        val scope: RecordScope = when (role) {
            Role.ADMIN ->
                if (requestedSiteId == null) {
                    RecordScope.All
                } else {
                    if (userRepository.siteActive(requestedSiteId) == null) {
                        return ReportResult.BadRequest("Unknown site")
                    }
                    RecordScope.Site(requestedSiteId)
                }
            Role.MANAGER -> {
                if (requestedSiteId != null && requestedSiteId != record.siteId) {
                    return ReportResult.Forbidden("Managers can only view their own site")
                }
                RecordScope.Site(record.siteId)
            }
            else -> return ReportResult.Forbidden("Forbidden")
        }

        val generatedAt = now()
        val to = if (toParam == null) generatedAt else toParam.toLongOrNull()
            ?: return ReportResult.BadRequest("Invalid 'to'")
        val from = if (fromParam == null) to - DEFAULT_RANGE_MS else fromParam.toLongOrNull()
            ?: return ReportResult.BadRequest("Invalid 'from'")
        if (from > to) return ReportResult.BadRequest("'from' must not be after 'to'")
        if (to - from > MAX_RANGE_MS) return ReportResult.BadRequest("Range is limited to 366 days")

        val bucket = when (bucketParam ?: "day") {
            "day", "week", "month" -> bucketParam ?: "day"
            else -> return ReportResult.BadRequest("Invalid 'bucket'")
        }

        val scopeSiteId = (scope as? RecordScope.Site)?.siteId

        return newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val siteName = scopeSiteId?.let { id ->
                SitesTable.selectAll().where { SitesTable.siteId eq id }.singleOrNull()?.get(SitesTable.name)
            }

            val built = when (type) {
                ReportType.MORTALITY -> mortality(scope, from, to)
                ReportType.TREATMENT_COST -> treatmentCost(scope, from, to)
                ReportType.COST_PER_ANIMAL -> costPerAnimal(scope, from, to)
                ReportType.CALF_REGISTRATIONS -> calfRegistrations(scope, from, to, bucket)
                ReportType.WORKER_PRODUCTIVITY -> workerProductivity(scope, from, to)
            }

            ReportResult.Ok(
                ReportResponse(
                    report = type.path,
                    title = type.title,
                    siteId = scopeSiteId,
                    siteName = siteName,
                    from = from,
                    to = to,
                    generatedAt = generatedAt,
                    summary = built.summary,
                    columns = built.columns,
                    rows = built.rows,
                    footer = built.footer
                )
            )
        }
    }

    private class Built(
        val summary: List<ReportFigure>,
        val columns: List<String>,
        val rows: List<List<String>>,
        val footer: String? = null
    )

    private fun mortality(scope: RecordScope, from: Long, to: Long): Built {
        val mortScope = scope.predicate(MortalityTable.submittedByUserId, MortalityTable.siteId, MortalityTable.voidedAt)
        val calfScope = scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt)

        val causes = MortalityTable.selectAll()
            .where { mortScope and (MortalityTable.timestamp greaterEq from) and (MortalityTable.timestamp lessEq to) }
            .map { it[MortalityTable.causeOfDeath].trim().ifEmpty { "Unspecified" } }
        val calves = CalfRegistrationTable.selectAll()
            .where { calfScope and (CalfRegistrationTable.captureAt greaterEq from) and (CalfRegistrationTable.captureAt lessEq to) }
            .count()

        val rate = if (calves == 0L) "n/a" else percent(causes.size.toDouble() / calves)

        return Built(
            summary = listOf(
                ReportFigure("Mortalities", causes.size.toString()),
                ReportFigure("Calves registered", calves.toString()),
                ReportFigure("Mortality rate", rate)
            ),
            columns = listOf("Cause of death", "Mortalities"),
            rows = causes.groupingBy { it }.eachCount().entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { listOf(it.key, it.value.toString()) },
            footer = "Rate = mortalities / calves registered in the same period and scope."
        )
    }

    private fun treatmentCost(scope: RecordScope, from: Long, to: Long): Built {
        val treatScope = scope.predicate(TreatmentTable.submittedByUserId, TreatmentTable.siteId, TreatmentTable.voidedAt)
        val treatments = TreatmentTable.selectAll()
            .where { treatScope and (TreatmentTable.timestamp greaterEq from) and (TreatmentTable.timestamp lessEq to) }
            .toList()

        val total = treatments.sumOf { it[TreatmentTable.cost] }
        val byName = treatments.groupBy { it[TreatmentTable.treatmentName].trim().ifEmpty { "Unspecified" } }

        return Built(
            summary = listOf(
                ReportFigure("Treatments", treatments.size.toString()),
                ReportFigure("Total cost", money(total))
            ),
            columns = listOf("Treatment", "Treatments", "Cost"),
            rows = byName.entries
                .map { (name, rows) -> Triple(name, rows.size, rows.sumOf { it[TreatmentTable.cost] }) }
                .sortedWith(compareByDescending<Triple<String, Int, Double>> { it.third }.thenBy { it.first })
                .map { listOf(it.first, it.second.toString(), money(it.third)) }
        )
    }

    private fun costPerAnimal(scope: RecordScope, from: Long, to: Long): Built {
        val treatScope = scope.predicate(TreatmentTable.submittedByUserId, TreatmentTable.siteId, TreatmentTable.voidedAt)
        val costScope = scope.predicate(CostTable.submittedByUserId, CostTable.siteId)

        val perAnimal = sortedMapOf<String, Double>()
        var treatmentTotal = 0.0
        var otherTotal = 0.0

        TreatmentTable.selectAll()
            .where { treatScope and (TreatmentTable.timestamp greaterEq from) and (TreatmentTable.timestamp lessEq to) }
            .forEach {
                perAnimal.merge(it[TreatmentTable.animalId], it[TreatmentTable.cost], Double::plus)
                treatmentTotal += it[TreatmentTable.cost]
            }

        /* A manual cost has a null source_entity, and a null never matches neq in SQL. */
        val notDerivedFromTreatment: Op<Boolean> =
            CostTable.sourceEntity.isNull() or (CostTable.sourceEntity neq "TREATMENT")
        CostTable.selectAll()
            .where {
                costScope and notDerivedFromTreatment and
                    (CostTable.timestamp greaterEq from) and (CostTable.timestamp lessEq to)
            }
            .forEach {
                perAnimal.merge(it[CostTable.animalId], it[CostTable.amount], Double::plus)
                otherTotal += it[CostTable.amount]
            }

        return Built(
            summary = listOf(
                ReportFigure("Animals with costs", perAnimal.size.toString()),
                ReportFigure("Treatment cost", money(treatmentTotal)),
                ReportFigure("Other costs", money(otherTotal)),
                ReportFigure("Total cost", money(treatmentTotal + otherTotal))
            ),
            columns = listOf("Animal", "Cost"),
            rows = perAnimal.entries
                .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
                .map { listOf(it.key, money(it.value)) },
            footer = "Treatment cost comes from treatment records; costs derived from treatments are left out so nothing is counted twice."
        )
    }

    private fun calfRegistrations(scope: RecordScope, from: Long, to: Long, bucket: String): Built {
        val calfScope = scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt)
        val stamps = CalfRegistrationTable.selectAll()
            .where { calfScope and (CalfRegistrationTable.captureAt greaterEq from) and (CalfRegistrationTable.captureAt lessEq to) }
            .map { it[CalfRegistrationTable.captureAt] }

        val counts = stamps.groupingBy { bucketLabel(it, bucket) }.eachCount().toSortedMap()

        return Built(
            summary = listOf(ReportFigure("Calves registered", stamps.size.toString())),
            columns = listOf(bucket.replaceFirstChar { it.titlecase(Locale.ROOT) }, "Calves"),
            rows = counts.map { listOf(it.key, it.value.toString()) },
            footer = "Periods are in UTC."
        )
    }

    private fun workerProductivity(scope: RecordScope, from: Long, to: Long): Built {
        fun countBy(
            submittedBy: org.jetbrains.exposed.sql.Column<String?>,
            siteId: org.jetbrains.exposed.sql.Column<String?>,
            voidedAt: org.jetbrains.exposed.sql.Column<Long?>,
            stamp: org.jetbrains.exposed.sql.Column<Long>,
            table: org.jetbrains.exposed.sql.Table
        ): Map<String?, Int> {
            val inScope = scope.predicate(submittedBy, siteId, voidedAt)
            return table.selectAll()
                .where { inScope and (stamp greaterEq from) and (stamp lessEq to) }
                .groupingBy { it[submittedBy] }
                .eachCount()
        }

        val calves = countBy(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt, CalfRegistrationTable.captureAt, CalfRegistrationTable)
        val treatments = countBy(TreatmentTable.submittedByUserId, TreatmentTable.siteId, TreatmentTable.voidedAt, TreatmentTable.timestamp, TreatmentTable)
        val mortalities = countBy(MortalityTable.submittedByUserId, MortalityTable.siteId, MortalityTable.voidedAt, MortalityTable.timestamp, MortalityTable)
        val movements = countBy(AnimalMovementTable.submittedByUserId, AnimalMovementTable.siteId, AnimalMovementTable.voidedAt, AnimalMovementTable.timestamp, AnimalMovementTable)

        val ids = (calves.keys + treatments.keys + mortalities.keys + movements.keys)
        val names = UsersTable.selectAll().associate { it[UsersTable.userId] to it[UsersTable.username] }

        data class Row(val name: String, val calves: Int, val treatments: Int, val mortalities: Int, val movements: Int) {
            val total: Int get() = this.calves + this.treatments + this.mortalities + this.movements
        }

        val rows = ids.map { id ->
            Row(
                name = id?.let { names[it] ?: it } ?: "Unknown",
                calves = calves[id] ?: 0,
                treatments = treatments[id] ?: 0,
                mortalities = mortalities[id] ?: 0,
                movements = movements[id] ?: 0
            )
        }.sortedWith(compareByDescending<Row> { it.total }.thenBy { it.name })

        return Built(
            summary = listOf(
                ReportFigure("Workers with records", rows.size.toString()),
                ReportFigure("Records", rows.sumOf { it.total }.toString())
            ),
            columns = listOf("Worker", "Calves", "Treatments", "Mortalities", "Movements", "Total"),
            rows = rows.map {
                listOf(it.name, it.calves.toString(), it.treatments.toString(), it.mortalities.toString(), it.movements.toString(), it.total.toString())
            }
        )
    }

    private fun bucketLabel(epochMs: Long, bucket: String): String {
        val date = Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).toLocalDate()
        return when (bucket) {
            "month" -> "%04d-%02d".format(date.year, date.monthValue)
            "week" -> "%04d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
            else -> date.toString()
        }
    }

    private fun money(value: Double) = String.format(Locale.ROOT, "%.2f", value)

    private fun percent(fraction: Double) = String.format(Locale.ROOT, "%.1f%%", fraction * 100)

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val DEFAULT_RANGE_MS = 30 * DAY_MS
        const val MAX_RANGE_MS = 366 * DAY_MS
    }
}
