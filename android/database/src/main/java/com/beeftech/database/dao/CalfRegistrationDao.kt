package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.CalfRegistration

@Dao
interface CalfRegistrationDao {

    @Insert
    suspend fun insert(calf: CalfRegistration)

    @Query("SELECT * FROM calf_registrations")
    suspend fun getAll(): List<CalfRegistration>

    @Query("SELECT EXISTS(SELECT 1 FROM calf_registrations WHERE animalId = :animalId LIMIT 1)")
    suspend fun existsByAnimalId(animalId: String): Boolean
}

