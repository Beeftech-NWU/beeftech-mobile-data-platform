package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beeftech.database.entity.PendingSync
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Insert
    suspend fun insert(item: PendingSync): Long

    @Query(
        """
        SELECT * FROM pending_sync
        ORDER BY createdAt ASC
        """
    )
    suspend fun getAll(): List<PendingSync>

    @Query(
        """
        SELECT * FROM pending_sync
        WHERE retryCount < :maxRetries
        ORDER BY createdAt ASC
        """
    )
    suspend fun getPendingForRetry(
        maxRetries: Int
    ): List<PendingSync>

    @Query(
        """
        SELECT * FROM pending_sync
        WHERE entityType = :entityType
        AND entityId = :entityId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getByEntity(
        entityType: String,
        entityId: String
    ): List<PendingSync>

    @Query(
        """
        UPDATE pending_sync
        SET retryCount = retryCount + 1
        WHERE id = :id
        """
    )
    suspend fun incrementRetryCount(
        id: Long
    )

    @Query(
        """
        UPDATE pending_sync
        SET retryCount = 0
        WHERE id = :id
        """
    )
    suspend fun resetRetryCount(
        id: Long
    )

    @Update
    suspend fun update(item: PendingSync)

    @Delete
    suspend fun delete(item: PendingSync)

    @Query(
        """
        DELETE FROM pending_sync
        WHERE id = :id
        """
    )
    suspend fun deleteById(
        id: Long
    )

    @Query(
        """
        DELETE FROM pending_sync
        WHERE entityType = :entityType
        AND entityId = :entityId
        """
    )
    suspend fun deleteByEntity(
        entityType: String,
        entityId: String
    )

    @Query("DELETE FROM pending_sync")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT MIN(createdAt) FROM pending_sync")
    fun observeOldestPendingCreatedAt(): Flow<Long?>

    // ========================================================
    // User-scoped Day-7 policy queries
    // ========================================================

    @Query(
        """
        SELECT *
        FROM pending_sync
        WHERE user_id = :userId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getAllForUser(
        userId: String
    ): List<PendingSync>

    @Query(
        """
        SELECT *
        FROM pending_sync
        WHERE user_id = :userId
          AND retryCount < :maxRetries
        ORDER BY createdAt ASC
        """
    )
    suspend fun getPendingForRetryForUser(
        userId: String,
        maxRetries: Int
    ): List<PendingSync>

    @Query(
        """
        SELECT *
        FROM pending_sync
        WHERE user_id = :userId
          AND entityType = :entityType
          AND entityId = :entityId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getByEntityForUser(
        userId: String,
        entityType: String,
        entityId: String
    ): List<PendingSync>

    @Query(
        """
        SELECT COUNT(*)
        FROM pending_sync
        WHERE user_id = :userId
        """
    )
    suspend fun getPendingCountForUser(
        userId: String
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM pending_sync
        WHERE user_id = :userId
        """
    )
    fun observePendingCountForUser(
        userId: String
    ): Flow<Int>

    @Query(
        """
        SELECT MIN(createdAt)
        FROM pending_sync
        WHERE user_id = :userId
        """
    )
    fun observeOldestPendingCreatedAtForUser(
        userId: String
    ): Flow<Long?>


    @Query(
        """
        SELECT COUNT(*)
        FROM pending_sync
        WHERE user_id = :userId
          AND retryCount >= :retryLimit
        """
    )
    fun observeRetryLimitCountForUser(
        userId: String,
        retryLimit: Int
    ): Flow<Int>


    @Query(
        """
        UPDATE pending_sync
        SET user_id = :userId,
            retryCount = 0
        WHERE entityType = :entityType
          AND entityId = :entityId
        """
    )
    suspend fun reassignEntityToUser(
        entityType: String,
        entityId: String,
        userId: String
    )


    /*
     * When an online login confirms that an existing cached
     * username now has a different server user ID, move that
     * same user's pending work to the authenticated identity.
     */
    @Query(
        """
        UPDATE pending_sync
        SET user_id = :newUserId,
            retryCount = 0
        WHERE user_id = :oldUserId
        """
    )
    suspend fun reassignUserOperations(
        oldUserId: String,
        newUserId: String
    ): Int

}
