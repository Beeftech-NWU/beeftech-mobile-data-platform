package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.SyncBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncBatchDao {

    @Query("SELECT * FROM sync_batches ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SyncBatchEntity>>

    @Query(
        """
        SELECT * FROM sync_batches
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    fun observeLatest(): Flow<SyncBatchEntity?>

    @Query(
        """
        SELECT * FROM sync_batches
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun getLatest(): SyncBatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: SyncBatchEntity)
}