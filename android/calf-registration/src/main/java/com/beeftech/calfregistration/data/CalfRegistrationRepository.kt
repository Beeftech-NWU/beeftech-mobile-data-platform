package com.beeftech.calfregistration.data

import android.util.Log

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.util.TagNamingUtils
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.DuplicateTagException
import com.beeftech.database.entity.IdentifierTypes
import com.beeftech.database.repository.PendingSyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class CalfRegistrationRepository(
    private val calfRegistrationDao: CalfRegistrationDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: CalfRegistrationApiClient,
    private val captureContextProvider: () -> CalfCaptureContext
) {

    suspend fun loadAll(): List<CalfRegistrationData> {
        return try {
            val views = calfRegistrationDao.getAllRegistrationViews().firstOrNull() ?: emptyList()
            views.map { CalfRegistrationMappers.toFormData(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun isTagRegistered(tagNumber: String): Boolean {
        if (tagNumber.isBlank()) return false

        return try {
            calfRegistrationDao.findAnimalIdByTag(TagNamingUtils.parseAndExpand(tagNumber)) != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun saveCalf(formData: CalfRegistrationData): SaveCalfOutcome {
        return try {
            val warnings = mutableListOf<String>()

            suspend fun resolveParent(label: String, value: String): String? {
                val tag = CalfRegistrationMappers.parentTag(value) ?: return null
                return calfRegistrationDao.findAnimalIdByTag(tag)
                    ?: null.also { warnings += "$label $tag is not registered; saved without $label." }
            }

            val damId = resolveParent("Dam", formData.dameTagNumber)
            val sireId = resolveParent("Sire", formData.sireTagNumber)

            val newCalf = try {
                CalfRegistrationMappers.toNewCalf(formData, captureContextProvider(), damId, sireId)
            } catch (e: IllegalArgumentException) {
                return SaveCalfOutcome(formData.copy(synced = false), validationError = e.message)
            }

            try {
                calfRegistrationDao.registerCalf(
                    newCalf.animal,
                    newCalf.identifiers,
                    newCalf.media,
                    newCalf.registration
                )
            } catch (e: DuplicateTagException) {
                return SaveCalfOutcome(formData.copy(synced = false), validationError = e.message)
            }

            val tag = newCalf.identifiers.first { it.identifierType == IdentifierTypes.TAG }.identifierValue
            val view = calfRegistrationDao.getRegistrationByTag(tag).first()
                ?: error("Calf $tag was saved but could not be read back")

            val sync =
                apiClient.syncCalves(
                    listOf(view),
                    deviceId = view.deviceId
                )

            if (sync.isSuccess) {

                val serverResult =
                    sync
                        .getOrThrow()
                        .results
                        .firstOrNull {
                            it.recordguid == view.recordGuid
                        }

                if (serverResult?.status == SYNC_STATUS_SYNCED) {

                    calfRegistrationDao.markSynced(
                        listOf(view.recordGuid),
                        System.currentTimeMillis()
                    )

                    SaveCalfOutcome(
                        data =
                            CalfRegistrationMappers
                                .toFormData(view)
                                .copy(synced = true),
                        warnings = warnings
                    )

                } else {

                    pendingSyncRepository.queueOperation(
                        entityType = ENTITY_TYPE,
                        entityId = view.recordGuid,
                        operation = "UPSERT",
                        payload = ""
                    )

                    SaveCalfOutcome(
                        data =
                            CalfRegistrationMappers
                                .toFormData(view),

                        syncErrorMessage =
                            serverResult?.message
                                ?: "Server did not confirm calf synchronization.",

                        warnings = warnings
                    )
                }

            } else {

                pendingSyncRepository.queueOperation(
                    entityType = ENTITY_TYPE,
                    entityId = view.recordGuid,
                    operation = "UPSERT",
                    payload = ""
                )

                SaveCalfOutcome(
                    data =
                        CalfRegistrationMappers
                            .toFormData(view),

                    syncErrorMessage =
                        sync.exceptionOrNull()?.message
                            ?: "Sync failed",

                    warnings = warnings
                )
            }
        } catch (exception: Exception) {
            SaveCalfOutcome(
                data = formData.copy(synced = false),
                syncErrorMessage = exception.message
            )
        }
    }

    /*
     * BEEFTECH_CALF_PENDING_SOURCE_OF_TRUTH
     *
     * The calf-registration table is the source of truth.
     *
     * pending_sync is supporting bookkeeping only. A missing,
     * stale, or differently scoped queue row must never prevent
     * an actual PENDING calf from being synchronized.
     */
    suspend fun syncPending(): SyncPendingOutcome {

        return try {

            val pendingOperations =
                pendingSyncRepository
                    .getPendingOperations()
                    .filter {
                        it.entityType == ENTITY_TYPE
                    }

            /*
             * Read actual unsynchronized calf registrations FIRST.
             */
            val views =
                calfRegistrationDao
                    .getPendingRegistrationViews()

            /*
             * Nothing genuinely pending:
             * remove stale queue entries and finish successfully.
             */
            if (views.isEmpty()) {

                pendingOperations.forEach {
                    pendingSyncRepository
                        .markSyncSuccessful(it.id)
                }

                return SyncPendingOutcome(
                    syncedCount = 0
                )
            }

            val pendingGuids =
                views
                    .map { it.recordGuid }
                    .toSet()

            /*
             * Remove queue rows whose underlying calf is already
             * synced or no longer exists.
             */
            pendingOperations
                .filter {
                    it.entityId !in pendingGuids
                }
                .forEach {
                    pendingSyncRepository
                        .markSyncSuccessful(it.id)
                }

            val queuedEntityIds =
                pendingOperations
                    .map { it.entityId }
                    .toMutableSet()

            /*
             * Repair missing queue rows automatically.
             */
            views.forEach { view ->

                if (
                    queuedEntityIds.add(
                        view.recordGuid
                    )
                ) {

                    pendingSyncRepository
                        .queueOperation(
                            entityType = ENTITY_TYPE,
                            entityId = view.recordGuid,
                            operation = "UPSERT",
                            payload = ""
                        )
                }
            }

            Log.i(
                "CalfRegistrationRepository",
                "Attempting synchronization for " +
                    "${views.size} pending calf registration(s)."
            )

            val syncResult =
                apiClient.syncCalves(
                    views,
                    deviceId =
                        views.first().deviceId
                )

            if (syncResult.isSuccess) {

                val results =
                    syncResult
                        .getOrThrow()
                        .results

                val syncedGuids =
                    results
                        .filter {
                            it.status ==
                                SYNC_STATUS_SYNCED
                        }
                        .map {
                            it.recordguid
                        }
                        .toSet()

                if (syncedGuids.isNotEmpty()) {

                    calfRegistrationDao
                        .markSynced(
                            syncedGuids.toList(),
                            System.currentTimeMillis()
                        )

                    /*
                     * Re-read queue because missing entries may
                     * have been repaired above.
                     */
                    pendingSyncRepository
                        .getPendingOperations()
                        .filter {
                            it.entityType ==
                                ENTITY_TYPE &&
                                it.entityId in syncedGuids
                        }
                        .forEach {

                            pendingSyncRepository
                                .markSyncSuccessful(
                                    it.id
                                )
                        }
                }

                val resultByGuid =
                    results.associateBy {
                        it.recordguid
                    }

                val errors =
                    views
                        .filter {
                            it.recordGuid !in
                                syncedGuids
                        }
                        .associate { view ->

                            val result =
                                resultByGuid[
                                    view.recordGuid
                                ]

                            view.tagNumber to (
                                result?.message
                                    ?: "Server did not confirm calf synchronization."
                            )
                        }

                Log.i(
                    "CalfRegistrationRepository",
                    "Calf synchronization result: " +
                        "${syncedGuids.size} synced, " +
                        "${errors.size} still pending."
                )

                SyncPendingOutcome(
                    syncedCount =
                        syncedGuids.size,

                    errorMessagesByTagNumber =
                        errors
                )

            } else {

                val message =
                    syncResult
                        .exceptionOrNull()
                        ?.message
                        ?: "Unable to reach the server"

                Log.w(
                    "CalfRegistrationRepository",
                    "Calf synchronization request failed: $message"
                )

                SyncPendingOutcome(
                    syncedCount = 0,

                    errorMessagesByTagNumber =
                        views.associate {
                            it.tagNumber to message
                        }
                )
            }

        } catch (exception: Exception) {

            /*
             * Never silently convert a real synchronization
             * exception into a successful empty result.
             */
            Log.e(
                "CalfRegistrationRepository",
                "Pending calf synchronization failed unexpectedly.",
                exception
            )

            val remainingViews =
                try {
                    calfRegistrationDao
                        .getPendingRegistrationViews()
                } catch (_: Exception) {
                    emptyList()
                }

            val message =
                exception.message
                    ?: "Unexpected calf synchronization error."

            val errors =
                if (remainingViews.isNotEmpty()) {

                    remainingViews.associate {
                        it.tagNumber to message
                    }

                } else {

                    mapOf(
                        "_sync" to message
                    )
                }

            SyncPendingOutcome(
                syncedCount = 0,
                errorMessagesByTagNumber = errors
            )
        }
    }

    companion object {
        private const val ENTITY_TYPE = "CALF_REGISTRATION"
    }
}

data class SaveCalfOutcome(
    val data: CalfRegistrationData,
    val syncErrorMessage: String? = null,
    val warnings: List<String> = emptyList(),
    val validationError: String? = null
)

data class SyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByTagNumber: Map<String, String?> = emptyMap()
)
