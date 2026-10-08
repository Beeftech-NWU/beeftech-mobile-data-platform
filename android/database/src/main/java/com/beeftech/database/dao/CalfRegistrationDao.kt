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
    val hideColour: String?,
    val brandMark: String?,
    val birthdate: Long,
    val damAnimalId: String?,
    val damTagNumber: String?,
    val sireAnimalId: String?,
    val sireTagNumber: String?,
    val birthWeightKg: Double?,
    val calvingEase: String?,
    val ageClass: String?,
    val bodyCondition: String?,
    val conformity: String?,
    val processProof: String?,
    val implantProof: String?,
    val oldTagNumber: String?,
    val referenceNumber: String?,
    val registrationDate: Long,
    val gpsLat: Double,
    val gpsLng: Double,
    val deviceId: String,
    val captureAt: Long,
    val photoPath: String?,
    val recordGuid: String,
    val syncStatus: String,
    val syncedAt: Long?,
    val syncError: String?
)

/** A registered animal that can be picked as a dam or sire: its active TAG and breed. */
data class ParentCandidate(
    val tagNumber: String,
    val breed: String
)

/** A calf photo on the device that has not reached the server yet. */
data class PhotoUpload(
    val mediaId: String,
    val tagNumber: String,
    val filePath: String
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

    @Query(VIEW_SELECT + " WHERE cr.sync_status = 'PENDING'")
    abstract suspend fun getPendingRegistrationViews(): List<CalfRegistrationView>

    @Query("""
        SELECT t.identifier_value AS tagNumber, a.breed AS breed
        FROM animals a
        INNER JOIN animal_identifiers t
                ON t.animal_id = a.animalId AND t.identifier_type = 'TAG' AND t.valid_to IS NULL
        WHERE a.gender = :gender
        ORDER BY t.identifier_value
    """)
    abstract suspend fun getParentCandidates(gender: String): List<ParentCandidate>

    @Query("UPDATE calf_registrations SET sync_status = 'SYNCED', synced_at = :syncedAt, sync_error = NULL, sync_attempts = 0 WHERE record_guid IN (:recordGuids)")
    abstract suspend fun markSynced(recordGuids: List<String>, syncedAt: Long)

    /**
     * The server explicitly rejected this record. Stores its message and counts the
     * attempt; once [maxAttempts] is reached the record becomes REJECTED and stops
     * being retried until [requeueRejected].
     */
    @Query("""
        UPDATE calf_registrations
        SET sync_error = :message,
            sync_attempts = sync_attempts + 1,
            sync_status = CASE WHEN sync_attempts + 1 >= :maxAttempts THEN 'REJECTED' ELSE sync_status END
        WHERE record_guid = :recordGuid AND sync_status = 'PENDING'
    """)
    abstract suspend fun recordRejection(recordGuid: String, message: String, maxAttempts: Int)

    /**
     * Photos still to upload. Only calves the server already has: the upload route needs
     * the record to exist.
     */
    @Query("""
        SELECT m.media_id AS mediaId, t.identifier_value AS tagNumber, m.file_path AS filePath
        FROM animal_media m
        INNER JOIN calf_registrations cr ON cr.registered_animal_id = m.animal_id
        INNER JOIN animal_identifiers t
                ON t.animal_id = m.animal_id AND t.identifier_type = 'TAG' AND t.valid_to IS NULL
        WHERE m.media_type = 'PHOTO' AND m.upload_status = 'PENDING' AND cr.sync_status = 'SYNCED'
        ORDER BY m.created_at
    """)
    abstract suspend fun getPhotosAwaitingUpload(): List<PhotoUpload>

    @Query("UPDATE animal_media SET upload_status = 'UPLOADED', upload_error = NULL WHERE media_id = :mediaId")
    abstract suspend fun markPhotoUploaded(mediaId: String)

    @Query("UPDATE animal_media SET upload_status = 'FAILED', upload_error = :error WHERE media_id = :mediaId")
    abstract suspend fun markPhotoUploadFailed(mediaId: String, error: String)

    /** Manual retry: give every REJECTED record a fresh set of attempts. */
    @Query("UPDATE calf_registrations SET sync_status = 'PENDING', sync_attempts = 0, sync_error = NULL WHERE sync_status = 'REJECTED'")
    abstract suspend fun requeueRejected()

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
                a.hideColour        AS hideColour,
                a.brandMark         AS brandMark,
                a.birthdate         AS birthdate,
                cr.dam_id           AS damAnimalId,
                dt.identifier_value AS damTagNumber,
                cr.sire_id          AS sireAnimalId,
                st.identifier_value AS sireTagNumber,
                cr.birth_weight_kg  AS birthWeightKg,
                cr.calving_ease     AS calvingEase,
                cr.age_class        AS ageClass,
                cr.body_condition   AS bodyCondition,
                cr.conformity       AS conformity,
                cr.process_proof    AS processProof,
                cr.implant_proof    AS implantProof,
                (SELECT i.identifier_value FROM animal_identifiers i
                  WHERE i.animal_id = a.animalId AND i.identifier_type = 'OLD_TAG' AND i.valid_to IS NULL
                  ORDER BY i.valid_from DESC LIMIT 1) AS oldTagNumber,
                (SELECT i.identifier_value FROM animal_identifiers i
                  WHERE i.animal_id = a.animalId AND i.identifier_type = 'REFERENCE' AND i.valid_to IS NULL
                  ORDER BY i.valid_from DESC LIMIT 1) AS referenceNumber,
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
                cr.synced_at        AS syncedAt,
                cr.sync_error       AS syncError
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
