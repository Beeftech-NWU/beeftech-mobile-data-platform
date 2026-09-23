package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalIdentifierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalIdentifierDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentifier(identifier: AnimalIdentifierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentifiers(identifiers: List<AnimalIdentifierEntity>)

    // Fetch all active/valid identifiers for a specific animal
    @Query("SELECT * FROM animal_identifiers WHERE animal_id = :animalId AND valid_to IS NULL")
    fun getActiveIdentifiersForAnimal(animalId: String): Flow<List<AnimalIdentifierEntity>>

    // Fetch identifier history for a specific animal
    @Query("SELECT * FROM animal_identifiers WHERE animal_id = :animalId ORDER BY valid_from DESC")
    fun getAllIdentifiersForAnimal(animalId: String): Flow<List<AnimalIdentifierEntity>>

    // Search animal_id by a specific tag or identifier value (e.g., RFID tag number)
    @Query("""
        SELECT animal_id FROM animal_identifiers 
        WHERE identifier_type = :type AND identifier_value = :value AND valid_to IS NULL 
        LIMIT 1
    """)
    suspend fun findAnimalIdByIdentifier(type: String, value: String): String?

    @Delete
    suspend fun deleteIdentifier(identifier: AnimalIdentifierEntity)
}
