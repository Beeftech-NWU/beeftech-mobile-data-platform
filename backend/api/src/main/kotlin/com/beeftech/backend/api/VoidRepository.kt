package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * A record type that can be voided. [slug] is the path segment in
 * `POST /api/records/{slug}/{id}/void` and [idColumn] holds the id in that path.
 */
class VoidTarget(
    val slug: String,
    val entityType: String,
    val table: Table,
    val idColumn: Column<String>,
    val siteId: Column<String?>,
    val voidedAt: Column<Long?>,
    val voidedByUserId: Column<String?>,
    val voidReason: Column<String?>
)

val VOID_TARGETS: List<VoidTarget> = listOf(
    VoidTarget(
        "calf-registrations", "CALF_REGISTRATION", CalfRegistrationTable, CalfRegistrationTable.recordguid,
        CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt,
        CalfRegistrationTable.voidedByUserId, CalfRegistrationTable.voidReason
    ),
    VoidTarget(
        "treatments", "TREATMENT", TreatmentTable, TreatmentTable.recordguid,
        TreatmentTable.siteId, TreatmentTable.voidedAt,
        TreatmentTable.voidedByUserId, TreatmentTable.voidReason
    ),
    VoidTarget(
        "farmers", "FARMER_REGISTRATION", FarmerTable, FarmerTable.farmerId,
        FarmerTable.siteId, FarmerTable.voidedAt,
        FarmerTable.voidedByUserId, FarmerTable.voidReason
    ),
    VoidTarget(
        "animal-movements", "ANIMAL_MOVEMENT", AnimalMovementTable, AnimalMovementTable.recordguid,
        AnimalMovementTable.siteId, AnimalMovementTable.voidedAt,
        AnimalMovementTable.voidedByUserId, AnimalMovementTable.voidReason
    ),
    VoidTarget(
        "mortalities", "MORTALITY", MortalityTable, MortalityTable.recordguid,
        MortalityTable.siteId, MortalityTable.voidedAt,
        MortalityTable.voidedByUserId, MortalityTable.voidReason
    )
)

sealed interface VoidOutcome {
    data class Voided(val voidedAt: Long) : VoidOutcome
    data object NotFound : VoidOutcome
    data object AlreadyVoided : VoidOutcome
}

class VoidRepository {

    /*
     * Marks the record voided and writes the audit row in one transaction, so a void
     * is never recorded without its audit entry or the other way round. Nothing is
     * deleted. A manager's [siteScope] is their own site; null means any site (admin).
     * A record outside the scope is reported as NotFound, like out-of-scope reads.
     */
    suspend fun void(
        target: VoidTarget,
        id: String,
        reason: String,
        actorUserId: String,
        actorUsername: String,
        actorRole: Int,
        siteScope: SiteScope
    ): VoidOutcome =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

            val row = target.table
                .select(target.siteId, target.voidedAt)
                .where { target.idColumn eq id }
                .singleOrNull()
                ?: return@newSuspendedTransaction VoidOutcome.NotFound

            val recordSite = row[target.siteId]

            if (siteScope is SiteScope.Only && (recordSite == null || recordSite != siteScope.siteId)) {
                return@newSuspendedTransaction VoidOutcome.NotFound
            }

            if (row[target.voidedAt] != null) {
                return@newSuspendedTransaction VoidOutcome.AlreadyVoided
            }

            val now = System.currentTimeMillis()

            target.table.update({ target.idColumn eq id }) {
                it[target.voidedAt] = now
                it[target.voidedByUserId] = actorUserId
                it[target.voidReason] = reason
            }

            AuditLogTable.insert {
                it[action] = "VOID"
                it[entityType] = target.entityType
                it[entityId] = id
                it[AuditLogTable.reason] = reason
                it[AuditLogTable.actorUserId] = actorUserId
                it[AuditLogTable.actorUsername] = actorUsername
                it[AuditLogTable.actorRole] = actorRole
                it[siteId] = recordSite
                it[createdAt] = now
            }

            VoidOutcome.Voided(now)
        }

    /* Newest first. A manager only gets entries about their own site. */
    suspend fun auditLog(siteScope: SiteScope, limit: Int): List<AuditLogEntryDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            AuditLogTable
                .selectAll()
                .apply {
                    if (siteScope is SiteScope.Only) {
                        where { AuditLogTable.siteId eq siteScope.siteId }
                    }
                }
                .orderBy(AuditLogTable.createdAt, SortOrder.DESC)
                .orderBy(AuditLogTable.id, SortOrder.DESC)
                .limit(limit)
                .map {
                    AuditLogEntryDto(
                        id = it[AuditLogTable.id],
                        action = it[AuditLogTable.action],
                        entityType = it[AuditLogTable.entityType],
                        entityId = it[AuditLogTable.entityId],
                        reason = it[AuditLogTable.reason],
                        actorUserId = it[AuditLogTable.actorUserId],
                        actorUsername = it[AuditLogTable.actorUsername],
                        actorRole = it[AuditLogTable.actorRole],
                        siteId = it[AuditLogTable.siteId],
                        createdAt = it[AuditLogTable.createdAt]
                    )
                }
        }
}

/* Which sites a manager or admin may act on. */
sealed interface SiteScope {
    data object Any : SiteScope
    data class Only(val siteId: String) : SiteScope
}
