package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalWeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalWeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weightRecord: AnimalWeightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecords(weightRecords: List<AnimalWeightEntity>)

    // Get the complete weight history for an animal sorted by date
    @Query("SELECT * FROM animal_weights WHERE animal_id = :animalId ORDER BY weigh_date DESC")
    fun getWeightHistoryForAnimal(animalId: String): Flow<List<AnimalWeightEntity>>

    // Get the most recent weight reading for an animal
    @Query("SELECT * FROM animal_weights WHERE animal_id = :animalId ORDER BY weigh_date DESC LIMIT 1")
    fun getLatestWeightForAnimal(animalId: String): Flow<AnimalWeightEntity?>

    @Delete
    suspend fun deleteWeightRecord(weightRecord: AnimalWeightEntity)
}
