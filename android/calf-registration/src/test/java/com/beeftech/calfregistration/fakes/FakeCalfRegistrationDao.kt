package com.beeftech.calfregistration.fakes

import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.entity.CalfRegistration

/**
 * Simple in-memory fake of [CalfRegistrationDao], keyed by `animalId`
 * (mirroring the real table's unique index), used to unit test the
 * calf-registration repository/viewmodel without pulling in Room/Robolectric.
 */
class FakeCalfRegistrationDao : CalfRegistrationDao {

    private val storageByAnimalId = mutableMapOf<String, CalfRegistration>()
    private var nextId = 1L

    override suspend fun insert(calf: CalfRegistration) {
        require(!storageByAnimalId.containsKey(calf.animalId)) {
            "UNIQUE constraint failed: animalId already exists"
        }
        storageByAnimalId[calf.animalId] = calf.copy(id = nextId++)
    }

    override suspend fun upsert(calf: CalfRegistration): Long {
        val id = if (calf.id != 0L) calf.id else nextId++
        val withId = calf.copy(id = id)
        storageByAnimalId[calf.animalId] = withId
        return id
    }

    override suspend fun getAll(): List<CalfRegistration> {
        return storageByAnimalId.values.toList()
    }

    override suspend fun existsByAnimalId(animalId: String): Boolean {
        return storageByAnimalId.containsKey(animalId)
    }

    override suspend fun findByAnimalId(animalId: String): CalfRegistration? {
        return storageByAnimalId[animalId]
    }

    override suspend fun updateSyncStatus(
        animalId: String,
        syncStatus: String,
        syncedAt: Long?
    ): Int {
        val existing = storageByAnimalId[animalId] ?: return 0
        storageByAnimalId[animalId] = existing.copy(syncStatus = syncStatus, syncedat = syncedAt)
        return 1
    }
}
