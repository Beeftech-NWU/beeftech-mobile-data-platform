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
        return emptyList()
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
            val entity = CalfRegistrationMappers.toEntity(formData)
            calfRegistrationDao.insertCalfRegistration(entity)
            SaveCalfOutcome(data = formData.copy(synced = true))
        } catch (exception: Exception) {
            SaveCalfOutcome(
                data = formData.copy(synced = false),
                syncErrorMessage = exception.message
            )
        }
    }

    suspend fun syncPending(): SyncPendingOutcome {
        return SyncPendingOutcome(syncedCount = 0)
    }

    companion object {
        private const val ENTITY_TYPE =
            "CALF_REGISTRATION"
    }
}

data class SaveCalfOutcome(
    val data: CalfRegistrationData,
    val syncErrorMessage: String? = null
)

data class SyncPendingOutcome(
    val syncedCount: Int,
    val errorMessagesByAnimalId:
        Map<String, String?> = emptyMap()
)