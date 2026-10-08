package com.beeftech.database.repository

import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.SyncRunDao
import com.beeftech.database.entity.SyncRunEntity
import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunResult
import com.beeftech.database.security.CurrentUserIdRegistry
import com.beeftech.database.util.BatchNaming
import com.beeftech.database.util.ProjectCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/** Keeps and reads the sync history ("what synced, when") for the signed-in user. */
class SyncRunRepository(
    private val syncRunDao: SyncRunDao,
    private val pendingSyncDao: PendingSyncDao,
    private val userIdProvider: () -> String? = { CurrentUserIdRegistry.currentUserId() },
    private val userIdFlow: Flow<String?> = CurrentUserIdRegistry.currentUserIdFlow,
    private val now: () -> Long = System::currentTimeMillis
) {

    /** Saves one run and drops runs older than 30 days. Does nothing without a signed-in user. */
    suspend fun recordRun(
        module: String,
        trigger: String,
        startedAt: Long,
        syncedCount: Int,
        failedCount: Int,
        result: String,
        message: String? = null,
        batchName: String? = null
    ) {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() } ?: return

        /* The upload this run sent, if it sent one: the newest name its module's client issued since it started. */
        val batch = batchName ?: projectOf(module)?.let { BatchNaming.lastNameSince(it, startedAt) }

        syncRunDao.insert(
            SyncRunEntity(
                userId = userId,
                module = module,
                trigger = trigger,
                startedAt = startedAt,
                finishedAt = now(),
                syncedCount = syncedCount,
                failedCount = failedCount,
                result = result,
                message = message?.take(MAX_MESSAGE_LENGTH),
                batchName = batch
            )
        )
        syncRunDao.pruneOlderThan(now() - RETENTION_MILLIS)
    }

    /** Newest first; switches with the signed-in user. */
    fun observeRecent(limit: Int = 50): Flow<List<SyncRunEntity>> =
        userIdFlow.flatMapLatest { raw ->
            val userId = raw?.trim()?.takeIf { it.isNotEmpty() }
            if (userId == null) flowOf(emptyList()) else syncRunDao.observeRecent(userId, limit)
        }

    /** Waiting records per entity type for the signed-in user, e.g. ANIMAL_COST to 3. */
    fun observePendingByType(): Flow<Map<String, Int>> =
        userIdFlow.flatMapLatest { raw ->
            val userId = raw?.trim()?.takeIf { it.isNotEmpty() }
            if (userId == null) {
                flowOf(emptyMap())
            } else {
                pendingSyncDao.observePendingCountsByType(userId).map { rows ->
                    rows.associate { it.entityType to it.count }
                }
            }
        }

    /**
     * Runs [block] (a module's sync) and records how many of [entityTypes] it cleared from the
     * queue. An exception is recorded as a failed run and then rethrown, so the worker still
     * decides whether to retry.
     */
    suspend fun <R> trackRun(
        module: String,
        entityTypes: List<String>,
        trigger: String,
        block: suspend () -> R
    ): R {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() }
        val startedAt = now()
        val before = userId?.let { pendingSyncDao.countForUserAndTypes(it, entityTypes) } ?: 0

        var error: String? = null
        try {
            return block()
        } catch (e: Exception) {
            error = e.message ?: e::class.java.simpleName
            throw e
        } finally {
            if (userId != null) {
                val after = pendingSyncDao.countForUserAndTypes(userId, entityTypes)
                if (SyncRunSummary.worthRecording(trigger, before, after, error)) {
                    val summary = SyncRunSummary.of(before, after, error)
                    recordRun(module, trigger, startedAt, summary.syncedCount, summary.failedCount, summary.result, error)
                }
            }
        }
    }

    /** Records that a sync could not start because there was no server session. */
    suspend fun recordOffline(module: String, trigger: String, firstAttempt: Boolean) {
        if (!SyncRunSummary.worthRecordingOffline(trigger, firstAttempt)) return
        recordRun(module, trigger, now(), 0, 0, SyncRunResult.OFFLINE, "No server session. Records stay queued.")
    }

    companion object {
        /** The project code each module's uploads are named with. */
        fun projectOf(module: String): ProjectCode? = when (module) {
            SyncRunModule.CALF -> ProjectCode.CALF_REG
            SyncRunModule.FARMER -> ProjectCode.FARMER_REG
            SyncRunModule.TREATMENT -> ProjectCode.TREATMENT
            SyncRunModule.MOVEMENT -> ProjectCode.MOVEMENT
            SyncRunModule.MORTALITY -> ProjectCode.MORTALITY
            SyncRunModule.COST -> ProjectCode.COST
            SyncRunModule.TRACEABILITY -> ProjectCode.TRACE_EVENT
            else -> null
        }

        const val MAX_MESSAGE_LENGTH = 300
        val RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
