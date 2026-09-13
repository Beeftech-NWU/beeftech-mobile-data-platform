package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.AnimalCost

@Dao
interface AnimalCostDao {

    @Insert
    suspend fun insert(
        cost: AnimalCost
    )

    @Query(
        """
        SELECT *
        FROM animal_costs
        WHERE animalId = :animalId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByAnimalId(
        animalId: String
    ): List<AnimalCost>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM animal_costs
        WHERE animalId = :animalId
        AND costType = :costType
        """
    )
    suspend fun getTotalByType(
        animalId: String,
        costType: String
    ): Double
}