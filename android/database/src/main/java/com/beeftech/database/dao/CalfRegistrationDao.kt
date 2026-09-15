package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.CalfRegistration

@Dao
interface CalfRegistrationDao {

    @Insert
    suspend fun insert(calf: CalfRegistration)

    /**
     * Inserts a new calf registration, or replaces the existing row when a
     * conflict occurs on the primary key or a unique index (e.g. `animalId`
     * or `recordguid`). Used by callers that need "save or update" semantics
     * (e.g. re-saving/editing an already-registered animal) without having
     * to first delete the previous row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(calf: CalfRegistration): Long

    @Query("SELECT * FROM calf_registrations")
    suspend fun getAll(): List<CalfRegistration>

    @Query("""
        SELECT EXISTS(
            SELECT 1 
            FROM calf_registrations 
            WHERE animalId = :animalId 
            LIMIT 1
        )
    """)
    suspend fun existsByAnimalId(animalId: String): Boolean

    @Query("""
        SELECT * 
        FROM calf_registrations
        WHERE animalId = :animalId
        LIMIT 1
    """)
    suspend fun findByAnimalId(
        animalId: String
    ): CalfRegistration?

    @Query("""
        UPDATE calf_registrations
        SET syncStatus = :syncStatus, syncedat = :syncedAt
        WHERE animalId = :animalId
    """)
    suspend fun updateSyncStatus(
        animalId: String,
        syncStatus: String,
        syncedAt: Long?
    ): Int
}
