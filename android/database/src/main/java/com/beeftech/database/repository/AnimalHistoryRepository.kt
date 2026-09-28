package com.beeftech.database.repository

import com.beeftech.database.BeefTechDatabase
import com.beeftech.database.dao.AnimalGroupMembershipDao
import com.beeftech.database.dao.AnimalIdentifierDao
import com.beeftech.database.dao.AnimalWeightDao
import com.beeftech.database.entity.AnimalGroupMembershipEntity
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.AnimalWeightEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AnimalHistoryRepository(
    private val animalWeightDao: AnimalWeightDao,
    private val animalGroupMembershipDao: AnimalGroupMembershipDao,
    private val animalIdentifierDao: AnimalIdentifierDao,
    private val database: BeefTechDatabase
) {

    suspend fun recordWeighIn(
        animalId: String,
        weightKg: Double,
        bodyConditionScore: String? = null,
        notes: String? = null,
        capturedAt: Long = System.currentTimeMillis(),
        deviceId: String = "",
        gpsLat: Double = 0.0,
        gpsLng: Double = 0.0
    ): AnimalWeightEntity {
        val record = AnimalWeightEntity(
            weightId = UUID.randomUUID().toString(),
            animalId = animalId,
            weightKg = weightKg,
            weighDate = capturedAt,
            bodyConditionScore = bodyConditionScore,
            notes = notes,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            deviceId = deviceId,
            capturedAt = capturedAt
        )
        animalWeightDao.insertWeight(record)
        return record
    }

    fun getWeightHistory(animalId: String): Flow<List<AnimalWeightEntity>> {
        return animalWeightDao.getWeightHistoryForAnimal(animalId)
    }

    fun getLatestWeight(animalId: String): Flow<AnimalWeightEntity?> {
        return animalWeightDao.getLatestWeightForAnimal(animalId)
    }

    suspend fun changeGroup(
        animalId: String,
        groupId: String,
        at: Long = System.currentTimeMillis()
    ): AnimalGroupMembershipEntity {
        val newMembership = AnimalGroupMembershipEntity(
            membershipId = UUID.randomUUID().toString(),
            animalId = animalId,
            groupId = groupId,
            joinedAt = at,
            leftAt = null
        )
        database.runInTransaction {
            database.openHelper.writableDatabase.execSQL(
                "UPDATE animal_group_memberships SET left_at = ? WHERE animal_id = ? AND left_at IS NULL",
                arrayOf<Any>(at, animalId)
            )
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO animal_group_memberships (membership_id, animal_id, group_id, joined_at, left_at, record_guid) VALUES (?, ?, ?, ?, NULL, ?)",
                arrayOf<Any>(newMembership.membershipId, newMembership.animalId, newMembership.groupId, newMembership.joinedAt, newMembership.recordGuid)
            )
        }
        return newMembership
    }

    fun getGroupHistory(animalId: String): Flow<List<AnimalGroupMembershipEntity>> {
        return animalGroupMembershipDao.getGroupHistoryForAnimalFlow(animalId)
    }

    fun getActiveGroupMembership(animalId: String): Flow<AnimalGroupMembershipEntity?> {
        return animalGroupMembershipDao.getActiveGroupMembershipFlow(animalId)
    }

    suspend fun retagIdentifier(
        animalId: String,
        identifierType: String,
        newValue: String,
        at: Long = System.currentTimeMillis()
    ): AnimalIdentifierEntity {
        val newIdentifier = AnimalIdentifierEntity(
            identifierId = UUID.randomUUID().toString(),
            animalId = animalId,
            identifierType = identifierType,
            identifierValue = newValue,
            validFrom = at,
            validTo = null
        )
        database.runInTransaction {
            database.openHelper.writableDatabase.execSQL(
                "UPDATE animal_identifiers SET valid_to = ? WHERE animal_id = ? AND identifier_type = ? AND valid_to IS NULL",
                arrayOf<Any>(at, animalId, identifierType)
            )
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO animal_identifiers (identifier_id, animal_id, identifier_type, identifier_value, valid_from, valid_to, record_guid) VALUES (?, ?, ?, ?, ?, NULL, ?)",
                arrayOf<Any>(newIdentifier.identifierId, newIdentifier.animalId, newIdentifier.identifierType, newIdentifier.identifierValue, at, newIdentifier.recordGuid)
            )
        }
        return newIdentifier
    }

    fun getActiveIdentifiers(animalId: String): Flow<List<AnimalIdentifierEntity>> {
        return animalIdentifierDao.getActiveIdentifiersForAnimal(animalId)
    }

    fun getIdentifierHistory(animalId: String): Flow<List<AnimalIdentifierEntity>> {
        return animalIdentifierDao.getAllIdentifiersForAnimal(animalId)
    }
}
