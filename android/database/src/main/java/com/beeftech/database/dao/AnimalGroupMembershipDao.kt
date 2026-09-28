package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalGroupMembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalGroupMembershipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: AnimalGroupMembershipEntity)

    @Query("SELECT * FROM animal_group_memberships WHERE animal_id = :animalId ORDER BY joined_at DESC")
    suspend fun getGroupHistoryForAnimal(animalId: String): List<AnimalGroupMembershipEntity>

    @Query("SELECT * FROM animal_group_memberships WHERE animal_id = :animalId ORDER BY joined_at DESC")
    fun getGroupHistoryForAnimalFlow(animalId: String): Flow<List<AnimalGroupMembershipEntity>>

    @Query("SELECT * FROM animal_group_memberships WHERE animal_id = :animalId AND left_at IS NULL LIMIT 1")
    suspend fun getActiveGroupMembership(animalId: String): AnimalGroupMembershipEntity?

    @Query("SELECT * FROM animal_group_memberships WHERE animal_id = :animalId AND left_at IS NULL LIMIT 1")
    fun getActiveGroupMembershipFlow(animalId: String): Flow<AnimalGroupMembershipEntity?>

    @Query("UPDATE animal_group_memberships SET left_at = :leftAt WHERE animal_id = :animalId AND left_at IS NULL")
    suspend fun closeOpenMemberships(animalId: String, leftAt: Long)
}
