package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalWeightEntity

@Dao
interface AnimalWeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: AnimalWeightEntity)

    @Query("SELECT * FROM animal_weights WHERE animal_id = :animalId ORDER BY weighed_at DESC")
    suspend fun getWeightHistoryForAnimal(animalId: String): List<AnimalWeightEntity>

    @Query("SELECT * FROM animal_weights WHERE animal_id = :animalId ORDER BY weighed_at DESC LIMIT 1")
    suspend fun getLatestWeightForAnimal(animalId: String): AnimalWeightEntity?
}