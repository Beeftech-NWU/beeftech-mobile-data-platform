package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.SyncLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<SyncLog>>

    @Insert
    suspend fun insertLog(log: SyncLog): Long

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}
