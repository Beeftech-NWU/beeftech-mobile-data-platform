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
    ): Long

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

    @Query(
        """
        SELECT *
        FROM treatments
        WHERE syncStatus != 'SYNCED'
        ORDER BY timestamp ASC
        """
    )
    suspend fun getPendingSync():
            List<Treatment>

    @Query(
        """
        SELECT *
        FROM treatments
        WHERE recordguid = :recordGuid
        LIMIT 1
        """
    )
    suspend fun findByRecordGuid(
        recordGuid: String
    ): Treatment?

    @Query(
        """
        UPDATE treatments
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

    @Query(
        """
        UPDATE treatments
        SET
            syncStatus = 'PENDING',
            syncedAt = NULL
        WHERE recordguid = :recordGuid
        """
    )
    suspend fun markPending(
        recordGuid: String
    ): Int

    /*
     * Duplicate protection.
     *
     * This prevents Add Treatment followed immediately by another
     * save/double tap from creating two identical records.
     */
    @Query(
        """
        SELECT *
        FROM treatments
        WHERE animalId = :animalId
          AND disease = :disease
          AND treatmentName = :treatmentName
          AND batchNumber = :batchNumber
          AND volumeUsed = :volumeUsed
          AND cost = :cost
          AND timestamp >= :minimumTimestamp
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicate(
        animalId: String,
        disease: String,
        treatmentName: String,
        batchNumber: String,
        volumeUsed: String,
        cost: Double,
        minimumTimestamp: Long
    ): Treatment?

    @Query(
        """
        SELECT COUNT(*)
        FROM treatments
        WHERE syncStatus != 'SYNCED'
        """
    )
    suspend fun getPendingCount(): Int

    /*
     * Calculates the total treatment cost for one animal.
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