package com.beeftech.farmtraceability.repository

import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.entity.CalfRegistration

class FindAnimalRepository(
    private val calfRegistrationDao: CalfRegistrationDao
) {

    suspend fun findAnimal(
        animalReference: String
    ): CalfRegistration? {
        return calfRegistrationDao.findByAnimalId(
            animalReference.trim().uppercase()
        )
    }
}