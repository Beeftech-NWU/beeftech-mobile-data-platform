package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beeftech.database.entity.Animal

@Dao
interface AnimalDao {
    @Insert
    suspend fun insert(animal: Animal): Long

    @Update
    suspend fun update(animal: Animal): Int

    @Delete
    suspend fun delete(animal: Animal): Int

    @Query("SELECT * FROM animals")
    suspend fun getAll(): List<Animal>

    @Query("SELECT * FROM animals WHERE animalId = :animalId")
    suspend fun getById(animalId: String): Animal?

    @Query("SELECT * FROM animals WHERE temperatureNumber = :temperatureNumber")
    suspend fun getByTemperatureNumber(temperatureNumber: String): Animal?

    @Query("SELECT * FROM animals WHERE tagNumber = :tagNumber")
    suspend fun getByTagNumber(tagNumber: String): Animal?

    @Query("SELECT * FROM animals WHERE referenceNumber = :referenceNumber")
    suspend fun getByReferenceNumber(referenceNumber: String): Animal?

    @Query("SELECT * FROM animals WHERE damId = :parentId OR sireId = :parentId")
    suspend fun getOffspring(parentId: String): List<Animal>
}
