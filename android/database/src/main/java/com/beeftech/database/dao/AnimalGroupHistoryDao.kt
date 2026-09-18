package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.AnimalGroupHistory

@Dao
interface AnimalGroupHistoryDao {

    @Insert
    suspend fun insert(history: AnimalGroupHistory)

    @Delete
    suspend fun delete(history: AnimalGroupHistory): Int

    @Query("SELECT * FROM animal_group_history ORDER BY dateChange DESC")
    suspend fun getAll(): List<AnimalGroupHistory>

    @Query(
        """
        SELECT * FROM animal_group_history
        WHERE animalId = :animalId
        ORDER BY dateChange DESC
        """
    )
    suspend fun getByAnimalId(animalId: String): List<AnimalGroupHistory>

    @Query(
        """
        SELECT * FROM animal_group_history
        WHERE groupId = :groupId
        ORDER BY dateChange DESC
        """
    )
    suspend fun getByGroupId(groupId: String): List<AnimalGroupHistory>

    @Query(
        """
        SELECT * FROM animal_group_history
        WHERE animalId = :animalId AND groupId = :groupId
        ORDER BY dateChange DESC
        """
    )
    suspend fun getByAnimalAndGroup(
        animalId: String,
        groupId: String
    ): List<AnimalGroupHistory>
}
