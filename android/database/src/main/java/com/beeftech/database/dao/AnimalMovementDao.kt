package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.AnimalMovement

@Dao
interface AnimalMovementDao {

    @Insert
    suspend fun insert(
        movement: AnimalMovement
    ): Long

    @Query(
        """
        SELECT *
        FROM animal_movements
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAll(): List<AnimalMovement>

    @Query(
        """
        SELECT *
        FROM animal_movements
        WHERE animalId = :animalId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByAnimalId(
        animalId: String
    ): List<AnimalMovement>

    /*
     * Protect against accidental duplicate saves, such as:
     * Add Movement Record -> Save Movement Records
     * or a quick double tap.
     *
     * This only considers records created within the supplied time window,
     * so the same legitimate movement can still be recorded later.
     */
    @Query(
        """
        SELECT *
        FROM animal_movements
        WHERE animalId = :animalId
          AND movementType = :movementType
          AND responsibleWorker = :responsibleWorker
          AND timestamp >= :minimumTimestamp
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicate(
        animalId: String,
        movementType: String,
        responsibleWorker: String,
        minimumTimestamp: Long
    ): AnimalMovement?

    /*
     * Records waiting to be uploaded to the backend.
     */
    @Query(
        """
        SELECT *
        FROM animal_movements
        WHERE syncStatus != 'SYNCED'
        ORDER BY timestamp ASC
        """
    )
    suspend fun getPendingSync():
            List<AnimalMovement>

    /*
     * Find a movement using its immutable GUID.
     */
    @Query(
        """
        SELECT *
        FROM animal_movements
        WHERE recordguid = :recordGuid
        LIMIT 1
        """
    )
    suspend fun findByRecordGuid(
        recordGuid: String
    ): AnimalMovement?

    /*
     * Called after the backend acknowledges the record.
     */
    @Query(
        """
        UPDATE animal_movements
        SET
            syncStatus = 'SYNCED',
            syncedAt = :syncedAt
        WHERE recordguid = :recordGuid
        """
    )
    suspend fun markSynced(
        recordGuid: String,
        syncedAt: Long
    ): Int

    /*
     * Explicitly return a record to PENDING when a sync
     * attempt fails.
     */
    @Query(
        """
        UPDATE animal_movements
        SET
            syncStatus = 'PENDING',
            syncedAt = NULL
        WHERE recordguid = :recordGuid
        """
    )
    suspend fun markPending(
        recordGuid: String
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM animal_movements
        WHERE syncStatus != 'SYNCED'
        """
    )
    suspend fun getPendingCount(): Int
}
