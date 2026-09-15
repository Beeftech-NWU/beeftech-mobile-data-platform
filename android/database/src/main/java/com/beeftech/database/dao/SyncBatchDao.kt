package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.SyncBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncBatchDao {
    @Query("SELECT * FROM sync_batches")
    fun getAll(): Flow<List<SyncBatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: SyncBatchEntity)
}
