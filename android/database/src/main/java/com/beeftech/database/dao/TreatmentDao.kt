package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.Treatment

@Dao
interface TreatmentDao {

    @Insert
    suspend fun insert(
        treatment: Treatment
    )

    @Query(
        """
        SELECT *
        FROM treatments
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAll(): List<Treatment>

    @Query(
        """
        SELECT *
        FROM treatments
        WHERE animalId = :animalId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByAnimalId(
        animalId: String
    ): List<Treatment>

    /*
     * Calculates the total treatment cost
     * for one animal.
     */
    @Query(
        """
        SELECT COALESCE(SUM(cost), 0.0)
        FROM treatments
        WHERE animalId = :animalId
        """
    )
    suspend fun getTotalCostByAnimalId(
        animalId: String
    ): Double
}