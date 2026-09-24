package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalCost

@Dao
interface AnimalCostDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cost: AnimalCost): Long

    @Query(
        """
        SELECT *
        FROM animal_costs
        WHERE animalId = :animalId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByAnimalId(animalId: String): List<AnimalCost>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM animal_costs
        WHERE animalId = :animalId
          AND costType = :costType
        """
    )
    suspend fun getTotalByType(animalId: String, costType: String): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM animal_costs
        WHERE animalId = :animalId
        """
    )
    suspend fun getTotalForAnimal(animalId: String): Double

    @Query(
        """
        SELECT costType, COALESCE(SUM(amount), 0.0) AS total
        FROM animal_costs
        WHERE animalId = :animalId
        GROUP BY costType
        ORDER BY costType
        """
    )
    suspend fun getTotalsByType(animalId: String): List<CostTypeTotal>
}

data class CostTypeTotal(
    val costType: String,
    val total: Double
)
