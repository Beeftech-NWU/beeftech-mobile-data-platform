package com.beeftech.database.repository

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.database.entity.SyncSecurityEvent

data class SyncPolicyEvaluation(
    val pendingCount: Int,
    val oldestPendingAgeDays: Long,
    val warningLevel: Int,
    val accountLocked: Boolean,
    val wipedOperationCount: Int = 0
)

class SyncPolicyEnforcer(
    private val pendingSyncDao: PendingSyncDao,
    private val syncSecurityDao: SyncSecurityDao
) {

    suspend fun evaluate(
        userId: String,
        now: Long = System.currentTimeMillis()
    ): SyncPolicyEvaluation {

        /*
         * Once locked, only the administrator recovery path may
         * unlock the account.
         */
        if (
            syncSecurityDao.isLocked(
                userId
            )
        ) {

            return SyncPolicyEvaluation(
                pendingCount =
                    pendingSyncDao.getPendingCountForUser(userId),

                oldestPendingAgeDays =
                    7,

                warningLevel =
                    4,

                accountLocked =
                    true
            )
        }

        val pending =
            pendingSyncDao.getAllForUser(userId)

        if (pending.isEmpty()) {

            return SyncPolicyEvaluation(
                pendingCount =
                    0,

                oldestPendingAgeDays =
                    0,

                warningLevel =
                    0,

                accountLocked =
                    false
            )
        }

        val oldest =
            pending.minOf {
                it.createdAt
            }

        val ageMillis =
            (
                now -
                        oldest
                )
                .coerceAtLeast(0L)

        val ageDays =
            ageMillis /
                    ONE_DAY_MS

        /*
         * Record each threshold only once.
         *
         * event_key is UNIQUE, so repeated hourly evaluation
         * cannot duplicate Day 2/4/6 audit entries.
         */
        if (ageDays >= 2) {
            recordWarning(
                userId =
                    userId,

                warningDay =
                    2,

                pendingCount =
                    pending.size,

                oldestPendingCreatedAt =
                    oldest,

                now =
                    now
            )
        }

        if (ageDays >= 4) {
            recordWarning(
                userId =
                    userId,

                warningDay =
                    4,

                pendingCount =
                    pending.size,

                oldestPendingCreatedAt =
                    oldest,

                now =
                    now
            )
        }

        if (ageDays >= 6) {
            recordWarning(
                userId =
                    userId,

                warningDay =
                    6,

                pendingCount =
                    pending.size,

                oldestPendingCreatedAt =
                    oldest,

                now =
                    now
            )
        }

        if (ageDays >= 7) {

            val wiped =
                syncSecurityDao.enforceDay7(
                    userId =
                        userId,

                    pendingRecords =
                        pending,

                    oldestPendingCreatedAt =
                        oldest,

                    now =
                        now
                )

            return SyncPolicyEvaluation(
                pendingCount =
                    0,

                oldestPendingAgeDays =
                    ageDays,

                warningLevel =
                    4,

                accountLocked =
                    true,

                wipedOperationCount =
                    wiped
            )
        }

        return SyncPolicyEvaluation(
            pendingCount =
                pending.size,

            oldestPendingAgeDays =
                ageDays,

            warningLevel =
                warningLevel(
                    ageDays
                ),

            accountLocked =
                false
        )
    }

    private suspend fun recordWarning(
        userId: String,
        warningDay: Int,
        pendingCount: Int,
        oldestPendingCreatedAt: Long,
        now: Long
    ) {

        syncSecurityDao.insertEvent(
            SyncSecurityEvent(
                eventKey =
                    "$userId:WARNING:$warningDay:$oldestPendingCreatedAt",

                userId =
                    userId,

                eventType =
                    SyncSecurityDao.EVENT_WARNING,

                eventTime =
                    now,

                warningDay =
                    warningDay,

                pendingCount =
                    pendingCount,

                oldestPendingCreatedAt =
                    oldestPendingCreatedAt,

                details =
                    "Unsynced local data reached the Day $warningDay warning threshold."
            )
        )
    }

    companion object {

        private const val ONE_DAY_MS =
            24L * 60L * 60L * 1000L

        internal fun warningLevel(
            ageDays: Long
        ): Int {

            return when {

                ageDays >= 7 ->
                    4

                ageDays >= 6 ->
                    3

                ageDays >= 4 ->
                    2

                ageDays >= 2 ->
                    1

                else ->
                    0
            }
        }
    }
}
