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
 *
 * The `calf_registrations.syncStatus` column (not the queue) is the source
 * of truth for *what* still needs syncing - it's what [syncPending] scans -
 * while the [PendingSyncRepository] queue is consumed alongside it purely
 * for retry-count bookkeeping, so the two don't drift out of sync with
 * each other.
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

    /** Checks whether a calf registration with the given [tagNumber] (animalId) already exists locally. */
    suspend fun isTagRegistered(tagNumber: String): Boolean {
        if (tagNumber.isBlank()) return false
        return try {
            calfRegistrationDao.existsByAnimalId(tagNumber.trim())
        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Persists [formData] locally, queues a pending-sync operation, and
     * makes a best-effort immediate attempt to sync it (and any other
     * pending records) to the backend.
     *
     * The returned [SaveCalfOutcome.data] reflects the actual outcome via
     * its `synced` flag (i.e. it will be `true` only if the immediate sync
     * attempt succeeded); when it's `false`, [SaveCalfOutcome.syncErrorMessage]
     * carries a human-readable reason - either the specific per-record error
     * the backend returned for this `animalId`, or the underlying
     * exception/HTTP-failure message when the whole sync attempt couldn't
     * even be made (e.g. no network).
     */
    suspend fun saveCalf(formData: CalfRegistrationData): SaveCalfOutcome {
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

            val syncOutcome = syncPending()

            val saved = calfRegistrationDao.findByAnimalId(entity.animalId)
            val formResult = saved?.let { CalfRegistrationMappers.toFormData(it) } ?: formData

            SaveCalfOutcome(
                data = formResult,
                syncErrorMessage = if (formResult.synced) {
                    null
                } else {
                    syncOutcome.errorMessagesByAnimalId[entity.animalId]
                }
            )
        } catch (exception: Exception) {
            SaveCalfOutcome(
                data = formData.copy(synced = false),
                syncErrorMessage = exception.message
            )
        }
    }

    /**
     * Attempts to sync every locally persisted record whose `syncStatus`
     * is not already [SYNC_STATUS_SYNCED]. On a total network/parsing
     * failure, all records are left as-is (`PENDING`) for a later retry,
     * and [SyncPendingOutcome.errorMessagesByAnimalId] is populated with
     * the same underlying failure message for every one of them.
     *
     * Also consumes/updates the matching [PendingSyncRepository] queue
     * entries created by [saveCalf] (marking them successful on a SYNCED
     * result, or bumping their retry count otherwise), so the queue and the
     * `calf_registrations.syncStatus` column stay consistent with each
     * other rather than drifting apart.
     */
    suspend fun syncPending(): SyncPendingOutcome {
        return try {
            val pendingRecords = calfRegistrationDao.getAll()
                .filter { it.syncStatus != SYNC_STATUS_SYNCED }

            if (pendingRecords.isEmpty()) {
                return SyncPendingOutcome(syncedCount = 0)
            }

            val deviceId = android.os.Build.MODEL ?: "unknown-device"
            val result = apiClient.syncCalves(pendingRecords, deviceId)

            result.fold(
                onSuccess = { response ->
                    var syncedCount = 0
                    val errorMessagesByAnimalId = mutableMapOf<String, String?>()

                    val queuedOperationsByAnimalId = pendingSyncRepository
                        .getPendingOperations()
                        .filter { it.entityType == ENTITY_TYPE }
                        .groupBy { it.entityId }

                    response.results.forEach { syncResult ->
                        val queuedOperation = queuedOperationsByAnimalId[syncResult.animalId]
                            ?.maxByOrNull { it.createdAt }

                        if (syncResult.status == "SYNCED") {
                            calfRegistrationDao.updateSyncStatus(
                                animalId = syncResult.animalId,
                                syncStatus = SYNC_STATUS_SYNCED,
                                syncedAt = syncResult.serverSyncedAt
                                    ?: System.currentTimeMillis()
                            )

                            queuedOperation?.let {
                                pendingSyncRepository.markSyncSuccessful(it.id)
                            }

                            syncedCount++
                        } else {
                            errorMessagesByAnimalId[syncResult.animalId] = syncResult.message

                            queuedOperation?.let {
                                pendingSyncRepository.markSyncFailed(it.id)
                            }
                        }
                    }

                    SyncPendingOutcome(syncedCount, errorMessagesByAnimalId)
                },
                onFailure = { exception ->
                    // The whole request failed (e.g. no network, or the
                    // server was unreachable) rather than any individual
                    // record being rejected - every pending record shares
                    // the same underlying reason.
                    val message = exception.message ?: "Unable to reach the server"

                    SyncPendingOutcome(
                        syncedCount = 0,
                        errorMessagesByAnimalId = pendingRecords.associate { it.animalId to message }
                    )
                }
            )
        } catch (exception: Exception) {
            SyncPendingOutcome(syncedCount = 0)
        }
    }

    companion object {
        private const val ENTITY_TYPE = "CALF_REGISTRATION"
    }
}

/**
 * Result of [CalfRegistrationRepository.saveCalf].
 *
 * @property data The persisted (and re-loaded) form data; its `synced`
 * flag is the primary success/failure signal.
 * @property syncErrorMessage A human-readable reason the immediate sync
 * attempt didn't succeed, or `null` when it did (or when there's nothing
 * more specific than "will retry later" to say).
 */
data class SaveCalfOutcome(
    val data: CalfRegistrationData,
    val syncErrorMessage: String? = null
)

/**
 * Result of [CalfRegistrationRepository.syncPending].
 *
 * @property syncedCount How many pending records were successfully synced.
 * @property errorMessagesByAnimalId Per-`animalId` reason a record was
 * *not* synced (only contains entries for records that failed).
 */
data class SyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByAnimalId: Map<String, String?> = emptyMap()
)
