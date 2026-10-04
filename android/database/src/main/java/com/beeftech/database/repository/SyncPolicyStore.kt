package com.beeftech.database.repository

import com.beeftech.database.dao.ReferenceDataDao
import com.beeftech.database.entity.DeviceConfigEntry
import kotlinx.coroutines.CancellationException

/**
 * The sync-warning policy the server last sent, kept in `device_config`. Reading it never throws
 * and always gives a valid policy, so the enforcer can't be stopped (or fooled) by it.
 */
class SyncPolicyStore(
    private val dao: ReferenceDataDao
) {

    /* The stored policy, or the default if none was ever pulled, it is unreadable, or the database fails. */
    suspend fun current(): SyncWarningPolicy =
        try {
            SyncWarningPolicy.parse(dao.getConfig(DeviceConfigEntry.SYNC_WARNING_DAYS))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncWarningPolicy.DEFAULT
        }

    suspend fun version(): Long? =
        dao.getConfig(DeviceConfigEntry.SYNC_POLICY_VERSION)?.toLongOrNull()

    /*
     * Stores what the server sent. The days are sanitized first, so an invalid set is stored as
     * the default; the version is stored either way, so the same bad answer isn't pulled again.
     * One statement, so the days and the version are never saved apart.
     */
    suspend fun save(warningDays: List<Int>?, version: Long, now: Long): SyncWarningPolicy {
        val policy = SyncWarningPolicy.sanitize(warningDays)
        dao.putConfigs(
            listOf(
                DeviceConfigEntry(DeviceConfigEntry.SYNC_WARNING_DAYS, policy.serialize(), now),
                DeviceConfigEntry(DeviceConfigEntry.SYNC_POLICY_VERSION, version.toString(), now)
            )
        )
        return policy
    }
}
