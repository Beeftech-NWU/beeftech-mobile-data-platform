package com.beeftech.management

import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.database.entity.SyncPolicyState
import com.beeftech.database.entity.SyncSecurityEvent

/*
 * In-memory stand-in for the Room DAO, so the upload and unlock logic can be tested on the JVM.
 * It keeps the same rules as the real SQL (the user filter, oldest first, the flag only set once).
 * The real queries are covered by SyncSecurityUploadDaoTest on a device. The wipe queries are not
 * used here.
 */
class FakeSyncSecurityDao : SyncSecurityDao() {

    val events = mutableListOf<SyncSecurityEvent>()
    val states = mutableMapOf<String, SyncPolicyState>()

    override suspend fun insertEvent(event: SyncSecurityEvent): Long {
        events += event.copy(id = events.size + 1L)
        return events.size.toLong()
    }

    override suspend fun getAllEvents() = events.sortedByDescending { it.eventTime }

    override suspend fun getNotUploaded(userId: String, limit: Int) =
        events.filter { it.uploadedAt == null && (it.userId == userId || it.userId == null) }
            .sortedWith(compareBy({ it.eventTime }, { it.id }))
            .take(limit)

    override suspend fun markUploaded(ids: List<Long>, uploadedAt: Long): Int {
        var changed = 0
        events.replaceAll {
            if (it.id in ids && it.uploadedAt == null) {
                changed++
                it.copy(uploadedAt = uploadedAt)
            } else it
        }
        return changed
    }

    override suspend fun savePolicyState(state: SyncPolicyState) {
        states[state.userId] = state
    }

    override suspend fun getPolicyState(userId: String) = states[userId]

    override suspend fun isLocked(userId: String) = states[userId]?.locked == true

    override suspend fun unlockByAdministrator(userId: String): Int {
        val state = states[userId] ?: return 0
        states[userId] = state.copy(locked = false, lockedAt = null, lockReason = null)
        return 1
    }

    override suspend fun deleteUnsyncedMovement(recordGuid: String) = unused()
    override suspend fun deleteUnsyncedMortality(recordGuid: String) = unused()
    override suspend fun deleteUnsyncedCost(recordGuid: String) = unused()
    override suspend fun deleteQueuedPurchase(recordGuid: String) = unused()
    override suspend fun unsyncedTreatmentExists(recordGuid: String) = unused()
    override suspend fun deleteTreatmentDerivedCost(recordGuid: String) = unused()
    override suspend fun deleteUnsyncedTreatment(recordGuid: String) = unused()
    override suspend fun deleteUnsyncedFarmer(farmerId: String) = unused()
    override suspend fun findUnsyncedCalfAnimalId(recordGuid: String): String? = unused()
    override suspend fun deleteUnsyncedCalfRegistration(recordGuid: String) = unused()
    override suspend fun deleteUnsyncedCalfAnimal(animalId: String) = unused()
    override suspend fun deletePendingQueueItem(pendingId: Long) = unused()

    private fun unused(): Nothing = error("Not used by these tests")
}
