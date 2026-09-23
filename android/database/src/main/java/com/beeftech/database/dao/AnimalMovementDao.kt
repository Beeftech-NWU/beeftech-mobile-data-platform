package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalMovementEntity

@Dao
interface AnimalMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: AnimalMovementEntity)

    @Query("SELECT * FROM animal_movements ORDER BY movement_date DESC")
    suspend fun getAll(): List<AnimalMovementEntity>

    @Query(
        """
        SELECT * 
        FROM animal_movements 
        WHERE animal_id = :animalId
        ORDER BY movement_date DESC
        """
    )
    suspend fun getByAnimalId(
        animalId: String
    ): List<AnimalMovementEntity>
}
