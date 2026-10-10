package com.beeftech.feedcrib.data

import android.util.Log
import com.beeftech.database.dao.FeedCribDao
import com.beeftech.database.dao.FeedCribSessionRow
import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.CurrentUserIdRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.util.TimeZone

/**
 * Cribs, codes and readings for the Feed tab: downloads the site's cribs, saves a reading
 * locally first, tries to send it straight away, and keeps whatever did not go through queued
 * for the background worker.
 *
 * The entries table is the source of truth for what still has to be sent. The pending_sync
 * queue is bookkeeping (it feeds the Day-7 policy and the "waiting" counts) and is repaired
 * here whenever it disagrees.
 */
class FeedCribRepository(
    private val feedCribDao: FeedCribDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: FeedCribApiClient,
    private val captureContextProvider: () -> FeedCribCaptureContext,
    private val userIdProvider: () -> String? = { CurrentUserIdRegistry.currentUserId() },
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val now: () -> Long = System::currentTimeMillis
) {

    // --- reads for the screens ---

    fun observeCribs(): Flow<List<FeedCribEntity>> = feedCribDao.observeCribs()

    fun observeCodes(): Flow<List<CribReadingCodeEntity>> = feedCribDao.observeCodes()

    fun observeLastDownloadedAt(): Flow<Long?> = feedCribDao.observeLastDownloadedAt()

    suspend fun findCrib(cribNumber: String): FeedCribEntity? =
        feedCribDao.getCrib(cribNumber.trim().uppercase())

    /** The codes shown in a crib's 3x3 grid: for each of the last 3 days and each block, the newest. */
    fun observeLastNineSlots(cribNumber: String): Flow<List<FeedCribEntryEntity>> =
        feedCribDao.observeLastSlots(cribNumber, SlotAllocator.dateDaysBefore(now(), GRID_DAYS - 1, timeZone))

    /** One line per crib the signed-in user took a reading on today. */
    fun observeTodaySessions(): Flow<List<FeedCribSessionRow>> {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        return feedCribDao.observeSessions(SlotAllocator.dateFor(now(), timeZone), userId)
    }

    /** Every entry the signed-in user took on a crib today, for the read-only session detail. */
    fun observeTodayEntries(cribNumber: String): Flow<List<FeedCribEntryEntity>> {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        return feedCribDao.observeEntriesForCrib(cribNumber, SlotAllocator.dateFor(now(), timeZone), userId)
    }

    // --- download ---

    /**
     * Downloads the site's cribs, the code table and the recent entries from every phone, and
     * replaces what is stored. A reading waiting to be sent is never touched. Returns the number
     * of cribs, or why it failed (offline, signed out); the old data stays in that case.
     */
    suspend fun refreshCribs(): Result<Int> {
        val download = apiClient.fetchCribs(GRID_DAYS)
        val response = download.getOrElse { return Result.failure(it) }

        return try {
            val downloadedAt = now()

            feedCribDao.applyDownload(
                cribs = response.cribs.map { FeedCribMappers.toEntity(it, downloadedAt) },
                codes = response.codes.map { FeedCribMappers.toEntity(it) },
                serverEntries = response.entries.map { FeedCribMappers.toEntity(it, response.siteId) },
                pruneBeforeDate = SlotAllocator.dateDaysBefore(downloadedAt, KEEP_SYNCED_DAYS, timeZone)
            )

            Result.success(response.cribs.size)
        } catch (exception: Exception) {
            Log.w(TAG, "Could not store the feed crib download.", exception)
            Result.failure(exception)
        }
    }

    // --- save ---

    /**
     * Files a reading under the right block by the clock, stores it, and tries to send it. The
     * local save is never lost to a sync problem. [code] is null for an ADI-only change.
     */
    suspend fun saveEntry(cribNumber: String, code: Int?, adi: Double): SaveEntryOutcome {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() }
            ?: return SaveEntryOutcome(validationError = "Sign in to save readings.")

        val crib = findCrib(cribNumber)
            ?: return SaveEntryOutcome(validationError = "Crib $cribNumber is not on this phone. Refresh the crib list.")

        if (!crib.active) {
            return SaveEntryOutcome(validationError = "Crib ${crib.cribNumber} is not active.")
        }

        if (!adi.isFinite() || adi < 0) {
            return SaveEntryOutcome(validationError = "ADI must be zero or more.")
        }

        if (code != null) {
            val known = feedCribDao.observeCodes().first()
            if (known.none { it.code == code && it.active }) {
                return SaveEntryOutcome(validationError = "Reading code $code is not valid.")
            }
        }

        val context = captureContextProvider()

        val entry = FeedCribEntryEntity(
            cribNumber = crib.cribNumber,
            siteId = crib.siteId,
            readingDate = SlotAllocator.dateFor(context.captureAt, timeZone),
            slot = SlotAllocator.slotFor(context.captureAt, timeZone),
            code = code,
            adi = adi,
            capturedAt = context.captureAt,
            deviceId = context.deviceId,
            gpsLat = context.gpsLat,
            gpsLng = context.gpsLng,
            userId = userId
        )

        try {
            feedCribDao.insertEntry(entry)
        } catch (exception: Exception) {
            return SaveEntryOutcome(saveError = exception.message ?: "Unable to save the reading.")
        }

        /* From here the reading is stored; nothing below may make the save look failed. */
        val syncError = try {
            pendingSyncRepository.queueOperation(
                entityType = ENTITY_TYPE,
                entityId = entry.recordGuid,
                operation = "UPSERT",
                payload = ""
            )

            val outcome = syncPending()
            outcome.errorMessagesByRecordGuid[entry.recordGuid]
                ?: outcome.rejectedByRecordGuid[entry.recordGuid]
        } catch (exception: Exception) {
            exception.message ?: "Sync failed"
        }

        val stored = feedCribDao.findEntry(entry.recordGuid) ?: entry

        return SaveEntryOutcome(
            entry = stored,
            syncErrorMessage = syncError,
            needsAttention = stored.syncStatus == STATUS_REJECTED
        )
    }

    // --- upload ---

    /**
     * Each request is labelled with the device that captured its records, so a batch holding
     * records from more than one device is sent as one request per device. A device whose
     * request fails simply returns no results, which leaves its records pending; the whole
     * call fails only when every request failed.
     */
    private suspend fun syncByDevice(entries: List<FeedCribEntryEntity>): Result<FeedCribEntrySyncResponse> {
        val byDevice = entries.groupBy { it.deviceId }

        if (byDevice.size == 1) {
            return apiClient.syncEntries(entries, deviceId = byDevice.keys.first())
        }

        val results = mutableListOf<FeedCribEntrySyncResult>()
        var firstFailure: Throwable? = null

        for ((deviceId, records) in byDevice) {
            apiClient.syncEntries(records, deviceId).fold(
                onSuccess = { results += it.results },
                onFailure = { firstFailure = firstFailure ?: it }
            )
        }

        val failure = firstFailure
        return if (results.isEmpty() && failure != null) {
            Result.failure(failure)
        } else {
            Result.success(FeedCribEntrySyncResponse(results))
        }
    }

    /**
     * Sends every waiting reading of the signed-in user. [retryRejected] is the user's manual
     * retry: readings the server rejected too many times get a fresh set of attempts first. The
     * background worker never sets it.
     */
    suspend fun syncPending(retryRejected: Boolean = false): FeedCribSyncOutcome {

        val userId = userIdProvider()?.takeIf { it.isNotBlank() }
            ?: return FeedCribSyncOutcome(syncedCount = 0)

        return try {

            if (retryRejected) {
                feedCribDao.requeueRejected(userId)
            }

            val queued = pendingSyncRepository.getAllPendingOperations().filter { it.entityType == ENTITY_TYPE }

            val pending = feedCribDao.getPending(userId)

            if (pending.isEmpty()) {
                /* Nothing genuinely waiting: clear stale queue rows and finish. */
                queued.forEach { pendingSyncRepository.markSyncSuccessful(it.id) }
                return FeedCribSyncOutcome(syncedCount = 0)
            }

            val pendingGuids = pending.map { it.recordGuid }.toSet()

            /* Queue rows whose reading is already synced or gone. */
            queued.filter { it.entityId !in pendingGuids }
                .forEach { pendingSyncRepository.markSyncSuccessful(it.id) }

            /* Repair missing queue rows. */
            val queuedIds = queued.map { it.entityId }.toMutableSet()
            pending.forEach { entry ->
                if (queuedIds.add(entry.recordGuid)) {
                    pendingSyncRepository.queueOperation(ENTITY_TYPE, entry.recordGuid, "UPSERT", "")
                }
            }

            val syncResult = syncByDevice(pending)

            if (syncResult.isFailure) {
                val message = syncResult.exceptionOrNull()?.message ?: "Unable to reach the server"
                Log.w(TAG, "Feed crib synchronization request failed: $message")
                return FeedCribSyncOutcome(
                    syncedCount = 0,
                    errorMessagesByRecordGuid = pending.associate { it.recordGuid to message }
                )
            }

            val results = syncResult.getOrThrow().results
            val resultByGuid = results.associateBy { it.recordguid }

            val synced = results.filter { it.status == STATUS_SYNCED }
            val syncedGuids = synced.map { it.recordguid }.toSet()

            if (syncedGuids.isNotEmpty()) {
                /* The server's own time when it gave one, so the phone and the server agree on when it arrived. */
                synced.forEach { feedCribDao.markSynced(listOf(it.recordguid), it.serverSyncedAt ?: now()) }
                clearQueue(syncedGuids)
            }

            /*
             * Only an explicit server rejection counts toward the cap. A missing result or a
             * transport failure is treated as temporary.
             */
            val unsynced = pending.filter { it.recordGuid !in syncedGuids }
            unsynced.forEach { entry ->
                resultByGuid[entry.recordGuid]?.let { result ->
                    feedCribDao.recordRejection(
                        entry.recordGuid,
                        result.message ?: "Rejected by the server.",
                        MAX_SERVER_REJECTIONS
                    )
                }
            }

            val stillPending = feedCribDao.getPending(userId).map { it.recordGuid }.toSet()
            val rejectedGuids = unsynced.map { it.recordGuid }.filter { it !in stillPending }.toSet()

            /* A rejected reading leaves the queue; the entries table keeps the truth. */
            clearQueue(rejectedGuids)

            FeedCribSyncOutcome(
                syncedCount = syncedGuids.size,
                errorMessagesByRecordGuid = unsynced
                    .filter { it.recordGuid in stillPending }
                    .associate {
                        it.recordGuid to (resultByGuid[it.recordGuid]?.message ?: "Server did not confirm the reading.")
                    },
                rejectedByRecordGuid = unsynced
                    .filter { it.recordGuid in rejectedGuids }
                    .associate {
                        it.recordGuid to (resultByGuid[it.recordGuid]?.message ?: "Rejected by the server.")
                    }
            )

        } catch (exception: Exception) {

            /* Never turn a real failure into a successful empty result. */
            Log.e(TAG, "Pending feed crib synchronization failed unexpectedly.", exception)

            val remaining = runCatching { feedCribDao.getPending(userId) }.getOrDefault(emptyList())
            val message = exception.message ?: "Unexpected feed crib synchronization error."

            FeedCribSyncOutcome(
                syncedCount = 0,
                errorMessagesByRecordGuid =
                    if (remaining.isNotEmpty()) remaining.associate { it.recordGuid to message }
                    else mapOf("_sync" to message)
            )
        }
    }

    private suspend fun clearQueue(recordGuids: Set<String>) {
        if (recordGuids.isEmpty()) return

        pendingSyncRepository.getAllPendingOperations()
            .filter { it.entityType == ENTITY_TYPE && it.entityId in recordGuids }
            .forEach { pendingSyncRepository.markSyncSuccessful(it.id) }
    }

    companion object {
        const val ENTITY_TYPE = SyncSecurityDao.ENTITY_FEED_CRIB_ENTRY

        /** Explicit server rejections after which a reading stops retrying automatically. */
        const val MAX_SERVER_REJECTIONS = 3

        /** The grid shows the last 3 days, today included. */
        const val GRID_DAYS = 3

        /** Synced readings older than this are dropped from the phone; the server keeps them. */
        const val KEEP_SYNCED_DAYS = 14

        private const val STATUS_SYNCED = "SYNCED"
        private const val STATUS_REJECTED = "REJECTED"
        private const val TAG = "FeedCribRepository"
    }
}

data class SaveEntryOutcome(
    /** The stored reading, as it stands after the immediate sync attempt. Null when nothing was saved. */
    val entry: FeedCribEntryEntity? = null,
    /** The input was not acceptable; nothing was saved. */
    val validationError: String? = null,
    /** The local save itself failed; nothing was saved. */
    val saveError: String? = null,
    /** Saved, but the server did not take it yet. The reading stays queued (or rejected). */
    val syncErrorMessage: String? = null,
    /** The server turned the reading down for good; it will not retry on its own. */
    val needsAttention: Boolean = false
) {
    val saved: Boolean get() = entry != null
}

data class FeedCribSyncOutcome(
    val syncedCount: Int,
    /** Readings still waiting to be sent, with why. */
    val errorMessagesByRecordGuid: Map<String, String?> = emptyMap(),
    /** Readings the server refused often enough that they stopped retrying. */
    val rejectedByRecordGuid: Map<String, String> = emptyMap()
)
