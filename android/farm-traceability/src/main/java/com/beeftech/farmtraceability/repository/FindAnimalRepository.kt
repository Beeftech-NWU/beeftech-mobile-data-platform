package com.beeftech.farmtraceability.repository

import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.CalfWithParents
import kotlinx.coroutines.flow.firstOrNull

class FindAnimalRepository(
    private val calfRegistrationDao: CalfRegistrationDao
) {

    suspend fun findAnimal(
        animalReference: String
    ): CalfWithParents? {
        return calfRegistrationDao.getCalfRegistrationDetails(
            animalReference.trim().uppercase()
        ).firstOrNull()
    }
}
