package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.dao.FeedCribDao
import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedEntryOrigin
import com.beeftech.database.entity.FeedSlots
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Device tests for the feed crib queries: the override rule, today's session and the download. */
@RunWith(AndroidJUnit4::class)
class FeedCribDaoTest {

    private lateinit var context: Context
    private lateinit var database: BeefTechDatabase
    private lateinit var dao: FeedCribDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("beeftech.db")

        val result = DatabaseFactory.create(
            context = context,
            passphrase = ByteArray(32) { index -> (index + 1).toByte() }
        )
        assertTrue("Database must open", result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database
        dao = database.feedCribDao()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase("beeftech.db")
    }

    private fun crib(number: String = "A01", currentAdi: Double? = null) =
        FeedCribEntity(cribNumber = number, siteId = "site-1", currentAdi = currentAdi)

    private fun entry(
        guid: String,
        crib: String = "A01",
        date: String = "2026-10-10",
        slot: String = FeedSlots.MORNING,
        code: Int? = 3,
        adi: Double = 10.0,
        capturedAt: Long = 1_000,
        userId: String = "user-1",
        status: String = "PENDING",
        origin: String = FeedEntryOrigin.LOCAL
    ) = FeedCribEntryEntity(
        recordGuid = guid, cribNumber = crib, readingDate = date, slot = slot, code = code,
        adi = adi, capturedAt = capturedAt, userId = userId, syncStatus = status, origin = origin
    )

    private suspend fun slots(crib: String = "A01", from: String = "2026-10-08") =
        dao.observeLastSlots(crib, from).first()

    @Test
    fun theNewestCodeInADateAndBlockOverridesAnEarlierOneButBothRowsStay() = runBlocking {
        dao.applyDownload(listOf(crib()), emptyList(), emptyList(), "0000-00-00")
        dao.insertEntry(entry("g-old", code = 2, capturedAt = 1_000))
        dao.insertEntry(entry("g-new", code = 4, capturedAt = 2_000))

        val shown = slots()

        assertEquals(listOf("g-new"), shown.map { it.recordGuid })
        assertEquals(4, shown.single().code)
        assertNotNull(dao.findEntry("g-old"))
    }

    @Test
    fun anAdiOnlyEntryNeverHidesACode() = runBlocking {
        dao.insertEntry(entry("g-code", code = 3, capturedAt = 1_000))
        dao.insertEntry(entry("g-adi", code = null, adi = 12.0, capturedAt = 2_000))

        assertEquals(listOf("g-code"), slots().map { it.recordGuid })
    }

    @Test
    fun aTieOnCapturedAtGoesToTheLargerGuid() = runBlocking {
        dao.insertEntry(entry("g-a", code = 1, capturedAt = 1_000))
        dao.insertEntry(entry("g-b", code = 5, capturedAt = 1_000))

        assertEquals(listOf("g-b"), slots().map { it.recordGuid })
    }

    @Test
    fun differentBlocksAndDatesAreKeptApartAndOldDatesFallOut() = runBlocking {
        dao.insertEntry(entry("today-m", date = "2026-10-10", slot = FeedSlots.MORNING))
        dao.insertEntry(entry("today-e", date = "2026-10-10", slot = FeedSlots.EVENING))
        dao.insertEntry(entry("yesterday-d", date = "2026-10-09", slot = FeedSlots.MIDDAY))
        dao.insertEntry(entry("old-m", date = "2026-10-05", slot = FeedSlots.MORNING))
        dao.insertEntry(entry("other-crib", crib = "A02", date = "2026-10-10"))

        assertEquals(
            listOf("today-e", "today-m", "yesterday-d"),
            slots().map { it.recordGuid }
        )
    }

    @Test
    fun savingAnEntryMovesTheCribAdiToTheNewestOne() = runBlocking {
        dao.applyDownload(listOf(crib(currentAdi = 9.0)), emptyList(), emptyList(), "0000-00-00")

        dao.insertEntry(entry("g-1", adi = 11.0, capturedAt = 2_000))
        assertEquals(11.0, dao.getCrib("A01")!!.currentAdi!!, 0.0)

        /* An older reading saved later does not roll it back. */
        dao.insertEntry(entry("g-0", adi = 8.0, capturedAt = 1_000))
        assertEquals(11.0, dao.getCrib("A01")!!.currentAdi!!, 0.0)
    }

    @Test
    fun todaysSessionHasOneLinePerCribWithTheNewestCodePerBlockAndTheLatestAdi() = runBlocking {
        dao.insertEntry(entry("a-m1", crib = "A01", slot = FeedSlots.MORNING, code = 1, adi = 10.0, capturedAt = 1_000))
        dao.insertEntry(entry("a-m2", crib = "A01", slot = FeedSlots.MORNING, code = 2, adi = 10.5, capturedAt = 2_000))
        dao.insertEntry(entry("a-d", crib = "A01", slot = FeedSlots.MIDDAY, code = 3, adi = 11.0, capturedAt = 3_000, status = "SYNCED"))
        dao.insertEntry(entry("b-e", crib = "A02", slot = FeedSlots.EVENING, code = 5, adi = 7.0, capturedAt = 4_000))
        /* Not in the session: another user, another date. */
        dao.insertEntry(entry("x-user", crib = "A03", userId = "user-2"))
        dao.insertEntry(entry("x-date", crib = "A04", date = "2026-10-09"))

        val rows = dao.observeSessions("2026-10-10", "user-1").first()

        assertEquals(listOf("A02", "A01"), rows.map { it.cribNumber })

        val a = rows.single { it.cribNumber == "A01" }
        assertEquals(2, a.morningCode)
        assertEquals(3, a.midDayCode)
        assertNull(a.eveningCode)
        assertEquals(11.0, a.latestAdi!!, 0.0)
        assertEquals(3_000L, a.lastCapturedAt)
        assertEquals(2, a.unsyncedCount)

        val b = rows.single { it.cribNumber == "A02" }
        assertEquals(5, b.eveningCode)
        assertNull(b.morningCode)
        assertEquals(1, b.unsyncedCount)
    }

