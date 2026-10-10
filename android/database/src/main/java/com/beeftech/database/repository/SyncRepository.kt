package com.beeftech.database.repository

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.SyncBatchDao
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.entity.SyncBatchEntity
import com.beeftech.database.security.CurrentUserIdRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class SyncRepository(
    private val pendingSyncDao: PendingSyncDao,
    private val syncBatchDao: SyncBatchDao,
    private val userIdProvider: () -> String? = {
        CurrentUserIdRegistry.currentUserId()
    },
    private val userIdFlow: Flow<String?> =
        CurrentUserIdRegistry.currentUserIdFlow
) {

    /*
     * The same ViewModel can remain alive while USER-A logs out
     * and USER-B logs in. flatMapLatest switches the Room query
     * to the newly authenticated user's queue automatically.
     */
    fun observePendingCount():
            Flow<Int> {

        return userIdFlow
            .flatMapLatest {
                    rawUserId ->

                val userId =
                    normalizeUserId(
                        rawUserId
                    )

                if (userId == null) {

                    flowOf(0)

                } else {

                    pendingSyncDao
                        .observePendingCountForUser(
                            userId
                        )
                }
            }
    }

    fun observeOldestPendingCreatedAt():
            Flow<Long?> {

        return userIdFlow
            .flatMapLatest {
                    rawUserId ->

                val userId =
                    normalizeUserId(
                        rawUserId
                    )

                if (userId == null) {

                    flowOf(null)

                } else {

                    pendingSyncDao
                        .observeOldestPendingCreatedAtForUser(
                            userId
                        )
                }
            }
    }

    fun observeLatestSyncBatch():
            Flow<SyncBatchEntity?> {

        return syncBatchDao
            .observeLatest()
    }

    suspend fun getPendingForRetry(
        maxRetries: Int = 3
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

    suspend fun incrementRetryCount(
        id: Long
    ) {

        pendingSyncDao
            .incrementRetryCount(
                id
            )
    }

    suspend fun recordSuccessfulSync(
        timestamp: Long =
            System.currentTimeMillis()
    ) {

        syncBatchDao.insert(
            SyncBatchEntity(
                id =
                    UUID.randomUUID()
                        .toString(),

                timestamp =
                    timestamp
            )
        )
    }

    private fun currentUserId():
            String? {

        return normalizeUserId(
            userIdProvider()
        )
    }

    private fun normalizeUserId(
        userId: String?
    ): String? {

        return userId
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
    }
}
