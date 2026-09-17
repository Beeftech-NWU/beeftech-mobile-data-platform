package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.PenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PenDao {
    @Query("SELECT * FROM pens")
    fun getAll(): Flow<List<PenEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pen: PenEntity)
}
