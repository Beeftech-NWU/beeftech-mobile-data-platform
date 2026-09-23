package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.CalfRegistrationEntity
import kotlinx.coroutines.flow.Flow

data class CalfWithParents(
    val registrationId: String,
    val registeredAnimalId: String,
    val damId: String?,
    val sireId: String?,
    val birthWeightKg: Double?,
    val calvingEase: String?,
    val registrationDate: String
) {
    val animalId: String get() = registeredAnimalId
}

@Dao
interface CalfRegistrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalfRegistration(registration: CalfRegistrationEntity)

    // Acceptance Criteria Query: "A join from calf registration to animal returns correct rows"
    @Query("""
        SELECT 
            cr.registration_id AS registrationId,
            cr.registered_animal_id AS registeredAnimalId,
            cr.dam_id AS damId,
            cr.sire_id AS sireId,
            cr.birth_weight_kg AS birthWeightKg,
            cr.calving_ease AS calvingEase,
            cr.registration_date AS registrationDate
        FROM calf_registrations cr
        INNER JOIN animals a ON cr.registered_animal_id = a.animalId
        WHERE cr.registered_animal_id = :animalId
    """)
    fun getCalfRegistrationDetails(animalId: String): Flow<CalfWithParents?>

    @Query("SELECT * FROM calf_registrations WHERE dam_id = :damId")
    fun getOffspringByDam(damId: String): Flow<List<CalfRegistrationEntity>>

    @Query("SELECT * FROM calf_registrations WHERE sire_id = :sireId")
    fun getOffspringBySire(sireId: String): Flow<List<CalfRegistrationEntity>>
}
