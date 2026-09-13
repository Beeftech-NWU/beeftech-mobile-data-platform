package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.LocationFeed

@Dao
interface LocationFeedDao {

    @Insert
    suspend fun insert(
        locationFeed: LocationFeed
    )

    @Query(
        """
        SELECT *
        FROM location_feed
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAll(): List<LocationFeed>

    @Query(
        """
        SELECT *
        FROM location_feed
        WHERE animalId = :animalId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByAnimalId(
        animalId: String
    ): List<LocationFeed>

    @Query(
        """
        SELECT COALESCE(SUM(rationCost), 0.0)
        FROM location_feed
        WHERE animalId = :animalId
        """
    )
    suspend fun getTotalRationCostByAnimalId(
        animalId: String
    ): Double
}