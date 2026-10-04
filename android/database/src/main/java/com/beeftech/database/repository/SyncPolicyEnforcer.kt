package com.beeftech.database.repository

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.database.entity.SyncSecurityEvent
import kotlinx.coroutines.CancellationException

data class SyncPolicyEvaluation(
    val pendingCount: Int,
    val oldestPendingAgeDays: Long,
    val warningLevel: Int,
    val accountLocked: Boolean,
    val wipedOperationCount: Int = 0
)

class SyncPolicyEnforcer(
    private val pendingSyncDao: PendingSyncDao,
    private val syncSecurityDao: SyncSecurityDao,
    /*
     * Where the three warning days come from (the server can change them). It only ever affects
     * the warnings: the wipe at [SyncWarningPolicy.WIPE_DAY] never reads it.
     */
    private val policyProvider: suspend () -> SyncWarningPolicy = { SyncWarningPolicy.DEFAULT }
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
                    SyncWarningPolicy.WIPE_DAY.toLong(),

                warningLevel =
                    SyncWarningPolicy.WIPE_LEVEL,

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
         * The wipe is decided here, from a constant, before any policy is read: nothing the
         * server sends can bring it forward, push it back or stop it.
         */
        val wipeDue =
            ageDays >= SyncWarningPolicy.WIPE_DAY

        val policy =
            loadPolicy()

        /*
         * Record each threshold only once.
         *
         * event_key is UNIQUE, so repeated hourly evaluation
         * cannot duplicate warning audit entries.
         */
        policy.warningDays
            .filter { ageDays >= it }
            .forEach { warningDay ->
                recordWarning(
                    userId =
                        userId,

                    warningDay =
                        warningDay,

                    pendingCount =
                        pending.size,

                    oldestPendingCreatedAt =
                        oldest,

                    now =
                        now
                )
            }

        if (wipeDue) {

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
                    SyncWarningPolicy.WIPE_LEVEL,

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
                policy.levelFor(
                    ageDays
                ),

            accountLocked =
                false
        )
    }

    /* A policy that can't be read is the default; it never stops an evaluation. */
    private suspend fun loadPolicy(): SyncWarningPolicy =
        try {
            policyProvider()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncWarningPolicy.DEFAULT
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
    }
}
