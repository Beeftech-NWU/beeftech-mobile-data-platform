package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beeftech.database.entity.AnimalGroup

@Dao
interface AnimalGroupDao {

    @Insert
    suspend fun insert(group: AnimalGroup): Long

    @Update
    suspend fun update(group: AnimalGroup)

    @Delete
    suspend fun delete(group: AnimalGroup)

    @Query("SELECT * FROM animal_groups")
    suspend fun getAll(): List<AnimalGroup>

    @Query("SELECT * FROM animal_groups WHERE animalGroupId = :groupId")
    suspend fun getById(groupId: String): AnimalGroup?

    @Query("SELECT * FROM animal_groups WHERE groupName = :groupName")
    suspend fun getByName(groupName: String): AnimalGroup?
}
