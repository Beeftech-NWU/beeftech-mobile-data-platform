package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalOwnershipEntity
import kotlinx.coroutines.flow.Flow

data class OwnerHeadCount(
    val ownerName: String,
    val headCount: Int
)

data class AnimalLocationAndOwner(
    val animalId: String,
    val ownerName: String,
    val destinationFarmId: String?,
    val destinationPenId: String?
)

@Dao
interface AnimalOwnershipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnership(ownership: AnimalOwnershipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerships(ownerships: List<AnimalOwnershipEntity>)

    // Get current active ownership records for a specific animal
    @Query("SELECT * FROM animal_ownerships WHERE animal_id = :animalId AND end_date IS NULL")
    fun getActiveOwnershipByAnimalId(animalId: String): Flow<List<AnimalOwnershipEntity>>

    // Acceptance Criteria Query: "How many head does this farmer own today?"
    @Query("""
        SELECT owner_name AS ownerName, COUNT(DISTINCT animal_id) AS headCount 
        FROM animal_ownerships 
        WHERE owner_name = :ownerName AND end_date IS NULL
        GROUP BY owner_name
    """)
    fun getCurrentHeadCountByOwner(ownerName: String): Flow<OwnerHeadCount?>

    // Acceptance Criteria Query: "Where are the animals owned by this farmer today?"
    @Query("""
        SELECT 
            o.animal_id AS animalId, 
            o.owner_name AS ownerName, 
            m.destination_farm_id AS destinationFarmId, 
            m.destination_pen_id AS destinationPenId 
        FROM animal_ownerships o
        LEFT JOIN animal_movements m ON o.animal_id = m.animal_id
        WHERE o.owner_name = :ownerName 
          AND o.end_date IS NULL
          AND m.movement_id = (
              SELECT movement_id FROM animal_movements 
              WHERE animal_id = o.animal_id 
              ORDER BY movement_date DESC LIMIT 1
          )
    """)
    fun getCurrentLocationsByOwner(ownerName: String): Flow<List<AnimalLocationAndOwner>>
}
