package com.beeftech.backend.api

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class AnimalMovementRepository {

    fun upsert(
        movement: AnimalMovementSyncRecord
    ): Long {

        return transaction {

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
                    }
            }

            serverSyncedAt
        }
    }
}