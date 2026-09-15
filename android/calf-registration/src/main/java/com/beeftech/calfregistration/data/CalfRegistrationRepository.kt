package com.beeftech.calfregistration.data

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.repository.PendingSyncRepository

/**
 * Coordinates local persistence (Room, via [calfRegistrationDao]) and
 * remote sync (via [apiClient]) for calf registrations, and exposes a
 * simple API consumed by [com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel].
 *
 * Design note: this uses [PendingSyncRepository] (rather than the lower
 * level `PendingSyncDao` directly) to queue/drain pending-sync bookkeeping,
 * since `PendingSyncRepository` already wraps exactly the operations
 * needed here (`queueOperation`, `markSyncSuccessful`, `markSyncFailed`)
 * with no extra ceremony - using the DAO directly would just mean
 * re-implementing that same thin wrapper here.
 */
class CalfRegistrationRepository(
    private val calfRegistrationDao: CalfRegistrationDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: CalfRegistrationApiClient
) {

    /** Loads every locally persisted calf registration, newest-first. */
    suspend fun loadAll(): List<CalfRegistrationData> {
        return try {
            calfRegistrationDao.getAll()
                .sortedByDescending { it.captureAt }
                .map { CalfRegistrationMappers.toFormData(it) }
        } catch (exception: Exception) {
            emptyList()
        }
    }

    /**
     * Persists [formData] locally, queues a pending-sync operation, and
     * makes a best-effort immediate attempt to sync it (and any other
     * pending records) to the backend. Returns the resulting
     * [CalfRegistrationData] with its `synced` flag reflecting the actual
     * outcome (i.e. it will be `true` only if the immediate sync attempt
     * succeeded).
     */
    suspend fun saveCalf(formData: CalfRegistrationData): CalfRegistrationData {
        return try {
            val existing = calfRegistrationDao.findByAnimalId(formData.tagNumber)

            val entity = CalfRegistrationMappers.toEntity(
                formData = formData,
                deviceId = android.os.Build.MODEL ?: "unknown-device",
                captureAt = System.currentTimeMillis(),
                existing = existing
            )

            // `upsert` (REPLACE on conflict) is used instead of `insert` so
            // that re-saving/editing an already-registered animalId updates
            // the existing row in place (preserving its recordguid) rather
            // than throwing a unique-constraint exception.
            calfRegistrationDao.upsert(entity)

            pendingSyncRepository.queueOperation(
                entityType = ENTITY_TYPE,
                entityId = entity.animalId,
                operation = if (existing == null) "CREATE" else "UPDATE",
                payload = entity.animalId
            )

            syncPending()

            val saved = calfRegistrationDao.findByAnimalId(entity.animalId)
            saved?.let { CalfRegistrationMappers.toFormData(it) } ?: formData
        } catch (exception: Exception) {
            formData.copy(synced = false)
        }
    }

    /**
     * Attempts to sync every locally persisted record whose `syncStatus`
     * is not already [SYNC_STATUS_SYNCED]. On a total network/parsing
     * failure, all records are left as-is (`PENDING`) for a later retry.
     *
     * Returns the number of records that were successfully synced.
     */
    suspend fun syncPending(): Int {
        return try {
            val pendingRecords = calfRegistrationDao.getAll()
                .filter { it.syncStatus != SYNC_STATUS_SYNCED }

            if (pendingRecords.isEmpty()) {
                return 0
            }

            val deviceId = android.os.Build.MODEL ?: "unknown-device"
            val result = apiClient.syncCalves(pendingRecords, deviceId)

            result.fold(
                onSuccess = { response ->
                    var syncedCount = 0

                    response.results.forEach { syncResult ->
                        if (syncResult.status == "SYNCED") {
                            calfRegistrationDao.updateSyncStatus(
                                animalId = syncResult.animalId,
                                syncStatus = SYNC_STATUS_SYNCED,
                                syncedAt = syncResult.serverSyncedAt
                                    ?: System.currentTimeMillis()
                            )
                            syncedCount++
                        }
                    }

                    syncedCount
                },
                onFailure = { 0 }
            )
        } catch (exception: Exception) {
            0
        }
    }

    companion object {
        private const val ENTITY_TYPE = "CALF_REGISTRATION"
    }
}
