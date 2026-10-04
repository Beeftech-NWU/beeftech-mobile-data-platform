package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class AnimalMovementRepository {

    fun upsert(
        movement: AnimalMovementSyncRecord,
        submittedBy: String? = null,
        submitterSiteId: String? = null
    ): Long {

        return transaction(DatabaseFactory.getDatabase()) {

            val existing =
                AnimalMovementTable
                    .selectAll()
                    .where {
                        AnimalMovementTable.recordguid eq
                                movement.recordguid
                    }
                    .singleOrNull()

            val serverSyncedAt =
                System.currentTimeMillis()

            if (existing == null) {

                AnimalMovementTable
                    .insert {

                        it[animalId] =
                            movement.animalId

                        it[movementType] =
                            movement.movementType

                        it[responsibleWorker] =
                            movement.responsibleWorker

                        it[timestamp] =
                            movement.timestamp

                        it[gpsLat] =
                            movement.gpsLat

                        it[gpsLng] =
                            movement.gpsLng

                        it[deviceId] =
                            movement.deviceId

                        it[recordguid] =
                            movement.recordguid

                        it[syncStatus] =
                            "SYNCED"

                        it[syncedAt] =
                            serverSyncedAt

                        it[submittedByUserId] =
                            submittedBy

                        it[siteId] =
                            submitterSiteId
                    }

            } else {

                AnimalMovementTable
                    .update(
                        {
                            AnimalMovementTable.recordguid eq
                                    movement.recordguid
                        }
                    ) {

                        it[animalId] =
                            movement.animalId

                        it[movementType] =
                            movement.movementType

                        it[responsibleWorker] =
                            movement.responsibleWorker

                        it[timestamp] =
                            movement.timestamp

                        it[gpsLat] =
                            movement.gpsLat

                        it[gpsLng] =
                            movement.gpsLng

                        it[deviceId] =
                            movement.deviceId

                        it[syncStatus] =
                            "SYNCED"

                        it[syncedAt] =
                            serverSyncedAt

                        it[submittedByUserId] =
                            submittedBy

                        it[siteId] =
                            submitterSiteId
                    }
            }

            serverSyncedAt
        }
    }

    /* Newest first. Out-of-scope rows are filtered here, so callers never see them. */
    suspend fun list(scope: RecordScope): List<AnimalMovementDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            AnimalMovementTable
                .selectAll()
                .where { scope.predicate(AnimalMovementTable.submittedByUserId, AnimalMovementTable.siteId, AnimalMovementTable.voidedAt) }
                .orderBy(AnimalMovementTable.timestamp, SortOrder.DESC)
                .map { it.toDto() }
        }

    suspend fun findByAnimalId(animalId: String, scope: RecordScope): List<AnimalMovementDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            AnimalMovementTable
                .selectAll()
                .where {
                    (AnimalMovementTable.animalId eq animalId) and
                        scope.predicate(AnimalMovementTable.submittedByUserId, AnimalMovementTable.siteId, AnimalMovementTable.voidedAt)
                }
                .orderBy(AnimalMovementTable.timestamp, SortOrder.DESC)
                .map { it.toDto() }
        }

    private fun ResultRow.toDto() = AnimalMovementDto(
        animalId = this[AnimalMovementTable.animalId],
        movementType = this[AnimalMovementTable.movementType],
        responsibleWorker = this[AnimalMovementTable.responsibleWorker],
        timestamp = this[AnimalMovementTable.timestamp],
        gpsLat = this[AnimalMovementTable.gpsLat],
        gpsLng = this[AnimalMovementTable.gpsLng],
        deviceId = this[AnimalMovementTable.deviceId],
        recordguid = this[AnimalMovementTable.recordguid],
        syncStatus = this[AnimalMovementTable.syncStatus],
        syncedAt = this[AnimalMovementTable.syncedAt]
    )
}
