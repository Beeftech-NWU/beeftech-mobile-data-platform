package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: AnimalMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<AnimalMediaEntity>)

    // Retrieve all media assets (photos/videos) linked to a specific animal
    @Query("SELECT * FROM animal_media WHERE animal_id = :animalId ORDER BY created_at DESC")
    fun getMediaForAnimal(animalId: String): Flow<List<AnimalMediaEntity>>

    // Filter media by type (e.g., "PHOTO" or "VIDEO")
    @Query("SELECT * FROM animal_media WHERE animal_id = :animalId AND media_type = :mediaType ORDER BY created_at DESC")
    fun getMediaForAnimalByType(animalId: String, mediaType: String): Flow<List<AnimalMediaEntity>>

    @Delete
    suspend fun deleteMedia(media: AnimalMediaEntity)
}
