package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class MortalityRepository {

    /* Upserts by record GUID, so a retried sync never creates a duplicate. */
    suspend fun upsert(
        record: MortalitySyncRecord,
        submittedBy: String?,
        submitterSiteId: String?
    ): Long =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

            val serverSyncedAt = System.currentTimeMillis()

            val exists = MortalityTable
                .selectAll()
                .where { MortalityTable.recordguid eq record.recordguid }
                .any()

            if (!exists) {
                MortalityTable.insert {
                    it[animalId] = record.animalId
                    it[causeOfDeath] = record.causeOfDeath
                    it[necropsyCodeId] = record.necropsyCodeId
                    it[responsibleWorker] = record.responsibleWorker
                    it[notes] = record.notes
                    it[timestamp] = record.timestamp
                    it[deviceId] = record.deviceId
                    it[recordguid] = record.recordguid
                    it[syncStatus] = "SYNCED"
                    it[syncedAt] = serverSyncedAt
                    it[submittedByUserId] = submittedBy
                    it[siteId] = submitterSiteId
                }
            } else {
                MortalityTable.update({ MortalityTable.recordguid eq record.recordguid }) {
                    it[animalId] = record.animalId
                    it[causeOfDeath] = record.causeOfDeath
                    it[necropsyCodeId] = record.necropsyCodeId
                    it[responsibleWorker] = record.responsibleWorker
                    it[notes] = record.notes
                    it[timestamp] = record.timestamp
                    it[deviceId] = record.deviceId
                    it[syncStatus] = "SYNCED"
                    it[syncedAt] = serverSyncedAt
                    it[submittedByUserId] = submittedBy
                    it[siteId] = submitterSiteId
                }
            }

            serverSyncedAt
        }

    /* Newest first. Out-of-scope rows are filtered here, so callers never see them. */
    suspend fun list(scope: RecordScope): List<MortalityDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            MortalityTable
                .selectAll()
                .where { scope.predicate(MortalityTable.submittedByUserId, MortalityTable.siteId) }
                .orderBy(MortalityTable.timestamp, SortOrder.DESC)
                .map { it.toDto() }
        }

    suspend fun findByAnimalId(animalId: String, scope: RecordScope): List<MortalityDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            MortalityTable
                .selectAll()
                .where {
                    (MortalityTable.animalId eq animalId) and
                        scope.predicate(MortalityTable.submittedByUserId, MortalityTable.siteId)
                }
                .orderBy(MortalityTable.timestamp, SortOrder.DESC)
                .map { it.toDto() }
        }

    private fun ResultRow.toDto() = MortalityDto(
        animalId = this[MortalityTable.animalId],
        causeOfDeath = this[MortalityTable.causeOfDeath],
        necropsyCodeId = this[MortalityTable.necropsyCodeId],
        responsibleWorker = this[MortalityTable.responsibleWorker],
        notes = this[MortalityTable.notes],
        timestamp = this[MortalityTable.timestamp],
        deviceId = this[MortalityTable.deviceId],
        recordguid = this[MortalityTable.recordguid],
        syncStatus = this[MortalityTable.syncStatus],
        syncedAt = this[MortalityTable.syncedAt]
    )
}
