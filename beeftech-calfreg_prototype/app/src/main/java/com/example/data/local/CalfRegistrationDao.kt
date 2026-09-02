package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CalfRegistration
import kotlinx.coroutines.flow.Flow

@Dao
interface CalfRegistrationDao {

    @Query("SELECT * FROM calf_registration ORDER BY captured_at DESC")
    fun getAllCalves(): Flow<List<CalfRegistration>>

    @Query("SELECT * FROM calf_registration WHERE sync_status = 'PENDING' ORDER BY captured_at ASC")
    fun getPendingCalves(): Flow<List<CalfRegistration>>

    @Query("SELECT * FROM calf_registration WHERE sync_status = 'PENDING' ORDER BY captured_at ASC")
    suspend fun getPendingCalvesList(): List<CalfRegistration>

    @Query("SELECT * FROM calf_registration WHERE sync_status = 'SYNCED' ORDER BY synced_at DESC, captured_at DESC")
    fun getSyncedCalves(): Flow<List<CalfRegistration>>

    @Query("SELECT * FROM calf_registration WHERE id = :id")
    fun getCalfById(id: Int): Flow<CalfRegistration?>

    @Query("SELECT * FROM calf_registration WHERE animal_id = :animalId LIMIT 1")
    suspend fun findByAnimalId(animalId: String): CalfRegistration?

    @Query("SELECT COUNT(*) FROM calf_registration WHERE sync_status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM calf_registration WHERE sync_status = 'SYNCED'")
    fun getSyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM calf_registration")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT DISTINCT dam_id FROM calf_registration WHERE dam_id IS NOT NULL AND dam_id != ''")
    fun getKnownDamIds(): Flow<List<String>>

    @Query("SELECT DISTINCT sire_id FROM calf_registration WHERE sire_id IS NOT NULL AND sire_id != ''")
    fun getKnownSireIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(calf: CalfRegistration): Long

    @Update
    suspend fun update(calf: CalfRegistration)

    @Delete
    suspend fun delete(calf: CalfRegistration)

    @Query("UPDATE calf_registration SET sync_status = 'SYNCED', synced_at = :syncedAt WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>, syncedAt: Long)

    @Query("UPDATE calf_registration SET sync_status = 'PENDING', synced_at = NULL WHERE id = :id")
    suspend fun markAsPending(id: Int)
}
