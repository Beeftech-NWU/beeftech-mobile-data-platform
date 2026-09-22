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
    ): List<PendingSync> {

        return pendingSyncDao.getPendingForRetry(
            maxRetries
        )
    }

    suspend fun getAllPendingOperations():
            List<PendingSync> {

        return pendingSyncDao.getAll()
    }

    suspend fun getOperationsForEntity(
        entityType: String,
        entityId: String
    ): List<PendingSync> {

        return pendingSyncDao.getByEntity(
            entityType = entityType,
            entityId = entityId
        )
    }

    suspend fun markSyncFailed(
        id: Long
    ) {

        pendingSyncDao.incrementRetryCount(
            id
        )
    }

    suspend fun markSyncSuccessful(
        id: Long
    ) {

        pendingSyncDao.deleteById(
            id
        )
    }

    suspend fun markEntitySyncSuccessful(
        entityType: String,
        entityId: String
    ) {

        pendingSyncDao.deleteByEntity(
            entityType = entityType,
            entityId = entityId
        )
    }

    suspend fun getPendingCount(): Int {

        return pendingSyncDao.getPendingCount()
    }

    suspend fun clearAll() {

        pendingSyncDao.clearAll()
    }

    companion object {

        const val DEFAULT_MAX_RETRIES = 5
    }
}