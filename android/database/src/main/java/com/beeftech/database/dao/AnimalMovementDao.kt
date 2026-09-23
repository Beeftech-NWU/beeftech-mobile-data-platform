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

    @Query(
        """
        SELECT *
        FROM animal_movements
        WHERE movement_id = :movementId
        LIMIT 1
        """
    )
    suspend fun findByMovementId(
        movementId: String
    ): AnimalMovementEntity?

    @Query(
        """
        SELECT *
        FROM animal_movements
        WHERE animal_id = :animalId
          AND destination_farm_id = :destinationFarmId
          AND notes = :notes
        ORDER BY movement_date DESC
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicate(
        animalId: String,
        destinationFarmId: String,
        notes: String
    ): AnimalMovementEntity?
}
