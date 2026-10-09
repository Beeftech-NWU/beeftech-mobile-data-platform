package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.beeftech.database.entity.FarmerAnimalLink
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FarmerAnimalLinkDao {
    @Query("SELECT * FROM farmer_animal_links WHERE farmer_id = :farmerId AND effective_to IS NULL ORDER BY effective_from DESC")
    abstract suspend fun activeForFarmer(farmerId: String): List<FarmerAnimalLink>

    @Query("SELECT * FROM farmer_animal_links WHERE effective_to IS NULL")
    abstract suspend fun allActive(): List<FarmerAnimalLink>

    @Query("SELECT * FROM farmer_animal_links WHERE animal_id = :animalId AND effective_to IS NULL LIMIT 1")
    abstract suspend fun activeForAnimal(animalId: String): FarmerAnimalLink?

    @Query("UPDATE farmer_animal_links SET effective_to = :endedAt, sync_status = 'PENDING' WHERE animal_id = :animalId AND effective_to IS NULL")
    protected abstract suspend fun endActive(animalId: String, endedAt: Long)

    @Insert
    protected abstract suspend fun insert(link: FarmerAnimalLink)

    /**
     * End only the exact active assignment chosen by the user. Keep its identity and
     * original effectiveFrom so the server can acknowledge the closure, or confirm
     * that this never-uploaded test link belongs in local-only history.
     *
     * Returning 0 prevents a stale confirmation from ending a newer transfer.
     */
    @Query("""
        UPDATE farmer_animal_links
        SET effective_to = :endedAt, sync_status = 'PENDING'
        WHERE record_guid = :recordGuid AND farmer_id = :farmerId
          AND animal_id = :animalId AND effective_to IS NULL
          AND effective_from <= :endedAt
          AND sync_status != 'LOCAL_HISTORY'
    """)
    abstract suspend fun endExactActiveAssignment(
        recordGuid: String, farmerId: String, animalId: String, endedAt: Long
    ): Int

    // LOCAL_HISTORY means the server confirmed this ended link was never stored.
    // Keep it locally, but never retry it or count it as an upload.
    @Query("SELECT * FROM farmer_animal_links WHERE sync_status NOT IN ('SYNCED','LOCAL_HISTORY') ORDER BY effective_from ASC, CASE WHEN effective_to IS NOT NULL THEN 0 ELSE 1 END")
    abstract suspend fun pendingUploads(): List<FarmerAnimalLink>

    @Query("SELECT COUNT(*) FROM farmer_animal_links WHERE sync_status NOT IN ('SYNCED','LOCAL_HISTORY')")
    abstract fun observePendingUploadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM farmer_animal_links WHERE sync_status = 'LOCAL_HISTORY'")
    abstract fun observeLocalHistoryCount(): Flow<Int>

    /** Only an exact unchanged, ended revision can become local-only history. */
    @Query("""UPDATE farmer_animal_links SET sync_status = 'LOCAL_HISTORY'
        WHERE link_id = :linkId AND record_guid = :recordGuid
        AND effective_from = :from AND effective_to = :to
        AND effective_to IS NOT NULL AND sync_status NOT IN ('SYNCED','LOCAL_HISTORY')""")
    abstract suspend fun markLocalHistoryIfUnchanged(linkId: String, recordGuid: String, from: Long, to: Long): Int

    @Query("UPDATE farmer_animal_links SET sync_status = 'SYNCED' WHERE sync_status NOT IN ('SYNCED','LOCAL_HISTORY') AND link_id = :linkId AND record_guid = :recordGuid AND effective_from = :from AND ((effective_to IS NULL AND :to IS NULL) OR effective_to = :to)")
    abstract suspend fun markSyncedIfUnchanged(linkId: String, recordGuid: String, from: Long, to: Long?): Int

    /** Idempotent for same owner, preserving reassignment history. */
    @Transaction
    open suspend fun assign(farmerId: String, animalId: String): Boolean {
        val existing = activeForAnimal(animalId)
        if (existing?.farmerId == farmerId) return false
        val now = System.currentTimeMillis()
        if (existing != null) endActive(animalId, now)
        insert(FarmerAnimalLink(farmerId = farmerId, animalId = animalId, effectiveFrom = now))
        return true
    }
}
