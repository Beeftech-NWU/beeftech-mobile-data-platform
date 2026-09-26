package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.CostSource
import com.beeftech.database.entity.Treatment

@Dao
interface TreatmentDao {

    @Insert
    suspend fun insert(
        treatment: Treatment
    ): Long

    /*
     * Inserts the derived cost row for a treatment.
     * IGNORE + the unique (source_entity, source_record_id) index
     * make this safe to call more than once.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDerivedCost(
        cost: AnimalCost
    ): Long

    /*
     * Saves a treatment and, when it has a cost, its matching
     * animal_costs row in one transaction (remediation plan, Phase 5).
     *
     * Any future edit/delete of a treatment must update/delete
     * the derived cost row with source_record_id = recordguid.
     */
    @Transaction
    suspend fun insertWithCost(
        treatment: Treatment
    ): Long {
        val id = insert(treatment)
        if (treatment.cost > 0) {
            insertDerivedCost(
                AnimalCost(
                    animalId = treatment.animalId,
                    costType = "TREATMENT",
                    amount = treatment.cost,
                    description = treatment.treatmentName,
                    gpsLat = treatment.gpsLat,
                    gpsLng = treatment.gpsLng,
                    timestamp = treatment.timestamp,
                    sourceEntity = CostSource.TREATMENT,
                    sourceRecordId = treatment.recordguid
                )
            )
        }
        return id
    }

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
