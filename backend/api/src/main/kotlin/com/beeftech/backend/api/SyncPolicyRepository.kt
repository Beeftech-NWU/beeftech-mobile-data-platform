package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

sealed interface SyncPolicyUpdateOutcome {
    data class Changed(val policy: SyncPolicyDto) : SyncPolicyUpdateOutcome
    data class Unchanged(val policy: SyncPolicyDto) : SyncPolicyUpdateOutcome
}

class SyncPolicyRepository {

    suspend fun get(): SyncPolicyDto =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) { read() }

    /*
     * Saves the policy. Saving what is already stored changes nothing (no version bump, no audit
     * row), so repeating a request is harmless. The audit row is built from the values before
     * and after, and written in the same transaction as the change.
     */
    suspend fun update(
        warningDays: List<Int>,
        staleSyncAlertHours: Int,
        updatedByUserId: String,
        audit: (before: SyncPolicyDto, after: SyncPolicyDto) -> AuditEntry,
        now: Long
    ): SyncPolicyUpdateOutcome =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val before = read()
            if (before.warningDays == warningDays && before.staleSyncAlertHours == staleSyncAlertHours) {
                return@newSuspendedTransaction SyncPolicyUpdateOutcome.Unchanged(before)
            }

            val after = before.copy(
                version = before.version + 1,
                warningDays = warningDays,
                staleSyncAlertHours = staleSyncAlertHours
            )

            write(AppSettingKeys.SYNC_WARNING_DAYS, warningDays.joinToString(","), now, updatedByUserId)
            write(AppSettingKeys.SYNC_STALE_ALERT_HOURS, staleSyncAlertHours.toString(), now, updatedByUserId)
            write(AppSettingKeys.SYNC_POLICY_VERSION, after.version.toString(), now, updatedByUserId)
            insertAuditRow(audit(before, after), now)

            SyncPolicyUpdateOutcome.Changed(after)
        }

    /* Missing or unreadable values fall back to the defaults, so a fresh database needs no seeding. */
    private fun read(): SyncPolicyDto {
        val settings = AppSettingsTable.selectAll()
            .where {
                (AppSettingsTable.key eq AppSettingKeys.SYNC_WARNING_DAYS) or
                    (AppSettingsTable.key eq AppSettingKeys.SYNC_STALE_ALERT_HOURS) or
                    (AppSettingsTable.key eq AppSettingKeys.SYNC_POLICY_VERSION)
            }
            .associate { it[AppSettingsTable.key] to it[AppSettingsTable.value] }

        val days = settings[AppSettingKeys.SYNC_WARNING_DAYS]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { SyncPolicyValidation.warningDaysError(it) == null }
            ?: SyncPolicyLimits.DEFAULT_WARNING_DAYS

        val hours = settings[AppSettingKeys.SYNC_STALE_ALERT_HOURS]
            ?.toIntOrNull()
            ?.takeIf { SyncPolicyValidation.staleHoursError(it) == null }
            ?: SyncPolicyLimits.DEFAULT_STALE_HOURS

        return SyncPolicyDto(
            version = settings[AppSettingKeys.SYNC_POLICY_VERSION]?.toLongOrNull() ?: 1L,
            warningDays = days,
            wipeDay = SyncPolicyLimits.WIPE_DAY,
            staleSyncAlertHours = hours
        )
    }

    private fun write(key: String, value: String, now: Long, updatedByUserId: String) {
        val updated = AppSettingsTable.update({ AppSettingsTable.key eq key }) {
            it[AppSettingsTable.value] = value
            it[updatedAt] = now
            it[AppSettingsTable.updatedByUserId] = updatedByUserId
        }
        if (updated == 0) {
            AppSettingsTable.insert {
                it[AppSettingsTable.key] = key
                it[AppSettingsTable.value] = value
                it[updatedAt] = now
                it[AppSettingsTable.updatedByUserId] = updatedByUserId
            }
        }
    }
}

object SyncPolicyValidation {

    /* Exactly three days, strictly increasing, each from 1 to one before the wipe day. */
    fun warningDaysError(days: List<Int>): String? =
        when {
            days.size != SyncPolicyLimits.WARNING_COUNT ->
                "Give exactly ${SyncPolicyLimits.WARNING_COUNT} warning days"
            days.any { it !in SyncPolicyLimits.MIN_WARNING_DAY..SyncPolicyLimits.MAX_WARNING_DAY } ->
                "Warning days must be from ${SyncPolicyLimits.MIN_WARNING_DAY} to ${SyncPolicyLimits.MAX_WARNING_DAY}: the data is wiped on day ${SyncPolicyLimits.WIPE_DAY}"
            days.zipWithNext().any { (a, b) -> a >= b } ->
                "Warning days must increase, for example 2, 4, 6"
            else -> null
        }

    fun staleHoursError(hours: Int): String? =
        if (hours in SyncPolicyLimits.MIN_STALE_HOURS..SyncPolicyLimits.MAX_STALE_HOURS) null
        else "Stale-sync hours must be from ${SyncPolicyLimits.MIN_STALE_HOURS} to ${SyncPolicyLimits.MAX_STALE_HOURS}"
}
