package com.beeftech.calfregistration.fakes

import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.dao.ParentCandidate
import com.beeftech.database.dao.PhotoUpload
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.AnimalMediaEntity
import com.beeftech.database.entity.Breed
import com.beeftech.database.entity.CalfRegistrationEntity
import com.beeftech.database.entity.Device
import com.beeftech.database.entity.IdentifierTypes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory fake of [CalfRegistrationDao]. Only the primitive inserts and queries
 * are overridden, so the real [registerCalf] (including its duplicate-tag check)
 * runs unchanged in unit tests. There is no rollback, which is fine because the
 * duplicate check happens before anything is written.
 */
class FakeCalfRegistrationDao : CalfRegistrationDao() {

    val animals = mutableListOf<Animal>()
    val identifiers = mutableListOf<AnimalIdentifierEntity>()
    val media = mutableListOf<AnimalMediaEntity>()
    val registrations = mutableListOf<CalfRegistrationEntity>()
    val breeds = mutableListOf<Breed>()
    val devices = mutableListOf<Device>()

    override suspend fun insertAnimal(animal: Animal) {
        animals += animal
    }

    override suspend fun insertIdentifiers(identifiers: List<AnimalIdentifierEntity>) {
        this.identifiers += identifiers
    }

    override suspend fun insertMedia(media: List<AnimalMediaEntity>) {
        this.media += media
    }

    override suspend fun insertRegistration(registration: CalfRegistrationEntity) {
        registrations += registration
    }

    // Mirrors the INSERT OR IGNORE / lookup queries behind importVerifiedServerCalf.
    override suspend fun insertRemoteBreed(value: Breed): Long =
        if (breeds.any { it.breedId == value.breedId }) -1L else { breeds += value; breeds.size.toLong() }

    override suspend fun insertRemoteDevice(value: Device): Long =
        if (devices.any { it.deviceId == value.deviceId }) -1L else { devices += value; devices.size.toLong() }

    override suspend fun existingBreedId(breed: String): String? =
        breeds.firstOrNull { it.breedId == breed || it.name == breed }?.breedId

    override suspend fun remoteAnimalExists(animalId: String): String? =
        animals.firstOrNull { it.animalId == animalId }?.animalId

    override suspend fun importedRegistrationByGuid(guid: String): String? =
        registrations.firstOrNull { it.recordGuid == guid }?.registeredAnimalId

    override suspend fun existingTagOwner(tag: String): String? =
        identifiers.firstOrNull {
            it.identifierType == IdentifierTypes.TAG && it.validTo == null &&
                it.identifierValue.equals(tag, ignoreCase = true)
        }?.animalId

    /** When set, [findAnimalIdByTag] throws, simulating a database failure before the write. */
    var lookupFailure: Exception? = null

    override suspend fun findAnimalIdByTag(tagNumber: String): String? {
        lookupFailure?.let { throw it }
        return activeTag { it == tagNumber }
    }

    override suspend fun getParentCandidates(gender: String): List<ParentCandidate> =
        animals.filter { it.gender == gender }
            .mapNotNull { animal -> tagOf(animal.animalId)?.let { ParentCandidate(it, animal.breed) } }
            .sortedBy { it.tagNumber }

    private fun activeTag(match: (String) -> Boolean): String? =
        identifiers.firstOrNull {
            it.identifierType == IdentifierTypes.TAG && it.validTo == null && match(it.identifierValue)
        }?.animalId

    private fun tagOf(animalId: String?): String? =
        identifiers.firstOrNull {
            it.animalId == animalId && it.identifierType == IdentifierTypes.TAG && it.validTo == null
        }?.identifierValue

    private fun identifierOf(animalId: String, type: String): String? =
        identifiers.firstOrNull {
            it.animalId == animalId && it.identifierType == type && it.validTo == null
        }?.identifierValue

