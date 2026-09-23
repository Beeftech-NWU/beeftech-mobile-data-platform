package com.beeftech.farmtraceability.data

import android.os.Build
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.repository.PendingSyncRepository

class AnimalMovementRepository(
    private val animalMovementDao: AnimalMovementDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: AnimalMovementApiClient
) {

    suspend fun loadMovements(
        animalId: String
    ): List<AnimalMovementEntity> {

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
             */
            val recentDuplicate =
                animalMovementDao.findRecentDuplicate(
                    animalId = normalizedAnimalId,
                    destinationFarmId = normalizedMovementInformation,
                    notes = normalizedResponsibleWorker
                )

            if (recentDuplicate != null) {

                val syncOutcome =
                    syncPending()

                val refreshedMovement =
                    animalMovementDao.findByMovementId(
                        recentDuplicate.movementId
                    ) ?: recentDuplicate

                return SaveAnimalMovementOutcome(
                    movement = refreshedMovement,
                    syncErrorMessage =
                        syncOutcome.errorMessagesByRecordGuid[
                            refreshedMovement.movementId
                        ],
                    duplicatePrevented = true
                )
            }

            val movement =
                AnimalMovementEntity(
                    animalId = normalizedAnimalId,
                    destinationFarmId = normalizedMovementInformation,
                    destinationPenId = "",
                    movementDate = now.toString(),
                    notes = normalizedResponsibleWorker
                )

            animalMovementDao.insert(
                movement
            )

            /*
             * Queue the movement for background synchronization.
             */
            pendingSyncRepository.queueOperation(
                entityType = ENTITY_TYPE,
                entityId = movement.movementId,
                operation = "CREATE",
                payload = movement.movementId
            )

            /*
             * Try immediately.
             */
            val syncOutcome =
                syncPending()

            val savedMovement =
                animalMovementDao.findByMovementId(
                    movement.movementId
                ) ?: movement

            SaveAnimalMovementOutcome(
                movement = savedMovement,
                syncErrorMessage =
                    syncOutcome.errorMessagesByRecordGuid[
                        movement.movementId
                    ],
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

            val pendingOperations =
                pendingSyncRepository
                    .getAllPendingOperations()
                    .filter { it.entityType == ENTITY_TYPE }

            if (pendingOperations.isEmpty()) {
                return AnimalMovementSyncPendingOutcome(
                    syncedCount = 0
                )
            }

            val pendingRecords =
                pendingOperations
                    .mapNotNull { operation ->
                        animalMovementDao.findByMovementId(operation.entityId)
                    }

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

                        val queuedByRecordGuid =
                            pendingOperations.groupBy { it.entityId }

                        response.results.forEach { syncResult ->

                            val queued =
                                queuedByRecordGuid[syncResult.recordguid].orEmpty()

                            if (syncResult.status == SYNC_STATUS_SYNCED) {

                                pendingSyncRepository.markEntitySyncSuccessful(
                                    entityType = ENTITY_TYPE,
                                    entityId = syncResult.recordguid
                                )

                                syncedCount++

                            } else {

                                errors[syncResult.recordguid] =
                                    syncResult.message

                                queued
                                    .filter {
                                        it.retryCount <
                                                PendingSyncRepository.DEFAULT_MAX_RETRIES
                                    }
                                    .maxByOrNull { it.createdAt }
                                    ?.let { pendingOperation ->

                                        pendingSyncRepository.markSyncFailed(
                                            pendingOperation.id
                                        )
                                    }
                            }
                        }

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
                                pendingRecords.associate { movement ->
                                    movement.movementId to message
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

    companion object {

        const val ENTITY_TYPE =
            "ANIMAL_MOVEMENT"

        const val SYNC_STATUS_SYNCED =
            "SYNCED"
    }
}

data class SaveAnimalMovementOutcome(
    val movement: AnimalMovementEntity?,
    val syncErrorMessage: String? = null,
    val duplicatePrevented: Boolean = false
)

data class AnimalMovementSyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByRecordGuid:
    Map<String, String?> = emptyMap()
)
