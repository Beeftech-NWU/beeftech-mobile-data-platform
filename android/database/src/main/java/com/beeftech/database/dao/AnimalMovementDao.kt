package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.AnimalMovement

@Dao
interface AnimalMovementDao {

    @Insert
    suspend fun insert(movement: AnimalMovement)

    @Query("SELECT * FROM animal_movements")
    suspend fun getAll(): List<AnimalMovement>
}