    @Test
    fun theSessionDetailListsTheUsersEntriesOldestFirst() = runBlocking {
        dao.insertEntry(entry("late", capturedAt = 2_000))
        dao.insertEntry(entry("early", capturedAt = 1_000))
        dao.insertEntry(entry("other-user", userId = "user-2", capturedAt = 1_500))

        assertEquals(
            listOf("early", "late"),
            dao.observeEntriesForCrib("A01", "2026-10-10", "user-1").first().map { it.recordGuid }
        )
    }

    @Test
    fun aDownloadReplacesCribsAndCodesAndAddsNewServerEntriesOnly() = runBlocking {
        dao.applyDownload(listOf(crib("OLD")), listOf(CribReadingCodeEntity(9, "Old")), emptyList(), "0000-00-00")
        dao.insertEntry(entry("g-local", capturedAt = 5_000, adi = 12.0))

        dao.applyDownload(
            cribs = listOf(crib("A01", currentAdi = 9.0)),
            codes = listOf(CribReadingCodeEntity(0, "Empty"), CribReadingCodeEntity(1, "Trace")),
            serverEntries = listOf(
                /* The server has not seen the local one under this guid, but the phone's row must win if it did. */
                entry("g-local", capturedAt = 5_000, adi = 99.0, status = "SYNCED", origin = FeedEntryOrigin.SERVER),
                entry("g-server", userId = "user-2", capturedAt = 4_000, status = "SYNCED", origin = FeedEntryOrigin.SERVER)
            ),
            pruneBeforeDate = "0000-00-00"
        )

        assertEquals(listOf("A01"), dao.observeCribs().first().map { it.cribNumber })
        assertEquals(listOf(0, 1), dao.observeCodes().first().map { it.code })
        assertNull(dao.getCrib("OLD"))

        val local = dao.findEntry("g-local")!!
        assertEquals(12.0, local.adi, 0.0)
        assertEquals("PENDING", local.syncStatus)
        assertEquals(FeedEntryOrigin.SERVER, dao.findEntry("g-server")!!.origin)

        /* The download carried an older ADI, but the phone has a newer reading waiting. */
        assertEquals(12.0, dao.getCrib("A01")!!.currentAdi!!, 0.0)
    }

    @Test
    fun aDownloadPrunesOnlySyncedRowsOlderThanTheCutOff() = runBlocking {
        dao.insertEntry(entry("old-synced", date = "2026-09-01", status = "SYNCED"))
        dao.insertEntry(entry("old-pending", date = "2026-09-01", status = "PENDING"))
        dao.insertEntry(entry("recent-synced", date = "2026-10-09", status = "SYNCED"))

        dao.applyDownload(listOf(crib()), emptyList(), emptyList(), "2026-09-26")

        assertNull(dao.findEntry("old-synced"))
        assertNotNull(dao.findEntry("old-pending"))
        assertNotNull(dao.findEntry("recent-synced"))
    }

    @Test
    fun aCribWithNoEntriesKeepsTheServersAdi() = runBlocking {
        dao.applyDownload(listOf(crib("A01", currentAdi = 9.5)), emptyList(), emptyList(), "0000-00-00")

        assertEquals(9.5, dao.getCrib("A01")!!.currentAdi!!, 0.0)
    }

    @Test
    fun pendingEntriesAreTheUsersOwnInCaptureOrder() = runBlocking {
        dao.insertEntry(entry("b", capturedAt = 2_000))
        dao.insertEntry(entry("a", capturedAt = 1_000))
        dao.insertEntry(entry("synced", status = "SYNCED"))
        dao.insertEntry(entry("other", userId = "user-2"))

        assertEquals(listOf("a", "b"), dao.getPending("user-1").map { it.recordGuid })
    }

    @Test
    fun aRejectedEntryStopsBeingRetriedAtTheCapAndComesBackOnRequeue() = runBlocking {
        dao.insertEntry(entry("g-1"))

        dao.recordRejection("g-1", "Crib not found", 2)
        assertEquals("PENDING", dao.findEntry("g-1")!!.syncStatus)
        assertEquals(1, dao.findEntry("g-1")!!.syncAttempts)

        dao.recordRejection("g-1", "Crib not found", 2)
        val rejected = dao.findEntry("g-1")!!
        assertEquals("REJECTED", rejected.syncStatus)
        assertEquals("Crib not found", rejected.syncError)
        assertTrue(dao.getPending("user-1").isEmpty())

        dao.requeueRejected("user-1")
        val again = dao.findEntry("g-1")!!
        assertEquals("PENDING", again.syncStatus)
        assertEquals(0, again.syncAttempts)
        assertNull(again.syncError)
    }

    @Test
    fun markSyncedClearsTheErrorAndLeavesTheOthersPending() = runBlocking {
        dao.insertEntry(entry("g-1"))
        dao.insertEntry(entry("g-2"))
        dao.recordRejection("g-1", "x", 5)

        dao.markSynced(listOf("g-1"), 777)

        val synced = dao.findEntry("g-1")!!
        assertEquals("SYNCED", synced.syncStatus)
        assertEquals(777L, synced.syncedAt)
        assertNull(synced.syncError)
        assertEquals("PENDING", dao.findEntry("g-2")!!.syncStatus)
    }
}
