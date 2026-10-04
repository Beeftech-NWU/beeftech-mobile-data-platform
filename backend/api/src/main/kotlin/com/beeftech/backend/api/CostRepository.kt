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

class CostRepository {

    /* Upserts by record GUID, so a retried sync never creates a duplicate. */
    suspend fun upsert(
        record: CostSyncRecord,
        submittedBy: String?,
        submitterSiteId: String?
    ): Long =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

            val serverSyncedAt = System.currentTimeMillis()

            val exists = CostTable
                .selectAll()
                .where { CostTable.recordguid eq record.recordguid }
                .any()

            if (!exists) {
                CostTable.insert {
                    it[animalId] = record.animalId
                    it[costType] = record.costType
                    it[amount] = record.amount
                    it[description] = record.description
                    it[gpsLat] = record.gpsLat
                    it[gpsLng] = record.gpsLng
                    it[timestamp] = record.timestamp
                    it[sourceEntity] = record.sourceEntity
                    it[sourceRecordId] = record.sourceRecordId
                    it[deviceId] = record.deviceId
                    it[recordguid] = record.recordguid
                    it[syncStatus] = "SYNCED"
                    it[syncedAt] = serverSyncedAt
                    it[submittedByUserId] = submittedBy
                    it[siteId] = submitterSiteId
                }
            } else {
                CostTable.update({ CostTable.recordguid eq record.recordguid }) {
                    it[animalId] = record.animalId
                    it[costType] = record.costType
                    it[amount] = record.amount
                    it[description] = record.description
                    it[gpsLat] = record.gpsLat
                    it[gpsLng] = record.gpsLng
                    it[timestamp] = record.timestamp
                    it[sourceEntity] = record.sourceEntity
                    it[sourceRecordId] = record.sourceRecordId
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
    suspend fun list(scope: RecordScope): List<CostDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            CostTable
                .selectAll()
                .where { scope.predicate(CostTable.submittedByUserId, CostTable.siteId) }
                .orderBy(CostTable.timestamp, SortOrder.DESC)
                .map { it.toDto() }
        }

    suspend fun findByAnimalId(animalId: String, scope: RecordScope): List<CostDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            CostTable
                .selectAll()
                .where {
                    (CostTable.animalId eq animalId) and
                        scope.predicate(CostTable.submittedByUserId, CostTable.siteId)
                }
                .orderBy(CostTable.timestamp, SortOrder.DESC)
                .map { it.toDto() }
        }

    private fun ResultRow.toDto() = CostDto(
        animalId = this[CostTable.animalId],
        costType = this[CostTable.costType],
        amount = this[CostTable.amount],
        description = this[CostTable.description],
        gpsLat = this[CostTable.gpsLat],
        gpsLng = this[CostTable.gpsLng],
        timestamp = this[CostTable.timestamp],
        sourceEntity = this[CostTable.sourceEntity],
        sourceRecordId = this[CostTable.sourceRecordId],
        deviceId = this[CostTable.deviceId],
        recordguid = this[CostTable.recordguid],
        syncStatus = this[CostTable.syncStatus],
        syncedAt = this[CostTable.syncedAt]
    )
}
