package com.beeftech.database.repository

import androidx.lifecycle.LiveData
import com.beeftech.database.dao.AnimalMortalityDao
import com.beeftech.database.dao.Animal_MovementDao
import com.beeftech.database.dao.AnimalTreatmentDao
import com.beeftech.database.entity.AnimalMortalityEntity
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.entity.AnimalTreatmentEntity

class AnimalTraceabilityRepository(
    private val movementDao: Animal_MovementDao,
    private val treatmentDao: AnimalTreatmentDao,
    private val mortalityDao: AnimalMortalityDao
) {

    // ------------------ Movements ------------------
    suspend fun upsertMovement(movement: AnimalMovementEntity) {
        movementDao.upsertMovement(movement)
    }

    suspend fun getMovementsForAnimal(animalGuid: String): LiveData<List<AnimalMovementEntity>> {
        return movementDao.getMovementsForAnimal(animalGuid) as LiveData<List<AnimalMovementEntity>>
    }

    // ------------------ Treatments ------------------
    suspend fun upsertTreatment(treatment: AnimalTreatmentEntity) {
        treatmentDao.upsertTreatment(treatment)
    }

    suspend fun getTreatmentsForAnimal(animalGuid: String): LiveData<List<AnimalTreatmentEntity>> {
        return treatmentDao.getTreatmentsForAnimal(animalGuid) as LiveData<List<AnimalTreatmentEntity>>
    }

    // ------------------ Mortalities ------------------
    suspend fun upsertMortality(mortality: AnimalMortalityEntity) {
        mortalityDao.upsertMortality(mortality)
    }

    suspend fun getMortalityForAnimal(animalGuid: String): LiveData<AnimalMortalityEntity?> {
        return mortalityDao.getMortalityForAnimal(animalGuid)
    }
}
