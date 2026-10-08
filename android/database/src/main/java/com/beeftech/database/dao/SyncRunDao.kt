package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.SyncRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRunDao {

    @Insert
    suspend fun insert(run: SyncRunEntity): Long

    /** Newest first. */
    @Query(
        """
        SELECT * FROM sync_runs
        WHERE user_id = :userId
        ORDER BY started_at DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeRecent(userId: String, limit: Int = 50): Flow<List<SyncRunEntity>>

    @Query(
        """
        SELECT * FROM sync_runs
        WHERE user_id = :userId
        ORDER BY started_at DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getRecent(userId: String, limit: Int = 50): List<SyncRunEntity>

    @Query("DELETE FROM sync_runs WHERE started_at < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long): Int
}
