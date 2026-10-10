package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalMovementEntity

@Dao
interface AnimalMovementDao {

    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insert(
        movement:
            AnimalMovementEntity
    )


    @Query(
        """
        SELECT *
        FROM animal_movements
        ORDER BY movement_date DESC
        """
    )
    suspend fun getAll():
            List<AnimalMovementEntity>


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


    /*
     * Legacy exact lookup retained for compatibility.
     */
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

    @Query(
        """
        SELECT *
        FROM animal_movements
        WHERE record_guid = :recordGuid
        LIMIT 1
        """
    )
    suspend fun findByRecordGuid(
        recordGuid: String
    ): AnimalMovementEntity?


    @Query(
        """
        UPDATE animal_movements
        SET
            sync_status = 'SYNCED',
            synced_at = :syncedAt
        WHERE record_guid = :recordGuid
        """
    )
    suspend fun markSynced(
        recordGuid: String,
        syncedAt: Long
    ): Int


    @Query(
        """
        UPDATE animal_movements
        SET
            sync_status = 'PENDING',
            synced_at = NULL
        WHERE record_guid = :recordGuid
        """
    )
    suspend fun markPending(
        recordGuid: String
    ): Int
}
