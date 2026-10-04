package com.beeftech.farmtraceability

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.entity.PendingSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/* In-memory stand-in for the pending_sync queue, shared by the repository tests. */
internal class FakePendingSyncDao : PendingSyncDao {
    val items = mutableListOf<PendingSync>()
    private var nextId = 1L

    override suspend fun insert(item: PendingSync): Long {
        val id = nextId++
        items += item.copy(id = id)
        return id
    }

    override suspend fun getAll() = items.toList()
    override suspend fun getPendingForRetry(maxRetries: Int) = items.filter { it.retryCount < maxRetries }
    override suspend fun getByEntity(entityType: String, entityId: String) =
        items.filter { it.entityType == entityType && it.entityId == entityId }

    override suspend fun incrementRetryCount(id: Long) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = items[i].copy(retryCount = items[i].retryCount + 1)
    }

    override suspend fun update(item: PendingSync) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) items[i] = item
    }

    override suspend fun delete(item: PendingSync) {
        items.removeAll { it.id == item.id }
    }

    override suspend fun deleteById(id: Long) {
        items.removeAll { it.id == id }
    }

    override suspend fun deleteByEntity(entityType: String, entityId: String) {
        items.removeAll { it.entityType == entityType && it.entityId == entityId }
    }

    override suspend fun clearAll() = items.clear()
    override suspend fun getPendingCount() = items.size
    override fun observePendingCount(): Flow<Int> = flowOf(items.size)
    override fun observeOldestPendingCreatedAt(): Flow<Long?> = flowOf(items.minOfOrNull { it.createdAt })
    override suspend fun getAllForUser(userId: String) = items.filter { it.userId == userId }
    override suspend fun getPendingForRetryForUser(userId: String, maxRetries: Int) =
        items.filter { it.userId == userId && it.retryCount < maxRetries }

    override suspend fun getByEntityForUser(userId: String, entityType: String, entityId: String) =
        items.filter { it.userId == userId && it.entityType == entityType && it.entityId == entityId }

    override suspend fun getPendingCountForUser(userId: String) = items.count { it.userId == userId }
    override fun observePendingCountForUser(userId: String): Flow<Int> =
        flowOf(items.count { it.userId == userId })

    override fun observeOldestPendingCreatedAtForUser(userId: String): Flow<Long?> =
        flowOf(items.filter { it.userId == userId }.minOfOrNull { it.createdAt })

    override suspend fun resetRetryCount(id: Long) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = items[i].copy(retryCount = 0)
    }

    override suspend fun reassignEntityToUser(entityType: String, entityId: String, userId: String) {
        items.replaceAll {
            if (it.entityType == entityType && it.entityId == entityId) it.copy(userId = userId, retryCount = 0) else it
        }
    }

    override suspend fun reassignUserOperations(oldUserId: String, newUserId: String): Int {
        var moved = 0
        items.replaceAll {
            if (it.userId == oldUserId) {
                moved++
                it.copy(userId = newUserId, retryCount = 0)
            } else it
        }
        return moved
    }
}
