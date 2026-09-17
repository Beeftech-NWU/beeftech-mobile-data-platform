package com.beeftech.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalMortalityEntity


@Dao
interface AnimalMortalityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMortality(mortality: AnimalMortalityEntity)

    @Query("SELECT * FROM animal_mortalities WHERE animalGuid = :animalGuid")
    suspend fun getMortalityForAnimal(animalGuid: String): LiveData<AnimalMortalityEntity?>
}