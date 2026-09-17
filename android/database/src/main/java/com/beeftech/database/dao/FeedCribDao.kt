package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.FeedCribEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedCribDao {
    @Query("SELECT * FROM feed_cribs")
    fun getAll(): Flow<List<FeedCribEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crib: FeedCribEntity)
}
