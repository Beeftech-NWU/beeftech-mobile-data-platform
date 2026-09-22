package com.beeftech.database.repository

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.SyncBatchDao
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.entity.SyncBatchEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SyncRepository(
    private val pendingSyncDao: PendingSyncDao,
    private val syncBatchDao: SyncBatchDao
) {

    fun observePendingCount(): Flow<Int> {
        return pendingSyncDao.observePendingCount()
    }

    fun observeLatestSyncBatch(): Flow<SyncBatchEntity?> {
        return syncBatchDao.observeLatest()
    }

    suspend fun getPendingRecords(): List<PendingSync> {
        return pendingSyncDao.getAll()
    }

    suspend fun getPendingForRetry(
        maxRetries: Int = 3
    ): List<PendingSync> {
        return pendingSyncDao.getPendingForRetry(maxRetries)
    }

    suspend fun incrementRetryCount(id: Long) {
        pendingSyncDao.incrementRetryCount(id)
    }

    suspend fun deletePendingRecord(id: Long) {
        pendingSyncDao.deleteById(id)
    }

    suspend fun recordSuccessfulSync(
        timestamp: Long = System.currentTimeMillis()
    ) {
        syncBatchDao.insert(
            SyncBatchEntity(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp
            )
        )
    }
}
