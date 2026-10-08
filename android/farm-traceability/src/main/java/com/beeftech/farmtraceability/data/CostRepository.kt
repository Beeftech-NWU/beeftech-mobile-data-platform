package com.beeftech.farmtraceability.data

import android.os.Build
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.repository.PendingSyncRepository

class CostRepository(
    private val animalCostDao: AnimalCostDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: CostApiClient,
    private val deviceIdProvider: () -> String = { Build.MODEL ?: "unknown-device" }
) {

    suspend fun saveCost(
        animalId: String,
        costType: String,
        amount: Double,
        description: String,
        gpsLat: Double,
        gpsLng: Double,
        timestamp: Long = System.currentTimeMillis(),
        submissionId: String = ""
    ): SaveCostOutcome {

        // The same draft submission ID is reused for retries, preventing duplicate charges.
        val recordGuid = submissionId.trim().ifBlank {
            java.util.UUID.randomUUID().toString()
        }
        val existing = animalCostDao.findByRecordGuid(recordGuid)
        if (existing != null) {
            return SaveCostOutcome(cost = existing, syncErrorMessage = null)
        }

        val cost =
            AnimalCost(
                animalId = animalId,
                costType = costType,
                amount = amount,
                description = description.trim(),
                gpsLat = gpsLat,
                gpsLng = gpsLng,
                timestamp = timestamp,
                recordGuid = recordGuid
            )

        /*
         * The local save must never be lost to a sync problem, so it
         * happens first and outside the sync error handling.
         */
        animalCostDao.insert(cost)

        pendingSyncRepository.queueOperation(
            entityType = ENTITY_TYPE,
            entityId = cost.recordGuid,
            operation = "CREATE",
            payload = cost.recordGuid
        )

        val outcome = syncPending()

        return SaveCostOutcome(
            cost = cost,
            syncErrorMessage = outcome.errorMessagesByRecordGuid[cost.recordGuid]
        )
    }

    /*
     * Uploads every unsynced cost for the signed-in user.
     *
     * Costs recorded before this app version could sync, and costs derived
     * from treatments, are PENDING in Room but were never queued. They are
     * queued here first, so the next sync picks them up. The signed-in user
     * becomes their owner.
     */
    suspend fun syncPending(): CostSyncPendingOutcome {

        return try {

            queueUnqueuedCosts()

            val pendingOperations =
                pendingSyncRepository
                    .getAllPendingOperations()
                    .filter { it.entityType == ENTITY_TYPE }

            if (pendingOperations.isEmpty()) {
                return CostSyncPendingOutcome(syncedCount = 0)
            }

            val pendingRecords =
                pendingOperations
                    .mapNotNull { animalCostDao.findByRecordGuid(it.entityId) }
                    .filter { it.syncStatus != SYNC_STATUS_SYNCED }

            if (pendingRecords.isEmpty()) {
                /* Already synced rows with a leftover queue item: just clear the queue. */
                pendingOperations.forEach { pendingSyncRepository.markSyncSuccessful(it.id) }
                return CostSyncPendingOutcome(syncedCount = 0)
            }

            apiClient
                .syncCosts(
                    records = pendingRecords,
                    deviceId = deviceIdProvider()
                )
                .fold(

                    onSuccess = { response ->

                        var syncedCount = 0

                        val errors = mutableMapOf<String, String?>()

                        val queuedByRecordGuid =
                            pendingOperations.groupBy { it.entityId }

                        response.results.forEach { result ->

                            val queued =
                                queuedByRecordGuid[result.recordguid].orEmpty()

                            if (result.status == SYNC_STATUS_SYNCED) {

                                animalCostDao.markSynced(
                                    recordGuid = result.recordguid,
                                    syncedAt = result.serverSyncedAt ?: System.currentTimeMillis()
                                )

                                queued.forEach {
                                    pendingSyncRepository.markSyncSuccessful(it.id)
                                }

                                syncedCount++

                            } else {

                                errors[result.recordguid] = result.message

                                queued
                                    .filter { it.retryCount < PendingSyncRepository.DEFAULT_MAX_RETRIES }
                                    .maxByOrNull { it.createdAt }
                                    ?.let { pendingSyncRepository.markSyncFailed(it.id) }
                            }
                        }

                        CostSyncPendingOutcome(
                            syncedCount = syncedCount,
                            errorMessagesByRecordGuid = errors
                        )
                    },

                    onFailure = { exception ->

                        val message = exception.message ?: "Unable to reach the server"

                        CostSyncPendingOutcome(
                            syncedCount = 0,
                            errorMessagesByRecordGuid =
                                pendingRecords.associate { it.recordGuid to message }
                        )
                    }
                )

        } catch (_: Exception) {

            CostSyncPendingOutcome(syncedCount = 0)
        }
    }

    private suspend fun queueUnqueuedCosts() {

        val queuedGuids =
            pendingSyncRepository
                .getAllPendingOperations()
                .filter { it.entityType == ENTITY_TYPE }
                .map { it.entityId }
                .toSet()

        animalCostDao
            .getUnsynced()
            .filter { it.recordGuid !in queuedGuids }
            .forEach {
                pendingSyncRepository.queueOperation(
                    entityType = ENTITY_TYPE,
                    entityId = it.recordGuid,
                    operation = "CREATE",
                    payload = it.recordGuid
                )
            }
    }

    companion object {

        const val ENTITY_TYPE =
            "ANIMAL_COST"

        const val SYNC_STATUS_SYNCED =
            "SYNCED"
    }
}

data class SaveCostOutcome(
    val cost: AnimalCost,
    val syncErrorMessage: String? = null
)

data class CostSyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByRecordGuid: Map<String, String?> = emptyMap()
)
