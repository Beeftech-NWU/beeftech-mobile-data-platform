package com.beeftech.calfregistration.data

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

            val sync = apiClient.syncCalves(listOf(view), deviceId = view.deviceId)
            if (sync.isSuccess) {
                calfRegistrationDao.markSynced(listOf(view.recordGuid), System.currentTimeMillis())
                SaveCalfOutcome(
                    data = CalfRegistrationMappers.toFormData(view).copy(synced = true),
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
                    data = CalfRegistrationMappers.toFormData(view),
                    syncErrorMessage = sync.exceptionOrNull()?.message ?: "Sync failed",
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

    suspend fun syncPending(): SyncPendingOutcome {
        return try {
            val pendingOperations = pendingSyncRepository.getPendingOperations()
                .filter { it.entityType == ENTITY_TYPE }

            if (pendingOperations.isEmpty()) {
                return SyncPendingOutcome(syncedCount = 0)
            }

            val views = calfRegistrationDao.getPendingRegistrationViews()
            val pendingGuids = views.map { it.recordGuid }.toSet()

            // Operations whose record is already SYNCED (or gone) have nothing left to send.
            pendingOperations.filter { it.entityId !in pendingGuids }
                .forEach { pendingSyncRepository.markSyncSuccessful(it.id) }

            if (views.isEmpty()) {
                return SyncPendingOutcome(syncedCount = 0)
            }

            val syncResult = apiClient.syncCalves(views, deviceId = views.first().deviceId)
            if (syncResult.isSuccess) {
                val results = syncResult.getOrThrow().results
                val syncedGuids = results.filter { it.status == SYNC_STATUS_SYNCED }.map { it.recordguid }

                if (syncedGuids.isNotEmpty()) {
                    calfRegistrationDao.markSynced(syncedGuids, System.currentTimeMillis())
                    pendingOperations.filter { it.entityId in syncedGuids }
                        .forEach { pendingSyncRepository.markSyncSuccessful(it.id) }
                }

                val errors = results
                    .filter { it.status != SYNC_STATUS_SYNCED }
                    .associate { it.tagNumber to it.message }
                SyncPendingOutcome(syncedCount = syncedGuids.size, errorMessagesByTagNumber = errors)
            } else {
                val errorMsg = syncResult.exceptionOrNull()?.message ?: "Sync failed"
                val errors = views.associate { it.tagNumber to (errorMsg as String?) }
                SyncPendingOutcome(syncedCount = 0, errorMessagesByTagNumber = errors)
            }
        } catch (_: Exception) {
            SyncPendingOutcome(syncedCount = 0)
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
