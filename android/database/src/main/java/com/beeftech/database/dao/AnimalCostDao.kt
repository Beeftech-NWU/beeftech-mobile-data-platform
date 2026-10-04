package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.AnimalCost

data class CostTypeTotal(
    val costType: String,
    val total: Double
)

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

    @Query(
        """
        SELECT costType, COALESCE(SUM(amount), 0.0) AS total
        FROM animal_costs
        WHERE animalId = :animalId
        GROUP BY costType
        """
    )
    suspend fun getTotalsByType(
        animalId: String
    ): List<CostTypeTotal>

    @Query(
        """
        SELECT *
        FROM animal_costs
        WHERE record_guid = :recordGuid
        LIMIT 1
        """
    )
    suspend fun findByRecordGuid(
        recordGuid: String
    ): AnimalCost?

    /*
     * Rows that still need to reach the server, including ones recorded
     * before costs could sync at all.
     */
    @Query(
        """
        SELECT *
        FROM animal_costs
        WHERE sync_status != 'SYNCED'
        ORDER BY timestamp ASC
        """
    )
    suspend fun getUnsynced(): List<AnimalCost>

    @Query(
        """
        UPDATE animal_costs
        SET sync_status = 'SYNCED',
            synced_at = :syncedAt
        WHERE record_guid = :recordGuid
        """
    )
    suspend fun markSynced(
        recordGuid: String,
        syncedAt: Long
    ): Int
}
