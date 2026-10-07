package com.beeftech.database.repository

import com.beeftech.database.dao.PendingSyncDao
import kotlinx.coroutines.flow.Flow
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.security.CurrentUserIdRegistry

class PendingSyncRepository(
    private val pendingSyncDao: PendingSyncDao,
    private val userIdProvider: () -> String? = {
        CurrentUserIdRegistry.currentUserId()
    }
) {

    suspend fun queueOperation(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String
    ): Long {

        return pendingSyncDao.insert(
            PendingSync(
                userId =
                    currentUserId(),

                entityType =
                    entityType,

                entityId =
                    entityId,

                operation =
                    operation,

                payload =
                    payload,

                createdAt =
                    System.currentTimeMillis(),

                retryCount =
                    0
            )
        )
    }

    /*
     * Returns only retry-eligible operations belonging to the
     * currently authenticated user.
     *
     * If no user is authenticated, do not expose another
     * account's offline queue.
     */
    suspend fun getPendingOperations(
        maxRetries: Int = DEFAULT_MAX_RETRIES
    ): List<PendingSync> {

        val userId =
            currentUserId()
                ?: return emptyList()

        return pendingSyncDao
            .getPendingForRetryForUser(
                userId =
                    userId,

                maxRetries =
                    maxRetries
            )
    }

    /*
     * Includes records that reached their retry limit, but still
     * only for the active user.
     */
    suspend fun getAllPendingOperations():
            List<PendingSync> {

        val userId =
            currentUserId()
                ?: return emptyList()

        return pendingSyncDao
            .getAllForUser(
                userId
            )
    }

    suspend fun getOperationsForEntity(
        entityType: String,
        entityId: String
    ): List<PendingSync> {

        val userId =
            currentUserId()
                ?: return emptyList()

        return pendingSyncDao
            .getByEntityForUser(
                userId =
                    userId,

                entityType =
                    entityType,

                entityId =
                    entityId
            )
    }

    suspend fun resetRetryCount(
        id: Long
    ) {
        pendingSyncDao.resetRetryCount(
            id
        )
    }

    suspend fun markSyncFailed(
        id: Long
    ) {

        /*
         * IDs supplied to this method come from the scoped
         * repository reads above.
         */
        pendingSyncDao
            .incrementRetryCount(
                id
            )
    }

    suspend fun markSyncSuccessful(
        id: Long
    ) {

        pendingSyncDao
            .deleteById(
                id
            )
    }

    suspend fun markEntitySyncSuccessful(
        entityType: String,
        entityId: String
    ) {

        /*
         * Do not use the old device-wide deleteByEntity here.
         * Delete only rows owned by the active account.
         */
        getOperationsForEntity(
            entityType =
                entityType,

            entityId =
                entityId
        ).forEach {
                pending ->

            pendingSyncDao
                .deleteById(
                    pending.id
                )
        }
    }

    /* For the "My activity" screen: the backlog for one user, as it changes. */
    fun observePendingCount(userId: String): Flow<Int> =
        pendingSyncDao.observePendingCountForUser(userId)

    fun observeOldestPendingAt(userId: String): Flow<Long?> =
        pendingSyncDao.observeOldestPendingCreatedAtForUser(userId)

    fun observeFailedCount(
        userId: String,
        retryLimit: Int = DEFAULT_MAX_RETRIES
    ): Flow<Int> =
        pendingSyncDao.observeRetryLimitCountForUser(
            userId = userId,
            retryLimit = retryLimit
        )

    suspend fun getPendingCount():
            Int {

        val userId =
            currentUserId()
                ?: return 0

        return pendingSyncDao
            .getPendingCountForUser(
                userId
            )
    }

    /*
     * User-scoped clear.
     *
     * Never call pendingSyncDao.clearAll() from normal runtime
     * code because that would remove other users' queues and
     * legacy unowned rows.
     */
    suspend fun clearAll() {

        getAllPendingOperations()
            .forEach {
                    pending ->

                pendingSyncDao
                    .deleteById(
                        pending.id
                    )
            }
    }

    private fun currentUserId():
            String? {

        return userIdProvider()
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
    }

    companion object {

        const val DEFAULT_MAX_RETRIES =
            3
    }
}
