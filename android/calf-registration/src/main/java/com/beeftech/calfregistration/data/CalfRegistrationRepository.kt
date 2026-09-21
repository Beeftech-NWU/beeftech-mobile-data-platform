package com.beeftech.calfregistration.data

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.entity.CalfRegistration
import com.beeftech.database.repository.PendingSyncRepository

class CalfRegistrationRepository(
    private val calfRegistrationDao: CalfRegistrationDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: CalfRegistrationApiClient
) {
    suspend fun loadAll(): List<CalfRegistrationData> =
        try {
            calfRegistrationDao.getAll()
                .sortedByDescending { it.captureAt }
                .map { CalfRegistrationMappers.toFormData(it) }
        } catch (_: Exception) {
            emptyList()
        }

    suspend fun saveCalf(
        formData: CalfRegistrationData
    ): SaveCalfOutcome {
        return try {
            val existing =
                calfRegistrationDao.findByAnimalId(formData.tagNumber)

            val entity =
                CalfRegistrationMappers.toEntity(
                    formData = formData,
                    deviceId = android.os.Build.MODEL ?: "unknown-device",
                    captureAt = System.currentTimeMillis(),
                    existing = existing
                )

            calfRegistrationDao.upsert(entity)

            pendingSyncRepository.queueOperation(
                entityType = ENTITY_TYPE,
                entityId = entity.animalId,
                operation = if (existing == null) "CREATE" else "UPDATE",
                payload = entity.animalId
            )

            val syncOutcome = syncPending()
            val saved =
                calfRegistrationDao.findByAnimalId(entity.animalId)

            val formResult =
                saved?.let { CalfRegistrationMappers.toFormData(it) }
                    ?: formData

            SaveCalfOutcome(
                data = formResult,
                syncErrorMessage =
                    if (formResult.synced) null
                    else syncOutcome.errorMessagesByAnimalId[entity.animalId]
            )
        } catch (exception: Exception) {
            SaveCalfOutcome(
                data = formData.copy(synced = false),
                syncErrorMessage = exception.message
            )
        }
    }

    suspend fun syncPending(): SyncPendingOutcome {
        return try {
            val allCalves = calfRegistrationDao.getAll()
            val pendingRecords =
                allCalves.filter {
                    it.syncStatus != SYNC_STATUS_SYNCED
                }

            // Remove queue entries for calves already marked SYNCED.
            cleanupStaleCalfQueueEntries(allCalves)

            if (pendingRecords.isEmpty()) {
                return SyncPendingOutcome(syncedCount = 0)
            }

            val deviceId =
                android.os.Build.MODEL ?: "unknown-device"

            apiClient.syncCalves(pendingRecords, deviceId).fold(
                onSuccess = { response ->
                    var syncedCount = 0
                    val errors =
                        mutableMapOf<String, String?>()

                    // Use every queue entry, including max-retry entries.
                    val queuedByAnimal =
                        pendingSyncRepository
                            .getAllPendingOperations()
                            .filter { it.entityType == ENTITY_TYPE }
                            .groupBy { it.entityId }

                    response.results.forEach { syncResult ->
                        val queued =
                            queuedByAnimal[syncResult.animalId]
                                .orEmpty()

                        if (syncResult.status == SYNC_STATUS_SYNCED) {
                            calfRegistrationDao.updateSyncStatus(
                                animalId = syncResult.animalId,
                                syncStatus = SYNC_STATUS_SYNCED,
                                syncedAt =
                                    syncResult.serverSyncedAt
                                        ?: System.currentTimeMillis()
                            )

                            // Delete ALL queue rows for this synced calf.
                            queued.forEach {
                                pendingSyncRepository
                                    .markSyncSuccessful(it.id)
                            }

                            syncedCount++
                        } else {
                            errors[syncResult.animalId] =
                                syncResult.message

                            queued
                                .filter {
                                    it.retryCount <
                                            PendingSyncRepository.DEFAULT_MAX_RETRIES
                                }
                                .maxByOrNull { it.createdAt }
                                ?.let {
                                    pendingSyncRepository
                                        .markSyncFailed(it.id)
                                }
                        }
                    }

                    // Final reconciliation after updating calf statuses.
                    cleanupStaleCalfQueueEntries(
                        calfRegistrationDao.getAll()
                    )

                    SyncPendingOutcome(
                        syncedCount = syncedCount,
                        errorMessagesByAnimalId = errors
                    )
                },
                onFailure = { exception ->
                    val message =
                        exception.message
                            ?: "Unable to reach the server"

                    SyncPendingOutcome(
                        syncedCount = 0,
                        errorMessagesByAnimalId =
                            pendingRecords.associate {
                                it.animalId to message
                            }
                    )
                }
            )
        } catch (_: Exception) {
            SyncPendingOutcome(syncedCount = 0)
        }
    }

    private suspend fun cleanupStaleCalfQueueEntries(
        calves: List<CalfRegistration>
    ) {
        val syncedAnimalIds =
            calves
                .filter { it.syncStatus == SYNC_STATUS_SYNCED }
                .map { it.animalId }
                .toSet()

        if (syncedAnimalIds.isEmpty()) return

        pendingSyncRepository
            .getAllPendingOperations()
            .filter {
                it.entityType == ENTITY_TYPE &&
                        it.entityId in syncedAnimalIds
            }
            .forEach {
                pendingSyncRepository.markSyncSuccessful(it.id)
            }
    }

    companion object {
        private const val ENTITY_TYPE = "CALF_REGISTRATION"
    }
}

data class SaveCalfOutcome(
    val data: CalfRegistrationData,
    val syncErrorMessage: String? = null
)

data class SyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByAnimalId: Map<String, String?> = emptyMap()
)
