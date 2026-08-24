package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beeftech.database.entity.PendingSync

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
        UPDATE pending_sync
        SET retryCount = retryCount + 1
        WHERE id = :id
        """
    )
    suspend fun incrementRetryCount(
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
        """
    )
    suspend fun clearAll()

    @Query(
        """
        SELECT COUNT(*) FROM pending_sync
        """
    )
    suspend fun getPendingCount(): Int
}