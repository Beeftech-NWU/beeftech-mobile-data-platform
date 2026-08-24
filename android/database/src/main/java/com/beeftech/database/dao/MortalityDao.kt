package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.Mortality

@Dao
interface MortalityDao {

    @Insert
    suspend fun insert(mortality: Mortality)

    @Query("SELECT * FROM mortalities")
    suspend fun getAll(): List<Mortality>
}