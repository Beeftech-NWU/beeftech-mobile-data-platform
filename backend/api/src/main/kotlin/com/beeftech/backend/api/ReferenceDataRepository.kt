package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

sealed interface ReferenceChangeOutcome {
    data class Changed(val version: Long, val entry: ReferenceEntryDto) : ReferenceChangeOutcome
    data class Unchanged(val version: Long, val entry: ReferenceEntryDto) : ReferenceChangeOutcome
    data object NotFound : ReferenceChangeOutcome
    data object Duplicate : ReferenceChangeOutcome
}

class ReferenceDataRepository {

    suspend fun version(): Long =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) { currentVersion() }

    suspend fun snapshot(): ReferenceDataSnapshot =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            ReferenceDataSnapshot(
                version = currentVersion(),
                diseases = DiseaseTable.selectAll()
                    .map { ReferenceItemDto(it[DiseaseTable.id], it[DiseaseTable.name], it[DiseaseTable.active]) }
                    .sortedBy { it.name.lowercase() },
                treatmentTypes = TreatmentTypeTable.selectAll()
                    .map { ReferenceItemDto(it[TreatmentTypeTable.id], it[TreatmentTypeTable.name], it[TreatmentTypeTable.active]) }
                    .sortedBy { it.name.lowercase() },
                costTypes = CostTypeTable.selectAll()
                    .orderBy(CostTypeTable.sortOrder, SortOrder.ASC)
                    .map {
                        CostTypeDto(it[CostTypeTable.code], it[CostTypeTable.displayName], it[CostTypeTable.sortOrder], it[CostTypeTable.active])
                    }
            )
        }

    /* Adds a disease or treatment type. A duplicate name (ignoring case) is refused. */
    suspend fun createNamed(kind: ReferenceKind, name: String, audit: (ReferenceEntryDto) -> AuditEntry, now: Long): ReferenceChangeOutcome =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val table = namedTable(kind)
            val exists = table.table.selectAll()
                .where { table.name.lowerCase() eq name.lowercase() }
                .any()
            if (exists) return@newSuspendedTransaction ReferenceChangeOutcome.Duplicate

            val id = table.table.insert {
                it[table.name] = name
                it[table.active] = true
            }[table.id]
            val entry = ReferenceEntryDto(kind.slug, id.toString(), name, true)

            insertAuditRow(audit(entry), now)
            ReferenceChangeOutcome.Changed(bumpReferenceDataVersion(now, null), entry)
        }

    suspend fun createCostType(code: String, displayName: String, sortOrder: Int?, audit: (ReferenceEntryDto) -> AuditEntry, now: Long): ReferenceChangeOutcome =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            if (CostTypeTable.selectAll().where { CostTypeTable.code eq code }.any()) {
                return@newSuspendedTransaction ReferenceChangeOutcome.Duplicate
            }
            val order = sortOrder ?: ((CostTypeTable.select(CostTypeTable.sortOrder.max()).singleOrNull()
                ?.get(CostTypeTable.sortOrder.max()) ?: -1) + 1)

            CostTypeTable.insert {
                it[CostTypeTable.code] = code
                it[CostTypeTable.displayName] = displayName
                it[CostTypeTable.sortOrder] = order
                it[active] = true
                it[createdAt] = now
            }
            val entry = ReferenceEntryDto(ReferenceKind.COST_TYPES.slug, code, displayName, true, order)

            insertAuditRow(audit(entry), now)
            ReferenceChangeOutcome.Changed(bumpReferenceDataVersion(now, null), entry)
        }

    /* Finds one value, or null. [id] is a number for diseases and treatment types and the code for cost types. */
    suspend fun find(kind: ReferenceKind, id: String): ReferenceEntryDto? =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) { findEntry(kind, id) }

    /*
     * Flips the active flag. Setting the flag it already has changes nothing: no version bump and
     * no audit row, so repeating a request is harmless.
     */
    suspend fun setActive(kind: ReferenceKind, id: String, active: Boolean, audit: (ReferenceEntryDto) -> AuditEntry, now: Long): ReferenceChangeOutcome =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val current = findEntry(kind, id) ?: return@newSuspendedTransaction ReferenceChangeOutcome.NotFound
            if (current.active == active) {
                return@newSuspendedTransaction ReferenceChangeOutcome.Unchanged(currentVersion(), current)
            }

            if (kind == ReferenceKind.COST_TYPES) {
                CostTypeTable.update({ CostTypeTable.code eq id }) { it[CostTypeTable.active] = active }
            } else {
                val table = namedTable(kind)
                table.table.update({ table.id eq id.toInt() }) { it[table.active] = active }
            }
            val entry = current.copy(active = active)

            insertAuditRow(audit(entry), now)
            ReferenceChangeOutcome.Changed(bumpReferenceDataVersion(now, null), entry)
        }

    private fun currentVersion(): Long =
        AppSettingsTable.selectAll()
            .where { AppSettingsTable.key eq AppSettingKeys.REFERENCE_DATA_VERSION }
            .singleOrNull()
            ?.get(AppSettingsTable.value)
            ?.toLongOrNull() ?: 1L

    private fun findEntry(kind: ReferenceKind, id: String): ReferenceEntryDto? =
        if (kind == ReferenceKind.COST_TYPES) {
            CostTypeTable.selectAll().where { CostTypeTable.code eq id }.singleOrNull()?.let {
                ReferenceEntryDto(kind.slug, id, it[CostTypeTable.displayName], it[CostTypeTable.active], it[CostTypeTable.sortOrder])
            }
        } else {
            val numeric = id.toIntOrNull()
            val table = namedTable(kind)
            if (numeric == null) null
            else table.table.selectAll().where { table.id eq numeric }.singleOrNull()?.let {
                ReferenceEntryDto(kind.slug, id, it[table.name], it[table.active])
            }
        }

    /* Diseases and treatment types have the same shape, so one code path serves both. */
    private class NamedTable(val table: Table, val id: Column<Int>, val name: Column<String>, val active: Column<Boolean>)

    private fun namedTable(kind: ReferenceKind): NamedTable =
        when (kind) {
            ReferenceKind.DISEASES -> NamedTable(DiseaseTable, DiseaseTable.id, DiseaseTable.name, DiseaseTable.active)
            ReferenceKind.TREATMENT_TYPES ->
                NamedTable(TreatmentTypeTable, TreatmentTypeTable.id, TreatmentTypeTable.name, TreatmentTypeTable.active)
            ReferenceKind.COST_TYPES -> error("Cost types are not a named table")
        }
}
