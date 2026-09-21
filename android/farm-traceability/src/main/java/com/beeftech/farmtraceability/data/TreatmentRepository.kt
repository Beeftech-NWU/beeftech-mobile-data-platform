package com.beeftech.farmtraceability.data

import android.os.Build
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.entity.Treatment
import com.beeftech.database.repository.PendingSyncRepository
import java.util.UUID

class TreatmentRepository(
    private val treatmentDao: TreatmentDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: TreatmentApiClient
) {

    suspend fun loadReferenceData():
            Result<TreatmentReferenceDataDto> {

        return apiClient.getReferenceData()
    }

    suspend fun loadTreatments(
        animalId: String
    ): List<Treatment> {

        if (animalId.isBlank()) {
            return emptyList()
        }

        return try {
            treatmentDao.getByAnimalId(
                animalId
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveTreatment(
        animalId: String,
        disease: String,
        treatmentName: String,
        batchNumber: String,
        volumeUsed: String,
        cost: Double
    ): SaveTreatmentOutcome {

        return try {

            val normalizedAnimalId =
                animalId.trim()

            val normalizedDisease =
                disease.trim()

            val normalizedTreatmentName =
                treatmentName.trim()

            val normalizedBatchNumber =
                batchNumber.trim()

            val normalizedVolumeUsed =
                volumeUsed.trim()

            val now =
                System.currentTimeMillis()

            /*
             * Prevent an immediate double tap / duplicate save.
             */
            val recentDuplicate =
                treatmentDao.findRecentDuplicate(
                    animalId =
                        normalizedAnimalId,

                    disease =
                        normalizedDisease,

                    treatmentName =
                        normalizedTreatmentName,

                    batchNumber =
                        normalizedBatchNumber,

                    volumeUsed =
                        normalizedVolumeUsed,

                    cost =
                        cost,

                    minimumTimestamp =
                        now -
                                DUPLICATE_PROTECTION_WINDOW_MS
                )

            if (recentDuplicate != null) {

                val syncOutcome =
                    if (
                        recentDuplicate.syncStatus !=
                        SYNC_STATUS_SYNCED
                    ) {
                        syncPending()
                    } else {
                        TreatmentSyncPendingOutcome(
                            syncedCount = 0
                        )
                    }

                val refreshed =
                    treatmentDao.findByRecordGuid(
                        recentDuplicate.recordguid
                    ) ?: recentDuplicate

                return SaveTreatmentOutcome(
                    treatment = refreshed,

                    syncErrorMessage =
                        if (
                            refreshed.syncStatus ==
                            SYNC_STATUS_SYNCED
                        ) {
                            null
                        } else {
                            syncOutcome
                                .errorMessagesByRecordGuid[
                                refreshed.recordguid
                            ]
                        },

                    duplicatePrevented = true
                )
            }

            val deviceId =
                Build.MODEL
                    ?: "unknown-device"

            val treatment =
                Treatment(
                    animalId =
                        normalizedAnimalId,

                    disease =
                        normalizedDisease,

                    treatmentName =
                        normalizedTreatmentName,

                    batchNumber =
                        normalizedBatchNumber,

                    volumeUsed =
                        normalizedVolumeUsed,

                    cost =
                        cost,

                    timestamp =
                        now,

                    deviceId =
                        deviceId,

                    recordguid =
                        UUID.randomUUID()
                            .toString(),

                    syncStatus =
                        SYNC_STATUS_PENDING,

                    syncedAt =
                        null
                )

            treatmentDao.insert(
                treatment
            )

            /*
             * Queue exactly this immutable GUID.
             */
            pendingSyncRepository.queueOperation(
                entityType =
                    ENTITY_TYPE,

                entityId =
                    treatment.recordguid,

                operation =
                    "CREATE",

                payload =
                    treatment.recordguid
            )

            /*
             * Try synchronization immediately.
             *
             * If offline, the local Treatment remains PENDING.
             */
            val syncOutcome =
                syncPending()

            val savedTreatment =
                treatmentDao.findByRecordGuid(
                    treatment.recordguid
                ) ?: treatment

            SaveTreatmentOutcome(
                treatment =
                    savedTreatment,

                syncErrorMessage =
                    if (
                        savedTreatment.syncStatus ==
                        SYNC_STATUS_SYNCED
                    ) {
                        null
                    } else {
                        syncOutcome
                            .errorMessagesByRecordGuid[
                            treatment.recordguid
                        ]
                    },

                duplicatePrevented =
                    false
            )

        } catch (exception: Exception) {

            SaveTreatmentOutcome(
                treatment = null,

                syncErrorMessage =
                    exception.message
                        ?: "Unable to save treatment record.",

                duplicatePrevented =
                    false
            )
        }
    }

    suspend fun syncPending():
            TreatmentSyncPendingOutcome {

        return try {

            val pendingRecords =
                treatmentDao.getPendingSync()

            cleanupStaleQueueEntries()

            if (pendingRecords.isEmpty()) {

                return TreatmentSyncPendingOutcome(
                    syncedCount = 0
                )
            }

            val deviceId =
                Build.MODEL
                    ?: "unknown-device"

            apiClient
                .syncTreatments(
                    records =
                        pendingRecords,

                    deviceId =
                        deviceId
                )
                .fold(

                    onSuccess = { response ->

                        var syncedCount = 0

                        val errors =
                            mutableMapOf<
                                    String,
                                    String?
                                    >()

                        val queuedByRecordGuid =
                            pendingSyncRepository
                                .getAllPendingOperations()
                                .filter {
                                    it.entityType ==
                                            ENTITY_TYPE
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

                                treatmentDao.markSynced(
                                    recordGuid =
                                        syncResult.recordguid,

                                    syncedAt =
                                        syncResult.serverSyncedAt
                                            ?: System.currentTimeMillis()
                                )

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

                                treatmentDao.markPending(
                                    syncResult.recordguid
                                )
                            }
                        }

                        cleanupStaleQueueEntries()

                        TreatmentSyncPendingOutcome(
                            syncedCount =
                                syncedCount,

                            errorMessagesByRecordGuid =
                                errors
                        )
                    },

                    onFailure = { exception ->

                        val message =
                            exception.message
                                ?: "Unable to reach the server"

                        TreatmentSyncPendingOutcome(
                            syncedCount = 0,

                            errorMessagesByRecordGuid =
                                pendingRecords.associate {
                                        treatment ->

                                    treatment.recordguid to
                                            message
                                }
                        )
                    }
                )

        } catch (_: Exception) {

            TreatmentSyncPendingOutcome(
                syncedCount = 0
            )
        }
    }

    private suspend fun cleanupStaleQueueEntries() {

        val allTreatments =
            treatmentDao.getAll()

        val syncedRecordGuids =
            allTreatments
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
                it.entityType ==
                        ENTITY_TYPE &&
                        it.entityId in
                        syncedRecordGuids
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
            "TREATMENT"

        const val SYNC_STATUS_PENDING =
            "PENDING"

        const val SYNC_STATUS_SYNCED =
            "SYNCED"

        private const val
                DUPLICATE_PROTECTION_WINDOW_MS =
            30_000L
    }
}

data class SaveTreatmentOutcome(
    val treatment: Treatment?,
    val syncErrorMessage: String? = null,
    val duplicatePrevented: Boolean = false
)

data class TreatmentSyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByRecordGuid:
    Map<String, String?> = emptyMap()
)