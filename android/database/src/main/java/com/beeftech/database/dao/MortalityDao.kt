package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.Mortality

@Dao
interface MortalityDao {

    @Insert
    suspend fun insert(
        mortality: Mortality
    )

    @Query(
        """
        SELECT *
        FROM mortalities
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAll(): List<Mortality>

    @Query(
        """
        SELECT *
        FROM mortalities
        WHERE animalId = :animalId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByAnimalId(
        animalId: String
    ): List<Mortality>

    @Query(
        """
        SELECT *
        FROM mortalities
        WHERE record_guid = :recordGuid
        LIMIT 1
        """
    )
    suspend fun findByRecordGuid(
        recordGuid: String
    ): Mortality?

    /*
     * Rows that still need to reach the server, including ones recorded
     * before mortalities could sync at all.
     */
    @Query(
        """
        SELECT *
        FROM mortalities
        WHERE sync_status != 'SYNCED'
        ORDER BY timestamp ASC
        """
    )
    suspend fun getUnsynced(): List<Mortality>

    @Query(
        """
        UPDATE mortalities
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
