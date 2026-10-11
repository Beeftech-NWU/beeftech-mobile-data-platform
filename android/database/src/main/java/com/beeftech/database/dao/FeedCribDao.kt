package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedCribSetupEntity
import kotlinx.coroutines.flow.Flow

/**
 * One line of today's session: a crib the user took readings on, with the newest code in each
 * block, the newest ADI, and how many of the day's entries have not reached the server yet.
 */
data class FeedCribSessionRow(
    val cribNumber: String,
    val morningCode: Int?,
    val midDayCode: Int?,
    val eveningCode: Int?,
    val latestAdi: Double?,
    val lastCapturedAt: Long,
    val unsyncedCount: Int,
    val cribUuid: String? = null
)

@Dao
abstract class FeedCribDao {

    // --- master data (replaced on every download) ---

    @Query("SELECT * FROM feed_cribs ORDER BY crib_number")
    abstract fun observeCribs(): Flow<List<FeedCribEntity>>

    @Query("SELECT * FROM feed_cribs WHERE crib_uuid = :cribNumber OR crib_number = :cribNumber LIMIT 1")
    abstract suspend fun getCrib(cribNumber: String): FeedCribEntity?

    @Query("SELECT * FROM crib_reading_codes ORDER BY code")
    abstract fun observeCodes(): Flow<List<CribReadingCodeEntity>>

