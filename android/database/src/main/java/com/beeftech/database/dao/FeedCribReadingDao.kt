package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.FeedCribReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedCribReadingDao {
    @Query("SELECT * FROM feed_crib_readings")
    fun getAll(): Flow<List<FeedCribReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: FeedCribReadingEntity)
}
