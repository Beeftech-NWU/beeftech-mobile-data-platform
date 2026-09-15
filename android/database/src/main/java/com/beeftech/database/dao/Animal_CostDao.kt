package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalCostEntity


@Dao
interface Animal_CostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCost(cost: AnimalCostEntity)

    @Query("SELECT * FROM animal_costs WHERE animalId = :animalGuid ORDER BY timestamp DESC")
    suspend fun getCostsForAnimal(animalGuid: String): List<AnimalCostEntity>
}