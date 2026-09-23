package com.beeftech.calfregistration.data

import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.entity.CalfRegistrationEntity
import com.beeftech.database.repository.PendingSyncRepository
import kotlinx.coroutines.flow.firstOrNull

class CalfRegistrationRepository(
    private val calfRegistrationDao: CalfRegistrationDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: CalfRegistrationApiClient
) {

    suspend fun loadAll(): List<CalfRegistrationData> {
        return try {
            val entities = calfRegistrationDao.getAllCalfRegistrations().firstOrNull() ?: emptyList()
            entities.map { CalfRegistrationMappers.toFormData(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun isTagRegistered(tagNumber: String): Boolean {
        if (tagNumber.isBlank()) return false

        return try {
            val details = calfRegistrationDao.getCalfRegistrationDetails(tagNumber.trim()).firstOrNull()
            details != null
        } catch (exception: Exception) {
            false
        }
    }

    suspend fun saveCalf(formData: CalfRegistrationData): SaveCalfOutcome {
        return try {
            val existingDetails = calfRegistrationDao.getCalfRegistrationDetails(formData.tagNumber.trim()).firstOrNull()
            val existingEntity = existingDetails?.let {
                CalfRegistrationEntity(
                    registrationId = it.registrationId,
                    registeredAnimalId = it.registeredAnimalId,
                    damId = it.damId,
                    sireId = it.sireId,
                    birthWeightKg = it.birthWeightKg,
                    calvingEase = it.calvingEase,
                    registrationDate = it.registrationDate
                )
            }

            val entity = CalfRegistrationMappers.toEntity(
                formData = formData,
                existing = existingEntity
            )
            calfRegistrationDao.insertCalfRegistration(entity)

            val syncResult = apiClient.syncCalves(listOf(entity), deviceId = "")
            if (syncResult.isSuccess) {
                val formDataResult = CalfRegistrationMappers.toFormData(entity).copy(synced = true)
                SaveCalfOutcome(data = formDataResult)
            } else {
                val errorMsg = syncResult.exceptionOrNull()?.message ?: "Sync failed"
                pendingSyncRepository.queueOperation(
                    entityType = ENTITY_TYPE,
                    entityId = entity.registrationId,
                    operation = "UPSERT",
                    payload = ""
                )
                val formDataResult = CalfRegistrationMappers.toFormData(entity).copy(synced = false)
                SaveCalfOutcome(data = formDataResult, syncErrorMessage = errorMsg)
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

            val allEntities = calfRegistrationDao.getAllCalfRegistrations().firstOrNull() ?: emptyList()
            val pendingIds = pendingOperations.map { it.entityId }.toSet()
            val entitiesToSync = allEntities.filter { it.registrationId in pendingIds }

            if (entitiesToSync.isEmpty()) {
                return SyncPendingOutcome(syncedCount = 0)
            }

            val syncResult = apiClient.syncCalves(entitiesToSync, deviceId = "")
            if (syncResult.isSuccess) {
                for (op in pendingOperations) {
                    pendingSyncRepository.markSyncSuccessful(op.id)
                }
                SyncPendingOutcome(syncedCount = entitiesToSync.size)
            } else {
                val errorMsg = syncResult.exceptionOrNull()?.message ?: "Sync failed"
                val errors = entitiesToSync.associate { it.registeredAnimalId to (errorMsg as String?) }
                SyncPendingOutcome(syncedCount = 0, errorMessagesByAnimalId = errors)
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
    val syncErrorMessage: String? = null
)

data class SyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByAnimalId: Map<String, String?> = emptyMap()
)
