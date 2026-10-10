package com.beeftech.feedcrib.fakes

import com.beeftech.database.dao.FeedCribDao
import com.beeftech.database.dao.FeedCribSessionRow
import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedSlots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [FeedCribDao]. The transaction methods (applyDownload, insertEntry) are the real
 * ones; only the SQL underneath is replaced by Kotlin that follows the same rules, so the
 * repository and view model tests run without Room. The SQL itself is covered by FeedCribDaoTest
 * on a device.
 */
class FakeFeedCribDao : FeedCribDao() {

    val cribs = MutableStateFlow<List<FeedCribEntity>>(emptyList())
    val codes = MutableStateFlow<List<CribReadingCodeEntity>>(emptyList())
    val entries = MutableStateFlow<List<FeedCribEntryEntity>>(emptyList())

    private val newestFirst = compareByDescending<FeedCribEntryEntity> { it.capturedAt }.thenByDescending { it.recordGuid }

    fun entry(recordGuid: String): FeedCribEntryEntity? = entries.value.firstOrNull { it.recordGuid == recordGuid }

    // --- master data ---

    override fun observeCribs(): Flow<List<FeedCribEntity>> = cribs.map { list -> list.sortedBy { it.cribNumber } }

    override suspend fun getCrib(cribNumber: String): FeedCribEntity? = cribs.value.firstOrNull { it.cribNumber == cribNumber }

    override fun observeCodes(): Flow<List<CribReadingCodeEntity>> = codes.map { list -> list.sortedBy { it.code } }

    override fun observeLastDownloadedAt(): Flow<Long?> = cribs.map { list -> list.maxOfOrNull { it.lastDownloadedAt } }

    override suspend fun insertCribs(cribs: List<FeedCribEntity>) {
        this.cribs.value = this.cribs.value.filter { old -> cribs.none { it.cribNumber == old.cribNumber } } + cribs
    }

    override suspend fun insertCodes(codes: List<CribReadingCodeEntity>) {
        this.codes.value = this.codes.value.filter { old -> codes.none { it.code == old.code } } + codes
    }

    override suspend fun insertServerEntries(entries: List<FeedCribEntryEntity>) {
        val known = this.entries.value.map { it.recordGuid }.toSet()
        this.entries.value = this.entries.value + entries.filter { it.recordGuid !in known }
    }

    override suspend fun deleteAllCribs() {
        cribs.value = emptyList()
    }

    override suspend fun deleteAllCodes() {
        codes.value = emptyList()
    }

    override suspend fun pruneSyncedBefore(beforeDate: String) {
        entries.value = entries.value.filterNot { it.syncStatus == "SYNCED" && it.readingDate < beforeDate }
    }

    override suspend fun recomputeCurrentAdi() {
        cribs.value = cribs.value.map { crib ->
            val newest = entries.value.filter { it.cribNumber == crib.cribNumber }.sortedWith(newestFirst).firstOrNull()
            if (newest == null) crib else crib.copy(currentAdi = newest.adi)
        }
    }

    // --- entries ---

    override suspend fun insertEntryRow(entry: FeedCribEntryEntity) {
        require(entries.value.none { it.recordGuid == entry.recordGuid }) { "Duplicate record guid ${entry.recordGuid}" }
        entries.value = entries.value + entry
    }

    override suspend fun refreshCurrentAdi(cribNumber: String) {
        val newest = entries.value.filter { it.cribNumber == cribNumber }.sortedWith(newestFirst).firstOrNull() ?: return
        cribs.value = cribs.value.map { if (it.cribNumber == cribNumber) it.copy(currentAdi = newest.adi) else it }
    }

    override fun observeLastSlots(cribNumber: String, fromDate: String): Flow<List<FeedCribEntryEntity>> =
        entries.map { all ->
            all.filter { it.cribNumber == cribNumber && it.readingDate >= fromDate && it.code != null }
                .groupBy { it.readingDate to it.slot }
                .map { (_, group) -> group.sortedWith(newestFirst).first() }
                .sortedWith(compareByDescending<FeedCribEntryEntity> { it.readingDate }.thenBy { it.slot })
        }

    override fun observeEntriesForCrib(cribNumber: String, date: String, userId: String): Flow<List<FeedCribEntryEntity>> =
        entries.map { all ->
            all.filter { it.cribNumber == cribNumber && it.readingDate == date && it.userId == userId }
                .sortedWith(compareBy<FeedCribEntryEntity> { it.capturedAt }.thenBy { it.recordGuid })
        }

    override fun observeSessions(date: String, userId: String): Flow<List<FeedCribSessionRow>> =
        entries.map { all ->
            val today = all.filter { it.readingDate == date && it.userId == userId }

            today.groupBy { it.cribNumber }.map { (crib, group) ->
                fun codeIn(slot: String) =
                    group.filter { it.slot == slot && it.code != null }.sortedWith(newestFirst).firstOrNull()?.code

                FeedCribSessionRow(
                    cribNumber = crib,
                    morningCode = codeIn(FeedSlots.MORNING),
                    midDayCode = codeIn(FeedSlots.MIDDAY),
                    eveningCode = codeIn(FeedSlots.EVENING),
                    latestAdi = group.sortedWith(newestFirst).first().adi,
                    lastCapturedAt = group.maxOf { it.capturedAt },
                    unsyncedCount = group.count { it.syncStatus != "SYNCED" }
                )
            }.sortedByDescending { it.lastCapturedAt }
        }

    // --- sync ---

    override suspend fun getPending(userId: String): List<FeedCribEntryEntity> =
        entries.value.filter { it.syncStatus == "PENDING" && it.userId == userId }
            .sortedWith(compareBy<FeedCribEntryEntity> { it.capturedAt }.thenBy { it.recordGuid })

    override suspend fun findEntry(recordGuid: String): FeedCribEntryEntity? = entry(recordGuid)

    override suspend fun markSynced(recordGuids: List<String>, syncedAt: Long) {
        entries.value = entries.value.map {
            if (it.recordGuid in recordGuids) it.copy(syncStatus = "SYNCED", syncedAt = syncedAt, syncError = null, syncAttempts = 0) else it
        }
    }

    override suspend fun recordRejection(recordGuid: String, message: String, maxAttempts: Int) {
        entries.value = entries.value.map {
            if (it.recordGuid == recordGuid && it.syncStatus == "PENDING") {
                val attempts = it.syncAttempts + 1
                it.copy(
                    syncError = message,
                    syncAttempts = attempts,
                    syncStatus = if (attempts >= maxAttempts) "REJECTED" else it.syncStatus
                )
            } else it
        }
    }

    override suspend fun requeueRejected(userId: String) {
        entries.value = entries.value.map {
            if (it.syncStatus == "REJECTED" && it.userId == userId) it.copy(syncStatus = "PENDING", syncAttempts = 0, syncError = null) else it
        }
    }
}
