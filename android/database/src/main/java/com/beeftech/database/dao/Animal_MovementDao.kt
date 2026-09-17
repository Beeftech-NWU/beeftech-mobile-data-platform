package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.beeftech.database.entity.AnimalMovementEntity

@Dao
interface Animal_MovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovement(movement: AnimalMovementEntity)

    @Query("SELECT * FROM animal_movements WHERE animalId = :animalGuid ORDER BY timestamp DESC")
    suspend fun getMovementsForAnimal(animalGuid: String): List<AnimalMovementEntity>
}