    @Query("SELECT MAX(last_downloaded_at) FROM feed_cribs")
    abstract fun observeLastDownloadedAt(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCribs(cribs: List<FeedCribEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCodes(codes: List<CribReadingCodeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertServerEntries(entries: List<FeedCribEntryEntity>)

    @Query("DELETE FROM feed_cribs WHERE sync_status = 'SYNCED' AND created_by IS NULL AND (:siteId IS NULL OR site_id = :siteId)")
    protected abstract suspend fun deleteAllCribs(siteId: String?)

    @Query("SELECT * FROM feed_cribs WHERE crib_uuid = :uuid")
    abstract suspend fun findCribUuid(uuid: String): FeedCribEntity?

    @Query("SELECT * FROM feed_cribs WHERE sync_status != 'SYNCED' AND created_by = :userId")
    abstract suspend fun pendingCribs(userId: String): List<FeedCribEntity>

    @Query("UPDATE feed_cribs SET sync_status = :status, sync_error = :error WHERE crib_uuid = :uuid")
    abstract suspend fun setCribSync(uuid: String, status: String, error: String?)

    @Query("UPDATE feed_cribs SET crib_number = :code, rfid = :tag, sync_status = 'PENDING', sync_error = NULL WHERE crib_uuid = :uuid AND sync_status = 'CONFLICT'")
    protected abstract suspend fun updateConflictedCrib(uuid: String, code: String, tag: String?)
    @Query("UPDATE feed_crib_entries SET crib_number = :code WHERE crib_uuid = :uuid")
    protected abstract suspend fun updateCribReadingLabels(uuid: String, code: String)
    @Transaction
    open suspend fun resolveCribConflict(uuid: String, code: String, tag: String?) {
        updateConflictedCrib(uuid, code, tag)
        updateCribReadingLabels(uuid, code)
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertCrib(crib: FeedCribEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun cacheSetup(setup: FeedCribSetupEntity)

    @Query("SELECT * FROM feed_crib_setup WHERE userId = :userId")
    abstract suspend fun getSetup(userId: String): FeedCribSetupEntity?

    @Query("DELETE FROM crib_reading_codes")
    protected abstract suspend fun deleteAllCodes()

    /** Only rows the server already has: an unsent reading is never pruned. */
    @Query("DELETE FROM feed_crib_entries WHERE sync_status = 'SYNCED' AND reading_date < :beforeDate")
    protected abstract suspend fun pruneSyncedBefore(beforeDate: String)

    /**
     * Sets every crib's ADI to its newest entry's, so a reading saved offline is not hidden by a
     * download that has not seen it yet. A crib with no entries keeps the server's value.
     */
    @Query(
        """
        UPDATE feed_cribs SET current_adi = COALESCE(
            (SELECT e.adi FROM feed_crib_entries e
             WHERE e.crib_uuid = feed_cribs.crib_uuid OR
                 (e.crib_uuid IS NULL AND e.crib_number = feed_cribs.crib_number AND
                  (e.site_id IS NULL OR e.site_id = feed_cribs.site_id))
             ORDER BY e.captured_at DESC, e.record_guid DESC LIMIT 1),
            current_adi)
        """
    )
    protected abstract suspend fun recomputeCurrentAdi()

    /**
     * Applies a download atomically. Cribs and codes are replaced. Entries from the server are
     * added when new; a row already here (a local one still waiting, say) is left as it is.
     * Synced rows older than [pruneBeforeDate] are dropped to keep the table small.
     */
    @Transaction
    open suspend fun applyDownload(
        cribs: List<FeedCribEntity>,
        codes: List<CribReadingCodeEntity>,
        serverEntries: List<FeedCribEntryEntity>,
        pruneBeforeDate: String,
        siteId: String? = null
    ) {
        deleteAllCribs(siteId)
        val preserved = observeCribsSnapshot()
        insertCribs(cribs.filter { incoming ->
            preserved.none { it.syncStatus != "SYNCED" &&
                ((it.siteId == incoming.siteId && it.cribNumber == incoming.cribNumber) ||
                    (it.rfid != null && it.rfid == incoming.rfid)) }
        }.map { incoming ->
            val old = preserved.firstOrNull { it.cribUuid == incoming.cribUuid }
            if (old?.createdBy == null) incoming else incoming.copy(
                createdBy = old.createdBy, createdAt = old.createdAt,
                creationDeviceId = old.creationDeviceId, creationGpsLat = old.creationGpsLat,
                creationGpsLng = old.creationGpsLng, initialAdi = old.initialAdi)
        })
        deleteAllCodes()
        insertCodes(codes)
        if (serverEntries.isNotEmpty()) insertServerEntries(serverEntries)
        pruneSyncedBefore(pruneBeforeDate)
        recomputeCurrentAdi()
    }

    @Query("SELECT * FROM feed_cribs")
    abstract suspend fun observeCribsSnapshot(): List<FeedCribEntity>

    // --- entries ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertEntryRow(entry: FeedCribEntryEntity)

    @Query(
        """
        UPDATE feed_cribs SET current_adi = (
            SELECT e.adi FROM feed_crib_entries e
            WHERE e.crib_uuid = :cribNumber OR (e.crib_uuid IS NULL AND e.crib_number = :cribNumber)
            ORDER BY e.captured_at DESC, e.record_guid DESC LIMIT 1)
        WHERE crib_uuid = :cribNumber OR crib_number = :cribNumber
        """
    )
    protected abstract suspend fun refreshCurrentAdi(cribNumber: String)

    /** Saves a reading and moves the crib's ADI to the newest one, together. */
    @Transaction
    open suspend fun insertEntry(entry: FeedCribEntryEntity) {
        insertEntryRow(entry)
        refreshCurrentAdi(entry.cribUuid ?: entry.cribNumber)
    }

    /**
     * The codes that show in a crib's grid since [fromDate]: for each date and block, the entry
     * with the newest captured_at (ties go to the larger guid). An ADI-only entry (no code)
     * never hides a code.
     */
    @Query(
        """
        SELECT * FROM feed_crib_entries e
        WHERE (e.crib_uuid = :cribNumber OR e.crib_number = :cribNumber) AND e.reading_date >= :fromDate AND e.code IS NOT NULL
          AND NOT EXISTS (
            SELECT 1 FROM feed_crib_entries n
            WHERE COALESCE(n.crib_uuid, n.crib_number) = COALESCE(e.crib_uuid, e.crib_number) AND n.reading_date = e.reading_date
              AND n.slot = e.slot AND n.code IS NOT NULL
              AND (n.captured_at > e.captured_at
                   OR (n.captured_at = e.captured_at AND n.record_guid > e.record_guid)))
        ORDER BY e.reading_date DESC, e.slot
        """
    )
    abstract fun observeLastSlots(cribNumber: String, fromDate: String): Flow<List<FeedCribEntryEntity>>

    /** Every entry the user took on a crib on a date, oldest first. For the read-only session detail. */
    @Query(
        """
        SELECT * FROM feed_crib_entries
        WHERE (crib_uuid = :cribNumber OR crib_number = :cribNumber) AND reading_date = :date AND user_id = :userId
        ORDER BY captured_at, record_guid
        """
    )
    abstract fun observeEntriesForCrib(cribNumber: String, date: String, userId: String): Flow<List<FeedCribEntryEntity>>

    /** One line per crib the user took a reading on today, newest first. */
    @Query(
        """
        SELECT c.crib_number AS cribNumber, c.crib_uuid AS cribUuid,
          (SELECT e.code FROM feed_crib_entries e WHERE COALESCE(e.crib_uuid, e.crib_number) = COALESCE(c.crib_uuid, c.crib_number)
             AND e.reading_date = :date AND e.user_id = :userId AND e.slot = 'MORNING' AND e.code IS NOT NULL
             ORDER BY e.captured_at DESC, e.record_guid DESC LIMIT 1) AS morningCode,
          (SELECT e.code FROM feed_crib_entries e WHERE COALESCE(e.crib_uuid, e.crib_number) = COALESCE(c.crib_uuid, c.crib_number)
             AND e.reading_date = :date AND e.user_id = :userId AND e.slot = 'MIDDAY' AND e.code IS NOT NULL
             ORDER BY e.captured_at DESC, e.record_guid DESC LIMIT 1) AS midDayCode,
          (SELECT e.code FROM feed_crib_entries e WHERE COALESCE(e.crib_uuid, e.crib_number) = COALESCE(c.crib_uuid, c.crib_number)
             AND e.reading_date = :date AND e.user_id = :userId AND e.slot = 'EVENING' AND e.code IS NOT NULL
             ORDER BY e.captured_at DESC, e.record_guid DESC LIMIT 1) AS eveningCode,
          (SELECT e.adi FROM feed_crib_entries e WHERE COALESCE(e.crib_uuid, e.crib_number) = COALESCE(c.crib_uuid, c.crib_number)
             AND e.reading_date = :date AND e.user_id = :userId
             ORDER BY e.captured_at DESC, e.record_guid DESC LIMIT 1) AS latestAdi,
          (SELECT MAX(e.captured_at) FROM feed_crib_entries e WHERE COALESCE(e.crib_uuid, e.crib_number) = COALESCE(c.crib_uuid, c.crib_number)
             AND e.reading_date = :date AND e.user_id = :userId) AS lastCapturedAt,
          (SELECT COUNT(*) FROM feed_crib_entries e WHERE COALESCE(e.crib_uuid, e.crib_number) = COALESCE(c.crib_uuid, c.crib_number)
             AND e.reading_date = :date AND e.user_id = :userId AND e.sync_status != 'SYNCED') AS unsyncedCount
        FROM (SELECT DISTINCT crib_number, crib_uuid FROM feed_crib_entries
              WHERE reading_date = :date AND user_id = :userId) c
        ORDER BY lastCapturedAt DESC
        """
    )
    abstract fun observeSessions(date: String, userId: String): Flow<List<FeedCribSessionRow>>

    // --- sync ---

    /** Entries still to upload for [userId]. The table is the source of truth, not the pending_sync queue. */
    @Query("SELECT * FROM feed_crib_entries WHERE sync_status = 'PENDING' AND user_id = :userId ORDER BY captured_at, record_guid")
    abstract suspend fun getPending(userId: String): List<FeedCribEntryEntity>

    @Query("SELECT * FROM feed_crib_entries WHERE record_guid = :recordGuid LIMIT 1")
    abstract suspend fun findEntry(recordGuid: String): FeedCribEntryEntity?

    @Query("UPDATE feed_crib_entries SET sync_status = 'SYNCED', synced_at = :syncedAt, sync_error = NULL, sync_attempts = 0 WHERE record_guid IN (:recordGuids)")
    abstract suspend fun markSynced(recordGuids: List<String>, syncedAt: Long)

    /**
     * The server explicitly rejected this record. Stores its message and counts the attempt;
     * once [maxAttempts] is reached the record becomes REJECTED and stops being retried until
     * [requeueRejected].
     */
    @Query(
        """
        UPDATE feed_crib_entries
        SET sync_error = :message,
            sync_attempts = sync_attempts + 1,
            sync_status = CASE WHEN sync_attempts + 1 >= :maxAttempts THEN 'REJECTED' ELSE sync_status END
        WHERE record_guid = :recordGuid AND sync_status = 'PENDING'
        """
    )
    abstract suspend fun recordRejection(recordGuid: String, message: String, maxAttempts: Int)

    /** Manual retry: give every REJECTED record of [userId] a fresh set of attempts. */
    @Query("UPDATE feed_crib_entries SET sync_status = 'PENDING', sync_attempts = 0, sync_error = NULL WHERE sync_status = 'REJECTED' AND user_id = :userId")
    abstract suspend fun requeueRejected(userId: String)
}
