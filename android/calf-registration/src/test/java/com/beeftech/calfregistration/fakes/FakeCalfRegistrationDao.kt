package com.beeftech.calfregistration.fakes

import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.CalfWithParents
import com.beeftech.database.entity.CalfRegistrationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Simple in-memory fake of [CalfRegistrationDao], keyed by `registeredAnimalId`,
 * used to unit test the calf-registration repository/viewmodel without Room/Robolectric.
 */
class FakeCalfRegistrationDao : CalfRegistrationDao {

    private val storageByAnimalId = mutableMapOf<String, CalfRegistrationEntity>()

    override suspend fun insertCalfRegistration(registration: CalfRegistrationEntity) {
        storageByAnimalId[registration.registeredAnimalId] = registration
    }

    override fun getCalfRegistrationDetails(animalId: String): Flow<CalfWithParents?> {
        val entity = storageByAnimalId[animalId] ?: return flowOf(null)
        return flowOf(
            CalfWithParents(
                registrationId = entity.registrationId,
                registeredAnimalId = entity.registeredAnimalId,
                damId = entity.damId,
                sireId = entity.sireId,
                birthWeightKg = entity.birthWeightKg,
                calvingEase = entity.calvingEase,
                registrationDate = entity.registrationDate
            )
        )
    }

    override fun getOffspringByDam(damId: String): Flow<List<CalfRegistrationEntity>> {
        val list = storageByAnimalId.values.filter { it.damId == damId }
        return flowOf(list)
    }

    override fun getOffspringBySire(sireId: String): Flow<List<CalfRegistrationEntity>> {
        val list = storageByAnimalId.values.filter { it.sireId == sireId }
        return flowOf(list)
    }

    override fun getAllCalfRegistrations(): Flow<List<CalfRegistrationEntity>> {
        return flowOf(storageByAnimalId.values.toList())
    }

    fun existsByAnimalId(animalId: String): Boolean = storageByAnimalId.containsKey(animalId)
    fun findByAnimalId(animalId: String): CalfRegistrationEntity? = storageByAnimalId[animalId]
}
