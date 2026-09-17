package com.beeftech.database.repository

import com.beeftech.database.dao.AnimalDao
import com.beeftech.database.dao.AnimalGroupDao
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalGroup

class AnimalManagementRepository(
    private val animalDao: AnimalDao,
    private val animalGroupDao: AnimalGroupDao
) {

    suspend fun addAnimal(animal: Animal): Long {
        return animalDao.insert(animal)
    }

    suspend fun updateAnimal(animal: Animal): Int {
        return animalDao.update(animal)
    }

    suspend fun deleteAnimal(animal: Animal): Int {
        return animalDao.delete(animal)
    }

    suspend fun getAllAnimals(): List<Animal> {
        return animalDao.getAll()
    }

    suspend fun getAnimalById(animalId: String): Animal? {
        return animalDao.getById(animalId)
    }

    suspend fun getAnimalByTemperatureNumber(
        temperatureNumber: String
    ): Animal? {
        return animalDao.getByTemperatureNumber(temperatureNumber)
    }

    suspend fun getAnimalByTagNumber(tagNumber: String): Animal? {
        return animalDao.getByTagNumber(tagNumber)
    }

    suspend fun getAnimalByReferenceNumber(
        referenceNumber: String
    ): Animal? {
        return animalDao.getByReferenceNumber(referenceNumber)
    }

    suspend fun getOffspring(parentId: String): List<Animal> {
        return animalDao.getOffspring(parentId)
    }

    suspend fun addAnimalGroup(group: AnimalGroup): Long {
        return animalGroupDao.insert(group)
    }

    suspend fun updateAnimalGroup(group: AnimalGroup) {
        animalGroupDao.update(group)
    }

    suspend fun deleteAnimalGroup(group: AnimalGroup): Int {
        return animalGroupDao.delete(group)
    }

    suspend fun getAllAnimalGroups(): List<AnimalGroup> {
        return animalGroupDao.getAll()
    }

    suspend fun getAnimalGroupById(groupId: String): AnimalGroup? {
        return animalGroupDao.getById(groupId)
    }

    suspend fun getAnimalGroupByName(groupName: String): AnimalGroup? {
        return animalGroupDao.getByName(groupName)
    }
}
