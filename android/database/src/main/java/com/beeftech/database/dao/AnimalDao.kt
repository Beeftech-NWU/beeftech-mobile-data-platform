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

    @Query("""
        SELECT a.* FROM animals a
        INNER JOIN animal_identifiers i ON a.animalId = i.animal_id
        WHERE i.identifier_type = 'TEMPERATURE' AND i.identifier_value = :temperatureNumber AND i.valid_to IS NULL
        LIMIT 1
    """)
    suspend fun getByTemperatureNumber(temperatureNumber: String): Animal?

    @Query("""
        SELECT a.* FROM animals a
        INNER JOIN animal_identifiers i ON a.animalId = i.animal_id
        WHERE i.identifier_type = 'TAG' AND i.identifier_value = :tagNumber AND i.valid_to IS NULL
        LIMIT 1
    """)
    suspend fun getByTagNumber(tagNumber: String): Animal?

    @Query("""
        SELECT a.* FROM animals a
        INNER JOIN animal_identifiers i ON a.animalId = i.animal_id
        WHERE i.identifier_type = 'REFERENCE' AND i.identifier_value = :referenceNumber AND i.valid_to IS NULL
        LIMIT 1
    """)
    suspend fun getByReferenceNumber(referenceNumber: String): Animal?

    @Query("SELECT * FROM animals WHERE dam_id = :parentId OR sire_id = :parentId")
    suspend fun getOffspring(parentId: String): List<Animal>
}
