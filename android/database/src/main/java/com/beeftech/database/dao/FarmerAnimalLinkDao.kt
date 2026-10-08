package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.beeftech.database.entity.FarmerAnimalLink

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
