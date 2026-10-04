package com.beeftech.management.data

import com.beeftech.database.dao.SyncSecurityDao

/**
 * Lifts this phone's Day-7 lock once an admin has cleared it on the server.
 *
 * The server only reports WHEN the admin cleared the lock. It is lifted only if that is later than
 * when this phone locked, so an old clearance can never undo a newer lock. Lifting it never brings
 * back wiped data; it only lets the person sign in and capture again.
 */
class SyncLockRelease(
    private val dao: SyncSecurityDao,
    private val userId: String
) {

    /* True if the lock was lifted. */
    suspend fun apply(clearedAt: Long?): Boolean {
        if (clearedAt == null) return false

        val state = dao.getPolicyState(userId) ?: return false
        val lockedAt = state.lockedAt
        if (!state.locked || lockedAt == null || clearedAt <= lockedAt) return false

        return dao.unlockByAdministrator(userId) > 0
    }
}
