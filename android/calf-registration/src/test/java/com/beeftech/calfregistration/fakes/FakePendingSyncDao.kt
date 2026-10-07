package com.beeftech.calfregistration.fakes

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.entity.PendingSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Simple in-memory fake of [PendingSyncDao] used to unit test
 * [com.beeftech.database.repository.PendingSyncRepository] usage from the
 * calf-registration repository, without pulling in Room/Robolectric.
 */
class FakePendingSyncDao : PendingSyncDao {

    private val items = mutableListOf<PendingSync>()
    private var nextId = 1L

    override suspend fun insert(item: PendingSync): Long {
        val withId = item.copy(id = nextId++)
        items.add(withId)
        return withId.id
    }

    override suspend fun getAll(): List<PendingSync> {
        return items.sortedBy { it.createdAt }
    }

    override suspend fun getPendingForRetry(maxRetries: Int): List<PendingSync> {
        return items.filter { it.retryCount < maxRetries }.sortedBy { it.createdAt }
    }

    override suspend fun getByEntity(entityType: String, entityId: String): List<PendingSync> {
        return items.filter { it.entityType == entityType && it.entityId == entityId }.sortedBy { it.createdAt }
    }

    override suspend fun incrementRetryCount(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(retryCount = items[index].retryCount + 1)
        }
    }

    override suspend fun update(item: PendingSync) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            items[index] = item
        }
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

    override suspend fun clearAll() {
        items.clear()
    }

    override suspend fun getPendingCount(): Int {
        return items.size
    }

    override fun observePendingCount(): Flow<Int> {
        return flowOf(items.size)
    }

    override fun observeOldestPendingCreatedAt(): Flow<Long?> {
        return flowOf(items.minOfOrNull { it.createdAt })
    }

    override suspend fun getAllForUser(userId: String): List<PendingSync> {
        return items.filter { it.userId == userId }.sortedBy { it.createdAt }
    }

    override suspend fun getPendingForRetryForUser(userId: String, maxRetries: Int): List<PendingSync> {
        return items
            .filter { it.userId == userId && it.retryCount < maxRetries }
            .sortedBy { it.createdAt }
    }

    override suspend fun getByEntityForUser(
        userId: String,
        entityType: String,
        entityId: String
    ): List<PendingSync> {
        return items
            .filter { it.userId == userId && it.entityType == entityType && it.entityId == entityId }
            .sortedBy { it.createdAt }
    }

    override suspend fun getPendingCountForUser(userId: String): Int {
        return items.count { it.userId == userId }
    }

    override fun observePendingCountForUser(userId: String): Flow<Int> {
        return flowOf(items.count { it.userId == userId })
    }

    override fun observeOldestPendingCreatedAtForUser(userId: String): Flow<Long?> {
        return flowOf(items.filter { it.userId == userId }.minOfOrNull { it.createdAt })
    }

    override fun observeRetryLimitCountForUser(
        userId: String,
        retryLimit: Int
    ): Flow<Int> =
        flowOf(
            items.count {
                it.userId == userId &&
                    it.retryCount >= retryLimit
            }
        )
    override suspend fun resetRetryCount(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(retryCount = 0)
        }
    }

    override suspend fun reassignEntityToUser(entityType: String, entityId: String, userId: String) {
        items.replaceAll {
            if (it.entityType == entityType && it.entityId == entityId) {
                it.copy(userId = userId, retryCount = 0)
            } else {
                it
            }
        }
    }

    override suspend fun reassignUserOperations(oldUserId: String, newUserId: String): Int {
        var moved = 0
        items.replaceAll {
            if (it.userId == oldUserId) {
                moved++
                it.copy(userId = newUserId, retryCount = 0)
            } else {
                it
            }
        }
        return moved
    }

    fun snapshot(): List<PendingSync> = items.toList()
}
