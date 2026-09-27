package com.beeftech.farmtraceability.repository

import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.database.util.TagNamingUtils
import kotlinx.coroutines.flow.firstOrNull

class FindAnimalRepository(
    private val calfRegistrationDao: CalfRegistrationDao
) {

    /** Accepts a full tag or shorthand ("B1234567", "blu1234567"). */
    suspend fun findAnimal(
        animalReference: String
    ): CalfRegistrationView? {
        return calfRegistrationDao.getRegistrationByTag(
            TagNamingUtils.parseAndExpand(animalReference)
        ).firstOrNull()
    }
}
