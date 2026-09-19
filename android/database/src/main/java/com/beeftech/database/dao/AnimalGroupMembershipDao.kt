package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.AnimalGroupMembership

@Dao
interface AnimalGroupMembershipDao {

    @Insert
    suspend fun insert(history: AnimalGroupMembership)

    @Delete
    suspend fun delete(history: AnimalGroupMembership): Int

    @Query("SELECT * FROM animal_group_memberships ORDER BY dateJoined DESC")
    suspend fun getAll(): List<AnimalGroupMembership>

    @Query(
        """
        SELECT * FROM animal_group_memberships
        WHERE animalId = :animalId
        ORDER BY dateJoined DESC
        """
    )
    suspend fun getByAnimalId(animalId: String): List<AnimalGroupMembership>

    @Query(
        """
        SELECT * FROM animal_group_memberships
        WHERE animalId = :animalId AND dateLeft IS NULL
        ORDER BY dateJoined DESC
        """
    )
    suspend fun getCurrentByAnimalId(
        animalId: String
    ): List<AnimalGroupMembership>

    @Query(
        """
        SELECT * FROM animal_group_memberships
        WHERE groupId = :groupId
        ORDER BY dateJoined DESC
        """
    )
    suspend fun getByGroupId(groupId: String): List<AnimalGroupMembership>

    @Query(
        """
        SELECT * FROM animal_group_memberships
        WHERE animalId = :animalId AND groupId = :groupId
        ORDER BY dateJoined DESC
        """
    )
    suspend fun getByAnimalAndGroup(
        animalId: String,
        groupId: String
    ): List<AnimalGroupMembership>
}
