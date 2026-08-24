package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.Treatment

@Dao
interface TreatmentDao {

    @Insert
    suspend fun insert(treatment: Treatment)

    @Query("SELECT * FROM treatments")
    suspend fun getAll(): List<Treatment>
}