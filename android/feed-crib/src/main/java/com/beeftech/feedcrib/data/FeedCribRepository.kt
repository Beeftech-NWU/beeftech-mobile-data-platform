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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.util.TimeZone
import java.util.UUID
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.beeftech.database.entity.FeedCribSetupEntity

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
    private val now: () -> Long = System::currentTimeMillis,
    private val canManageCribs: () -> Boolean = { false },
    private val userSiteId: () -> String? = { null }
) {

    // --- reads for the screens ---

    fun observeCribs(): Flow<List<FeedCribEntity>> = combine(feedCribDao.observeCribs(), CurrentUserIdRegistry.currentUserIdFlow) { list, _ ->
        val site = userSiteId()
        val setup = cachedSetup()
        list.filter { site == null || it.siteId == site }.map { crib ->
            val pen = setup.pens.firstOrNull { it.id == crib.penId }
            val ration = setup.rations.firstOrNull { it.id == crib.rationId }
            crib.copy(penDescription = pen?.description ?: crib.penDescription,
                ration = ration?.name ?: crib.ration, requiredKg = if (pen != null) pen.requiredKg else crib.requiredKg,
                animalsBegin = if (pen != null) pen.animalsBegin else crib.animalsBegin,
                animalsIn = if (pen != null) pen.animalsIn else crib.animalsIn,
                animalsOut = if (pen != null) pen.animalsOut else crib.animalsOut,
                animalsClose = if (pen != null) pen.animalsClose else crib.animalsClose)
        }
    }

    fun observeCodes(): Flow<List<CribReadingCodeEntity>> = feedCribDao.observeCodes()

    fun observeLastDownloadedAt(): Flow<Long?> = feedCribDao.observeLastDownloadedAt()

    suspend fun findCrib(cribNumber: String): FeedCribEntity? {
        val list = observeCribs().first()
        return list.firstOrNull { it.cribUuid == cribNumber }
            ?: list.filter { it.cribNumber.equals(cribNumber.trim(), ignoreCase = true) }.singleOrNull()
    }

    suspend fun cachedSetup(): CribSetup {
        val user = userIdProvider() ?: return CribSetup()
        return feedCribDao.getSetup(user)?.let { runCatching { Json.decodeFromString<CribSetup>(it.json) }.getOrNull() } ?: CribSetup()
    }

    suspend fun refreshSetup(): Result<CribSetup> {
        if (!canManageCribs()) return Result.failure(IllegalStateException("Only administrators can add cribs."))
        val result = apiClient.fetchSetup()
        result.getOrNull()?.let { setup ->
            userIdProvider()?.let { feedCribDao.cacheSetup(FeedCribSetupEntity(it, Json.encodeToString(setup))) }
        }
        return result
    }

    suspend fun addCrib(form: AddCribForm): Map<String, String> {
        if (!canManageCribs()) return mapOf("form" to "Only administrators can add cribs.")
        val user = userIdProvider() ?: return mapOf("form" to "Sign in to add a crib.")
        val setup = cachedSetup()
        val all = feedCribDao.observeCribsSnapshot()
        val errors = validateCribForm(form, setup, all)
        if (errors.isNotEmpty()) return errors
        val context = captureContextProvider()
        if (context.deviceId.isBlank() || context.captureAt <= 0)
            return mapOf("form" to "Device identity and creation time are required. Sign in again.")
        val crib = FeedCribEntity(
            cribUuid = UUID.randomUUID().toString(),
            cribNumber = form.code.trim().uppercase(Locale.ROOT), siteId = form.siteId,
            penId = form.penId, rationId = form.rationId,
            description = form.description.trim(), method = form.method,
            initialAdi = form.adi.toDouble(), currentAdi = form.adi.toDouble(),
            animalsBegin = null, animalsIn = null, animalsOut = null, animalsClose = null,
            rfid = form.rfid.trim().uppercase(Locale.ROOT).ifBlank { null },
            createdBy = user, createdAt = context.captureAt, creationDeviceId = context.deviceId,
            creationGpsLat = context.gpsLat, creationGpsLng = context.gpsLng,
            syncStatus = "PENDING", updatedAt = context.captureAt
        )
        return try { feedCribDao.insertCrib(crib); emptyMap() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            Log.e("FeedCribRepository", "Local crib insert failed", e)
            mapOf("form" to "Could not save to this device. Please try again. Your entered values have been kept.")
        }
    }

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

    suspend fun resolveConflict(uuid: String, code: String, tag: String): Map<String, String> {
        val crib = feedCribDao.findCribUuid(uuid) ?: return mapOf("form" to "Crib not found.")
        if (!canManageCribs() || crib.createdBy != userIdProvider())
            return mapOf("form" to "Sign in as the administrator who created this crib.")
        if (crib.syncStatus != "CONFLICT") return mapOf("form" to "Only an unregistered, conflicted crib can be changed.")
        val form = AddCribForm(code, crib.siteId, crib.penId.orEmpty(), crib.rationId.orEmpty(),
            crib.initialAdi.toString(), crib.description, crib.method, tag)
        val errors = validateCribForm(form, cachedSetup(), feedCribDao.observeCribsSnapshot().filter { it.cribUuid != uuid })
        if (errors.isNotEmpty()) return errors
        return try {
            feedCribDao.resolveCribConflict(uuid, code.trim().uppercase(Locale.ROOT), tag.trim().uppercase(Locale.ROOT).ifBlank { null })
            emptyMap()
        } catch (e: Exception) { mapOf("form" to "Could not resolve conflict. Check the code and tag for duplicates.") }
    }

    // --- download ---

    /**
     * Downloads the site's cribs, the code table and the recent entries from every phone, and
     * replaces what is stored. A reading waiting to be sent is never touched. Returns the number
     * of cribs, or why it failed (offline, signed out); the old data stays in that case.
     */
    suspend fun refreshCribs(): Result<Int> {
        if (canManageCribs()) refreshSetup()
        val setup = cachedSetup()
        val sites = setup.sites.map { it.id }
        if (sites.isNotEmpty()) {
            var total = 0
            for (site in sites) {
                val result = downloadSite(site)
                if (result.isFailure) return result
                total += result.getOrThrow()
            }
            return Result.success(total)
        }
        return downloadSite(null)
    }

    private suspend fun downloadSite(site: String?): Result<Int> {
        val download = apiClient.fetchCribs(GRID_DAYS, site)
        val response = download.getOrElse { return Result.failure(it) }

        return try {
            val downloadedAt = now()
            if (response.pens.isNotEmpty() || response.rations.isNotEmpty()) {
                val old = cachedSetup()
                val updated = old.copy(
                    pens = old.pens.filter { it.siteId != response.siteId } + response.pens,
                    rations = response.rations
                )
                userIdProvider()?.let { feedCribDao.cacheSetup(FeedCribSetupEntity(it, Json.encodeToString(updated))) }
            }

            feedCribDao.applyDownload(
                cribs = response.cribs.map { FeedCribMappers.toEntity(it, downloadedAt) },
                codes = response.codes.map { FeedCribMappers.toEntity(it) },
                serverEntries = response.entries.map { FeedCribMappers.toEntity(it, response.siteId) },
                pruneBeforeDate = SlotAllocator.dateDaysBefore(downloadedAt, KEEP_SYNCED_DAYS, timeZone),
                siteId = response.siteId
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
     * local save is never lost to a sync problem. An active reading code is required.
     */
    suspend fun saveEntry(cribNumber: String, code: Int?, adi: Double): SaveEntryOutcome {
        if (code == null) return SaveEntryOutcome(validationError = "Select a reading code before saving.")
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
            cribUuid = crib.cribUuid,
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
        val byDevice = entries.groupBy { it.deviceId to it.siteId }

        if (byDevice.size == 1) {
            return apiClient.syncEntries(entries, deviceId = byDevice.keys.first().first, siteId = byDevice.keys.first().second)
        }

        val results = mutableListOf<FeedCribEntrySyncResult>()
        var firstFailure: Throwable? = null

        for ((identity, records) in byDevice) {
            apiClient.syncEntries(records, identity.first, identity.second).fold(
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
    /** Register parent cribs first; a refused parent never sends readings under another UUID. */
    private suspend fun syncCribRegistrations(userId: String): Map<String, String> {
        val allCribs = feedCribDao.pendingCribs(userId)
        val cribs = allCribs.filter { it.syncStatus != "CONFLICT" }
        val errors = allCribs.filter { it.syncStatus == "CONFLICT" }.associate {
            it.cribUuid to (it.syncError ?: "Crib conflict requires resolution.")
        }.toMutableMap()
        if (cribs.isEmpty()) return errors
        for (batch in cribs.chunked(200)) {
            val response = apiClient.syncCribs(batch.map { crib -> CribRegistration(
                crib.cribUuid, crib.cribNumber, crib.siteId, crib.penId.orEmpty(), crib.rationId.orEmpty(),
                crib.initialAdi ?: 0.0, crib.description, crib.method, crib.rfid,
                crib.createdAt ?: 0, crib.creationDeviceId.orEmpty(), crib.creationGpsLat, crib.creationGpsLng
            ) })
            val results = response.getOrNull()?.results?.associateBy { it.cribUuid }.orEmpty()
            batch.forEach { crib ->
                val result = results[crib.cribUuid]
                val status = when (result?.status) { "SYNCED" -> "SYNCED"; "CONFLICT" -> "CONFLICT"; else -> "PENDING" }
                val error = if (status == "SYNCED") null else result?.message
                    ?: response.exceptionOrNull()?.message ?: "Server did not acknowledge crib creation."
                feedCribDao.setCribSync(crib.cribUuid, status, error)
                if (error != null) errors[crib.cribUuid] = error
            }
        }
        feedCribDao.pendingCribs(userId).filter { it.syncStatus == "CONFLICT" }.forEach {
            errors[it.cribUuid] = it.syncError ?: "Crib conflict requires resolution."
        }
        return errors
    }
    suspend fun syncPending(retryRejected: Boolean = false): FeedCribSyncOutcome {

        val userId = userIdProvider()?.takeIf { it.isNotBlank() }
            ?: return FeedCribSyncOutcome(syncedCount = 0)

        return try {

            val cribErrors = syncCribRegistrations(userId).toMutableMap()
            val blockedCribs = feedCribDao.observeCribsSnapshot().filter { it.syncStatus != "SYNCED" }.map { it.cribUuid }.toSet()

            if (retryRejected) {
                feedCribDao.requeueRejected(userId)
            }

            val queued = pendingSyncRepository.getAllPendingOperations().filter { it.entityType == ENTITY_TYPE }

            val allPending = feedCribDao.getPending(userId)
            allPending.filter { it.cribUuid in blockedCribs }.forEach {
                cribErrors[it.recordGuid] = "Crib creation must be acknowledged or its conflict resolved before this reading can upload."
            }
            val pending = allPending.filter { it.cribUuid !in blockedCribs }
            if (pending.isEmpty() && (cribErrors.isNotEmpty() || allPending.any { it.cribUuid in blockedCribs })) {
                return FeedCribSyncOutcome(0, errorMessagesByRecordGuid = cribErrors.ifEmpty {
                    allPending.filter { it.cribUuid in blockedCribs }.associate { it.recordGuid to "Crib creation must be resolved before readings can upload. Sign in as its creator to sync it." }
                })
            }

            if (pending.isEmpty()) {
                /* Nothing genuinely waiting: clear stale queue rows and finish. */
                queued.forEach { pendingSyncRepository.markSyncSuccessful(it.id) }
                return FeedCribSyncOutcome(syncedCount = 0)
            }

            val pendingGuids = allPending.map { it.recordGuid }.toSet()

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
                    errorMessagesByRecordGuid = pending.associate { it.recordGuid to message } + cribErrors
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
                errorMessagesByRecordGuid = cribErrors + unsynced
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
