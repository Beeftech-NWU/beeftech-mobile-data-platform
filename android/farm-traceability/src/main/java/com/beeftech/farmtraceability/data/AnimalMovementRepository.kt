package com.beeftech.farmtraceability.data

import android.os.Build
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalMovement
import com.beeftech.database.repository.PendingSyncRepository
import java.util.UUID

class AnimalMovementRepository(
    private val animalMovementDao: AnimalMovementDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: AnimalMovementApiClient
) {

    suspend fun loadMovements(
        animalId: String
    ): List<AnimalMovement> {

        if (animalId.isBlank()) {
            return emptyList()
        }

        return try {
            animalMovementDao.getByAnimalId(animalId)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveMovement(
        animalId: String,
        movementInformation: String,
        responsibleWorker: String
    ): SaveAnimalMovementOutcome {

        return try {

            val normalizedAnimalId =
                animalId.trim()

            val normalizedMovementInformation =
                movementInformation.trim()

            val normalizedResponsibleWorker =
                responsibleWorker.trim()

            val now =
                System.currentTimeMillis()

            /*
             * Prevent accidental duplicate records caused by pressing
             * Add Movement Record followed immediately by Save Movement
             * Records, or by quickly double-tapping a save action.
             *
             * We deliberately use a short time window rather than making
             * animalId + movementType + worker globally unique. The same
             * legitimate movement can therefore be recorded again later.
             */
            val recentDuplicate =
                animalMovementDao.findRecentDuplicate(
                    animalId = normalizedAnimalId,
                    movementType = normalizedMovementInformation,
                    responsibleWorker = normalizedResponsibleWorker,
                    minimumTimestamp =
                        now - DUPLICATE_PROTECTION_WINDOW_MS
                )

            if (recentDuplicate != null) {

                /*
                 * Do not insert or queue a second copy.
                 *
                 * If the existing record is still pending, it is already
                 * represented by its original GUID and sync queue entry.
                 * We can still attempt synchronization again here.
                 */
                val syncOutcome =
                    if (
                        recentDuplicate.syncStatus !=
                        SYNC_STATUS_SYNCED
                    ) {
                        syncPending()
                    } else {
                        AnimalMovementSyncPendingOutcome(
                            syncedCount = 0
                        )
                    }

                val refreshedMovement =
                    animalMovementDao.findByRecordGuid(
                        recentDuplicate.recordguid
                    ) ?: recentDuplicate

                return SaveAnimalMovementOutcome(
                    movement = refreshedMovement,
                    syncErrorMessage =
                        if (
                            refreshedMovement.syncStatus ==
                            SYNC_STATUS_SYNCED
                        ) {
                            null
                        } else {
                            syncOutcome
                                .errorMessagesByRecordGuid[
                                refreshedMovement.recordguid
                            ]
                        },
                    duplicatePrevented = true
                )
            }

            val deviceId =
                Build.MODEL ?: "unknown-device"

            val movement =
                AnimalMovement(
                    animalId = normalizedAnimalId,
                    movementType =
                        normalizedMovementInformation,
                    responsibleWorker =
                        normalizedResponsibleWorker,
                    timestamp = now,

                    /*
                     * GPS will remain 0.0 until we connect actual
                     * device location capture.
                     */
                    gpsLat = 0.0,
                    gpsLng = 0.0,

                    deviceId = deviceId,

                    /*
                     * Immutable identifier used by the backend
                     * for idempotent synchronization.
                     */
                    recordguid =
                        UUID.randomUUID().toString(),

                    syncStatus =
                        SYNC_STATUS_PENDING,
                    syncedAt = null
                )

            animalMovementDao.insert(
                movement
            )

            /*
             * Queue the movement for background synchronization.
             */
            pendingSyncRepository.queueOperation(
                entityType = ENTITY_TYPE,
                entityId = movement.recordguid,
                operation = "CREATE",
                payload = movement.recordguid
            )

            /*
             * Try immediately.
             *
             * If the backend/internet is unavailable, the local
             * movement remains safely stored as PENDING.
             */
            val syncOutcome =
                syncPending()

            val savedMovement =
                animalMovementDao.findByRecordGuid(
                    movement.recordguid
                ) ?: movement

            SaveAnimalMovementOutcome(
                movement = savedMovement,
                syncErrorMessage =
                    if (
                        savedMovement.syncStatus ==
                        SYNC_STATUS_SYNCED
                    ) {
                        null
                    } else {
                        syncOutcome
                            .errorMessagesByRecordGuid[
                            movement.recordguid
                        ]
                    },
                duplicatePrevented = false
            )

        } catch (exception: Exception) {

            SaveAnimalMovementOutcome(
                movement = null,
                syncErrorMessage =
                    exception.message
                        ?: "Unable to save movement record.",
                duplicatePrevented = false
            )
        }
    }

    suspend fun syncPending():
            AnimalMovementSyncPendingOutcome {

        return try {

            val pendingRecords =
                animalMovementDao.getPendingSync()

            /*
             * Remove stale queue rows for records that have
             * already been marked SYNCED.
             */
            cleanupStaleQueueEntries()

            if (pendingRecords.isEmpty()) {

                return AnimalMovementSyncPendingOutcome(
                    syncedCount = 0
                )
            }

            val deviceId =
                Build.MODEL ?: "unknown-device"

            apiClient
                .syncMovements(
                    records = pendingRecords,
                    deviceId = deviceId
                )
                .fold(

                    onSuccess = { response ->

                        var syncedCount = 0

                        val errors =
                            mutableMapOf<String, String?>()

                        /*
                         * Include all queue entries so successful
                         * synchronization can clean up even records
                         * that previously reached the retry limit.
                         */
                        val queuedByRecordGuid =
                            pendingSyncRepository
                                .getAllPendingOperations()
                                .filter {
                                    it.entityType == ENTITY_TYPE
                                }
                                .groupBy {
                                    it.entityId
                                }

                        response.results.forEach {
                                syncResult ->

                            val queued =
                                queuedByRecordGuid[
                                    syncResult.recordguid
                                ].orEmpty()

                            if (
                                syncResult.status ==
                                SYNC_STATUS_SYNCED
                            ) {

                                animalMovementDao.markSynced(
                                    recordGuid =
                                        syncResult.recordguid,

                                    syncedAt =
                                        syncResult.serverSyncedAt
                                            ?: System.currentTimeMillis()
                                )

                                /*
                                 * Remove all queue entries for the
                                 * successfully synchronized movement.
                                 */
                                queued.forEach {
                                        pendingOperation ->

                                    pendingSyncRepository
                                        .markSyncSuccessful(
                                            pendingOperation.id
                                        )
                                }

                                syncedCount++

                            } else {

                                errors[
                                    syncResult.recordguid
                                ] =
                                    syncResult.message

                                /*
                                 * Increment the newest eligible queue
                                 * entry's retry count.
                                 */
                                queued
                                    .filter {
                                        it.retryCount <
                                                PendingSyncRepository
                                                    .DEFAULT_MAX_RETRIES
                                    }
                                    .maxByOrNull {
                                        it.createdAt
                                    }
                                    ?.let {
                                            pendingOperation ->

                                        pendingSyncRepository
                                            .markSyncFailed(
                                                pendingOperation.id
                                            )
                                    }

                                animalMovementDao.markPending(
                                    syncResult.recordguid
                                )
                            }
                        }

                        cleanupStaleQueueEntries()

                        AnimalMovementSyncPendingOutcome(
                            syncedCount = syncedCount,
                            errorMessagesByRecordGuid = errors
                        )
                    },

                    onFailure = { exception ->

                        val message =
                            exception.message
                                ?: "Unable to reach the server"

                        AnimalMovementSyncPendingOutcome(
                            syncedCount = 0,

                            errorMessagesByRecordGuid =
                                pendingRecords.associate {
                                        movement ->

                                    movement.recordguid to message
                                }
                        )
                    }
                )

        } catch (_: Exception) {

            AnimalMovementSyncPendingOutcome(
                syncedCount = 0
            )
        }
    }

    private suspend fun cleanupStaleQueueEntries() {

        val allMovements =
            animalMovementDao.getAll()

        val syncedRecordGuids =
            allMovements
                .filter {
                    it.syncStatus ==
                            SYNC_STATUS_SYNCED
                }
                .map {
                    it.recordguid
                }
                .toSet()

        if (syncedRecordGuids.isEmpty()) {
            return
        }

        pendingSyncRepository
            .getAllPendingOperations()
            .filter {
                it.entityType == ENTITY_TYPE &&
                        it.entityId in syncedRecordGuids
            }
            .forEach {
                    pendingOperation ->

                pendingSyncRepository
                    .markSyncSuccessful(
                        pendingOperation.id
                    )
            }
    }

    companion object {

        const val ENTITY_TYPE =
            "ANIMAL_MOVEMENT"

        const val SYNC_STATUS_PENDING =
            "PENDING"

        const val SYNC_STATUS_SYNCED =
            "SYNCED"

        /*
         * Long enough to cover Add -> Save and accidental double taps,
         * while still allowing a genuine identical movement later.
         */
        private const val DUPLICATE_PROTECTION_WINDOW_MS =
            30_000L
    }
}

data class SaveAnimalMovementOutcome(
    val movement: AnimalMovement?,
    val syncErrorMessage: String? = null,
    val duplicatePrevented: Boolean = false
)

data class AnimalMovementSyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByRecordGuid:
    Map<String, String?> = emptyMap()
)
