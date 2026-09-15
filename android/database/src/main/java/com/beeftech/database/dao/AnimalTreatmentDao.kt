package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalTreatmentEntity


@Dao
interface AnimalTreatmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTreatment(treatment: AnimalTreatmentEntity)

    @Query("SELECT * FROM animal_treatments WHERE animalGuid = :animalGuid ORDER BY timestamp DESC")
    suspend fun getTreatmentsForAnimal(animalGuid: String): List<AnimalTreatmentEntity>
}