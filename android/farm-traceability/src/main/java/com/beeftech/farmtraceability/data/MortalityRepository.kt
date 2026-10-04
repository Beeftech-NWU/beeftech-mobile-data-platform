package com.beeftech.farmtraceability.data

import android.os.Build
import com.beeftech.database.dao.MortalityDao
import com.beeftech.database.entity.Mortality
import com.beeftech.database.repository.PendingSyncRepository

class MortalityRepository(
    private val mortalityDao: MortalityDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: MortalityApiClient,
    private val deviceIdProvider: () -> String = { Build.MODEL ?: "unknown-device" }
) {

    suspend fun loadMortalities(
        animalId: String
    ): List<Mortality> {

        if (animalId.isBlank()) {
            return emptyList()
        }

        return try {
            mortalityDao.getByAnimalId(animalId)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveMortality(
        animalId: String,
        causeOfDeath: String,
        responsibleWorker: String
    ): SaveMortalityOutcome {

        val mortality =
            Mortality(
                animalId = animalId.trim(),
                causeOfDeath = causeOfDeath.trim(),
                responsibleWorker = responsibleWorker.trim(),
                notes = null,
                timestamp = System.currentTimeMillis()
            )

        /*
         * The local save must never be lost to a sync problem, so it
         * happens first and outside the sync error handling.
         */
        mortalityDao.insert(mortality)

        pendingSyncRepository.queueOperation(
            entityType = ENTITY_TYPE,
            entityId = mortality.recordGuid,
            operation = "CREATE",
            payload = mortality.recordGuid
        )

        val outcome = syncPending()

        return SaveMortalityOutcome(
            mortality = mortality,
            syncErrorMessage = outcome.errorMessagesByRecordGuid[mortality.recordGuid]
        )
    }

    /*
     * Uploads every unsynced mortality for the signed-in user.
     *
     * Mortalities recorded before this app version could sync are still
     * PENDING in Room but were never queued. They are queued here first, so
     * the next sync picks them up. The signed-in user becomes their owner.
     */
    suspend fun syncPending(): MortalitySyncPendingOutcome {

        return try {

            queueUnqueuedMortalities()

            val pendingOperations =
                pendingSyncRepository
                    .getAllPendingOperations()
                    .filter { it.entityType == ENTITY_TYPE }

            if (pendingOperations.isEmpty()) {
                return MortalitySyncPendingOutcome(syncedCount = 0)
            }

            val pendingRecords =
                pendingOperations
                    .mapNotNull { mortalityDao.findByRecordGuid(it.entityId) }
                    .filter { it.syncStatus != SYNC_STATUS_SYNCED }

            if (pendingRecords.isEmpty()) {
                /* Already synced rows with a leftover queue item: just clear the queue. */
                pendingOperations.forEach { pendingSyncRepository.markSyncSuccessful(it.id) }
                return MortalitySyncPendingOutcome(syncedCount = 0)
            }

            apiClient
                .syncMortalities(
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

                                mortalityDao.markSynced(
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

                        MortalitySyncPendingOutcome(
                            syncedCount = syncedCount,
                            errorMessagesByRecordGuid = errors
                        )
                    },

                    onFailure = { exception ->

                        val message = exception.message ?: "Unable to reach the server"

                        MortalitySyncPendingOutcome(
                            syncedCount = 0,
                            errorMessagesByRecordGuid =
                                pendingRecords.associate { it.recordGuid to message }
                        )
                    }
                )

        } catch (_: Exception) {

            MortalitySyncPendingOutcome(syncedCount = 0)
        }
    }

    private suspend fun queueUnqueuedMortalities() {

        val queuedGuids =
            pendingSyncRepository
                .getAllPendingOperations()
                .filter { it.entityType == ENTITY_TYPE }
                .map { it.entityId }
                .toSet()

        mortalityDao
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
            "MORTALITY"

        const val SYNC_STATUS_SYNCED =
            "SYNCED"
    }
}

data class SaveMortalityOutcome(
    val mortality: Mortality,
    val syncErrorMessage: String? = null
)

data class MortalitySyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByRecordGuid: Map<String, String?> = emptyMap()
)
