package com.beeftech.database.repository

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.entity.PendingSync

class PendingSyncRepository(
    private val pendingSyncDao: PendingSyncDao
) {
    suspend fun queueOperation(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String
    ): Long {
        return pendingSyncDao.insert(
            PendingSync(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                createdAt = System.currentTimeMillis(),
                retryCount = 0
            )
        )
    }

    suspend fun getPendingOperations(
        maxRetries: Int = DEFAULT_MAX_RETRIES
    ): List<PendingSync> =
        pendingSyncDao.getPendingForRetry(maxRetries)

    // Includes records that have already reached the retry limit.
    suspend fun getAllPendingOperations(): List<PendingSync> =
        pendingSyncDao.getAll()

    suspend fun markSyncFailed(id: Long) {
        pendingSyncDao.incrementRetryCount(id)
    }

    suspend fun markSyncSuccessful(id: Long) {
        pendingSyncDao.deleteById(id)
    }

    suspend fun getPendingCount(): Int =
        pendingSyncDao.getPendingCount()

    suspend fun clearAll() {
        pendingSyncDao.clearAll()
    }

    companion object {
        const val DEFAULT_MAX_RETRIES = 5
    }
}
