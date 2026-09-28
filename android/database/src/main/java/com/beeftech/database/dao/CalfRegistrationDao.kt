package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.AnimalMediaEntity
import com.beeftech.database.entity.CalfRegistrationEntity
import com.beeftech.database.entity.IdentifierTypes
import kotlinx.coroutines.flow.Flow

class DuplicateTagException(val tagNumber: String) :
    IllegalStateException("Tag $tagNumber is already registered")

/** One registration joined to its animal, its active TAG and its parents' TAGs. */
data class CalfRegistrationView(
    val registrationId: String,
    val animalId: String,          // animals.animalId (UUID)
    val tagNumber: String,
    val breed: String,
    val gender: String?,
    val birthdate: Long,
    val damAnimalId: String?,
    val damTagNumber: String?,
    val sireAnimalId: String?,
    val sireTagNumber: String?,
    val birthWeightKg: Double?,
    val calvingEase: String?,
    val registrationDate: Long,
    val gpsLat: Double,
    val gpsLng: Double,
    val deviceId: String,
    val captureAt: Long,
    val photoPath: String?,
    val recordGuid: String,
    val syncStatus: String,
    val syncedAt: Long?
)

@Dao
abstract class CalfRegistrationDao {

    // --- writes (only called from registerCalf) ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertAnimal(animal: Animal)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertIdentifiers(identifiers: List<AnimalIdentifierEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertMedia(media: List<AnimalMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertRegistration(registration: CalfRegistrationEntity)

    @Query("""
        SELECT animal_id FROM animal_identifiers
        WHERE identifier_type = 'TAG' AND identifier_value = :tagNumber AND valid_to IS NULL
        LIMIT 1
    """)
    abstract suspend fun findAnimalIdByTag(tagNumber: String): String?

    /**
     * Registers a new calf atomically. Throws [DuplicateTagException] if the
     * tag is already active on another animal; nothing is written in that case.
     */
    @Transaction
    open suspend fun registerCalf(
        animal: Animal,
        identifiers: List<AnimalIdentifierEntity>,
        media: List<AnimalMediaEntity>,
        registration: CalfRegistrationEntity
    ) {
        val tag = identifiers.first { it.identifierType == IdentifierTypes.TAG }.identifierValue
        if (findAnimalIdByTag(tag) != null) throw DuplicateTagException(tag)

        insertAnimal(animal)
        insertIdentifiers(identifiers)
        if (media.isNotEmpty()) insertMedia(media)
        insertRegistration(registration)
    }

    // --- reads ---
    @Query(VIEW_SELECT + " WHERE t.identifier_value = :tagNumber")
    abstract fun getRegistrationByTag(tagNumber: String): Flow<CalfRegistrationView?>

    @Query(VIEW_SELECT + " ORDER BY a.captureAt DESC")
    abstract fun getAllRegistrationViews(): Flow<List<CalfRegistrationView>>

    @Query(VIEW_SELECT + " WHERE cr.sync_status != 'SYNCED'")
    abstract suspend fun getPendingRegistrationViews(): List<CalfRegistrationView>

    @Query("UPDATE calf_registrations SET sync_status = 'SYNCED', synced_at = :syncedAt WHERE record_guid IN (:recordGuids)")
    abstract suspend fun markSynced(recordGuids: List<String>, syncedAt: Long)

    @Query("SELECT * FROM calf_registrations WHERE dam_id = :damAnimalId")
    abstract fun getOffspringByDam(damAnimalId: String): Flow<List<CalfRegistrationEntity>>

    @Query("SELECT * FROM calf_registrations WHERE sire_id = :sireAnimalId")
    abstract fun getOffspringBySire(sireAnimalId: String): Flow<List<CalfRegistrationEntity>>

    companion object {
        private const val VIEW_SELECT = """
            SELECT
                cr.registration_id  AS registrationId,
                a.animalId          AS animalId,
                t.identifier_value  AS tagNumber,
                a.breed             AS breed,
                a.gender            AS gender,
                a.birthdate         AS birthdate,
                cr.dam_id           AS damAnimalId,
                dt.identifier_value AS damTagNumber,
                cr.sire_id          AS sireAnimalId,
                st.identifier_value AS sireTagNumber,
                cr.birth_weight_kg  AS birthWeightKg,
                cr.calving_ease     AS calvingEase,
                cr.registration_date AS registrationDate,
                a.gpsLat            AS gpsLat,
                a.gpsLng            AS gpsLng,
                a.deviceId          AS deviceId,
                a.captureAt         AS captureAt,
                (SELECT m.file_path FROM animal_media m
                  WHERE m.animal_id = a.animalId AND m.media_type = 'PHOTO'
                  ORDER BY m.created_at DESC LIMIT 1) AS photoPath,
                cr.record_guid      AS recordGuid,
                cr.sync_status      AS syncStatus,
                cr.synced_at        AS syncedAt
            FROM calf_registrations cr
            INNER JOIN animals a ON a.animalId = cr.registered_animal_id
            INNER JOIN animal_identifiers t
                    ON t.animal_id = a.animalId AND t.identifier_type = 'TAG' AND t.valid_to IS NULL
            LEFT JOIN animal_identifiers dt
                    ON dt.animal_id = cr.dam_id AND dt.identifier_type = 'TAG' AND dt.valid_to IS NULL
            LEFT JOIN animal_identifiers st
                    ON st.animal_id = cr.sire_id AND st.identifier_type = 'TAG' AND st.valid_to IS NULL
        """
    }
}