    private fun view(registration: CalfRegistrationEntity): CalfRegistrationView? {
        val animal = animals.firstOrNull { it.animalId == registration.registeredAnimalId } ?: return null
        val tag = tagOf(animal.animalId) ?: return null
        return CalfRegistrationView(
            registrationId = registration.registrationId,
            animalId = animal.animalId,
            tagNumber = tag,
            breed = animal.breed,
            gender = animal.gender,
            hideColour = animal.hideColour,
            brandMark = animal.brandMark,
            birthdate = animal.birthdate,
            damAnimalId = registration.damId,
            damTagNumber = tagOf(registration.damId),
            sireAnimalId = registration.sireId,
            sireTagNumber = tagOf(registration.sireId),
            birthWeightKg = registration.birthWeightKg,
            calvingEase = registration.calvingEase,
            ageClass = registration.ageClass,
            bodyCondition = registration.bodyCondition,
            conformity = registration.conformity,
            processProof = registration.processProof,
            implantProof = registration.implantProof,
            oldTagNumber = identifierOf(animal.animalId, IdentifierTypes.OLD_TAG),
            referenceNumber = identifierOf(animal.animalId, IdentifierTypes.REFERENCE),
            registrationDate = registration.registrationDate,
            gpsLat = animal.gpsLat,
            gpsLng = animal.gpsLng,
            deviceId = animal.deviceId,
            captureAt = animal.captureAt,
            photoPath = media.lastOrNull { it.animalId == animal.animalId && it.mediaType == "PHOTO" }?.filePath,
            recordGuid = registration.recordGuid,
            syncStatus = registration.syncStatus,
            syncedAt = registration.syncedAt,
            syncError = registration.syncError
        )
    }

    override fun getRegistrationByTag(tagNumber: String): Flow<CalfRegistrationView?> =
        flowOf(registrations.mapNotNull { view(it) }.firstOrNull { it.tagNumber == tagNumber })

    override fun getAllRegistrationViews(): Flow<List<CalfRegistrationView>> =
        flowOf(registrations.mapNotNull { view(it) }.sortedByDescending { it.captureAt })

    override suspend fun getPendingRegistrationViews(): List<CalfRegistrationView> =
        registrations.filter { it.syncStatus == "PENDING" }.mapNotNull { view(it) }

    override suspend fun markSynced(recordGuids: List<String>, syncedAt: Long) {
        registrations.replaceAll {
            if (it.recordGuid in recordGuids) {
                it.copy(syncStatus = "SYNCED", syncedAt = syncedAt, syncError = null, syncAttempts = 0)
            } else {
                it
            }
        }
    }

    override suspend fun recordRejection(recordGuid: String, message: String, maxAttempts: Int) {
        registrations.replaceAll {
            if (it.recordGuid == recordGuid && it.syncStatus == "PENDING") {
                val attempts = it.syncAttempts + 1
                it.copy(
                    syncError = message,
                    syncAttempts = attempts,
                    syncStatus = if (attempts >= maxAttempts) "REJECTED" else it.syncStatus
                )
            } else {
                it
            }
        }
    }

    override suspend fun getPhotosAwaitingUpload(): List<PhotoUpload> =
        media.filter { it.mediaType == "PHOTO" && it.uploadStatus == "PENDING" }
            .mapNotNull { m ->
                val synced = registrations.any { it.registeredAnimalId == m.animalId && it.syncStatus == "SYNCED" }
                val tag = tagOf(m.animalId)
                if (synced && tag != null) PhotoUpload(m.mediaId, tag, m.filePath) else null
            }

    override suspend fun markPhotoUploaded(mediaId: String) {
        media.replaceAll { if (it.mediaId == mediaId) it.copy(uploadStatus = "UPLOADED", uploadError = null) else it }
    }

    override suspend fun markPhotoUploadFailed(mediaId: String, error: String) {
        media.replaceAll { if (it.mediaId == mediaId) it.copy(uploadStatus = "FAILED", uploadError = error) else it }
    }

    override suspend fun requeueRejected() {
        registrations.replaceAll {
            if (it.syncStatus == "REJECTED") it.copy(syncStatus = "PENDING", syncAttempts = 0, syncError = null) else it
        }
    }

    override fun getOffspringByDam(damAnimalId: String): Flow<List<CalfRegistrationEntity>> =
        flowOf(registrations.filter { it.damId == damAnimalId })

    override fun getOffspringBySire(sireAnimalId: String): Flow<List<CalfRegistrationEntity>> =
        flowOf(registrations.filter { it.sireId == sireAnimalId })

    fun tagIdentifierCount(): Int = identifiers.count { it.identifierType == IdentifierTypes.TAG }
}
