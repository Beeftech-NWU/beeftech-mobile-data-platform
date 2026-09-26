package com.beeftech.calfregistration.fakes

import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.AnimalMediaEntity
import com.beeftech.database.entity.CalfRegistrationEntity
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

    override suspend fun findAnimalIdByTag(tagNumber: String): String? =
        activeTag { it == tagNumber }

    private fun activeTag(match: (String) -> Boolean): String? =
        identifiers.firstOrNull {
            it.identifierType == IdentifierTypes.TAG && it.validTo == null && match(it.identifierValue)
        }?.animalId

    private fun tagOf(animalId: String?): String? =
        identifiers.firstOrNull {
            it.animalId == animalId && it.identifierType == IdentifierTypes.TAG && it.validTo == null
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
            birthdate = animal.birthdate,
            damAnimalId = registration.damId,
            damTagNumber = tagOf(registration.damId),
            sireAnimalId = registration.sireId,
            sireTagNumber = tagOf(registration.sireId),
            birthWeightKg = registration.birthWeightKg,
            calvingEase = registration.calvingEase,
            registrationDate = registration.registrationDate,
            gpsLat = animal.gpsLat,
            gpsLng = animal.gpsLng,
            deviceId = animal.deviceId,
            captureAt = animal.captureAt,
            photoPath = media.lastOrNull { it.animalId == animal.animalId && it.mediaType == "PHOTO" }?.filePath,
            recordGuid = registration.recordGuid,
            syncStatus = registration.syncStatus,
            syncedAt = registration.syncedAt
        )
    }

    override fun getRegistrationByTag(tagNumber: String): Flow<CalfRegistrationView?> =
        flowOf(registrations.mapNotNull { view(it) }.firstOrNull { it.tagNumber == tagNumber })

    override fun getAllRegistrationViews(): Flow<List<CalfRegistrationView>> =
        flowOf(registrations.mapNotNull { view(it) }.sortedByDescending { it.captureAt })

    override suspend fun getPendingRegistrationViews(): List<CalfRegistrationView> =
        registrations.filter { it.syncStatus != "SYNCED" }.mapNotNull { view(it) }

    override suspend fun markSynced(recordGuids: List<String>, syncedAt: Long) {
        registrations.replaceAll {
            if (it.recordGuid in recordGuids) it.copy(syncStatus = "SYNCED", syncedAt = syncedAt) else it
        }
    }

    override fun getOffspringByDam(damAnimalId: String): Flow<List<CalfRegistrationEntity>> =
        flowOf(registrations.filter { it.damId == damAnimalId })

    override fun getOffspringBySire(sireAnimalId: String): Flow<List<CalfRegistrationEntity>> =
        flowOf(registrations.filter { it.sireId == sireAnimalId })

    fun tagIdentifierCount(): Int = identifiers.count { it.identifierType == IdentifierTypes.TAG }
}
