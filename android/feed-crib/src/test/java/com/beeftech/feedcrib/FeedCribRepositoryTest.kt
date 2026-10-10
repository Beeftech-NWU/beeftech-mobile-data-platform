package com.beeftech.feedcrib

import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedEntryOrigin
import com.beeftech.database.entity.FeedSlots
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.feedcrib.data.FeedCribCaptureContext
import com.beeftech.feedcrib.data.FeedCribRepository
import com.beeftech.feedcrib.fakes.FakeFeedCribDao
import com.beeftech.feedcrib.fakes.FakeFeedCribServer
import com.beeftech.feedcrib.fakes.FakePendingSyncDao
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FeedCribRepositoryTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun at(hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply { clear(); set(2026, Calendar.OCTOBER, 10, hour, minute, 0) }.timeInMillis

    private lateinit var dao: FakeFeedCribDao
    private lateinit var queueDao: FakePendingSyncDao
    private lateinit var queue: PendingSyncRepository
    private lateinit var server: FakeFeedCribServer

    private var clockMillis = at(12, 0)
    private var userId: String? = "user-1"
    private var deviceId = "dev-1"

    private fun repository() = FeedCribRepository(
        feedCribDao = dao,
        pendingSyncRepository = queue,
        apiClient = server.client(),
        captureContextProvider = { FeedCribCaptureContext(deviceId = deviceId, captureAt = clockMillis) },
        userIdProvider = { userId },
        timeZone = utc,
        now = { clockMillis }
    )

    @Before
    fun setUp() {
        dao = FakeFeedCribDao()
        queueDao = FakePendingSyncDao()
        queue = PendingSyncRepository(queueDao) { userId }
        server = FakeFeedCribServer()
        clockMillis = at(12, 0)
        userId = "user-1"
        deviceId = "dev-1"

        dao.cribs.value = listOf(
            FeedCribEntity(cribNumber = "A01", siteId = "site-1", currentAdi = 10.0),
            FeedCribEntity(cribNumber = "A02", siteId = "site-1"),
            FeedCribEntity(cribNumber = "OFF", siteId = "site-1", active = false)
        )
        dao.codes.value = (0..5).map { CribReadingCodeEntity(it, "Code $it") }
    }

    private fun pendingEntry(guid: String, user: String = "user-1", device: String = "dev-1", capturedAt: Long = 1_000) =
        FeedCribEntryEntity(
            recordGuid = guid, cribNumber = "A01", readingDate = "2026-10-10", slot = FeedSlots.MIDDAY,
            code = 3, adi = 11.0, capturedAt = capturedAt, deviceId = device, userId = user
        )

    private fun queuedGuids() = queueDao.snapshot().filter { it.entityType == FeedCribRepository.ENTITY_TYPE }.map { it.entityId }

    // --- download ---

    @Test
    fun `refresh stores the cribs, the codes and other phones' readings`() = runTest {
        dao.cribs.value = emptyList()
        dao.codes.value = emptyList()
        server.downloadJson = FakeFeedCribServer.sampleDownload()

        val result = repository().refreshCribs()

        assertEquals(2, result.getOrThrow())
        assertEquals(listOf("A01", "A02"), dao.cribs.value.map { it.cribNumber })
        assertEquals(clockMillis, dao.cribs.value.first().lastDownloadedAt)
        assertEquals((0..5).toList(), dao.codes.value.map { it.code })

        val history = dao.entry("server-1")!!
        assertEquals(FeedEntryOrigin.SERVER, history.origin)
        assertEquals("SYNCED", history.syncStatus)
        assertEquals("user-2", history.userId)
        assertEquals("site-1", history.siteId)
    }

    @Test
    fun `refresh asks for the last 3 days`() = runTest {
        server.downloadJson = FakeFeedCribServer.sampleDownload()

        repository().refreshCribs()

        assertTrue(server.downloadUrls.single().endsWith("days=3"))
    }

    @Test
    fun `a failed refresh keeps what is on the phone`() = runTest {
        server.offline = true

        val result = repository().refreshCribs()

        assertTrue(result.isFailure)
        assertEquals(listOf("A01", "A02", "OFF"), dao.cribs.value.map { it.cribNumber })
        assertEquals(6, dao.codes.value.size)
    }

    @Test
    fun `a refresh never touches a reading that is waiting to send and keeps the newest ADI`() = runTest {
        dao.insertEntry(pendingEntry("local-1", capturedAt = 99_999).copy(adi = 12.5))
        server.downloadJson = FakeFeedCribServer.sampleDownload()

        repository().refreshCribs()

        assertEquals("PENDING", dao.entry("local-1")!!.syncStatus)
        /* The server said 10.5, but the phone has a newer reading it has not sent. */
        assertEquals(12.5, dao.cribs.value.first { it.cribNumber == "A01" }.currentAdi!!, 0.0)
    }

    // --- save ---

    @Test
    fun `saving online files the reading by the clock, sends it and clears the queue`() = runTest {
        clockMillis = at(11, 0)

        val outcome = repository().saveEntry("A01", code = 3, adi = 11.2)

        assertTrue(outcome.saved)
        assertNull(outcome.syncErrorMessage)
        assertFalse(outcome.needsAttention)

        val saved = outcome.entry!!
        assertEquals("A01", saved.cribNumber)
        assertEquals("site-1", saved.siteId)
        assertEquals("2026-10-10", saved.readingDate)
        assertEquals(FeedSlots.MIDDAY, saved.slot)
        assertEquals(3, saved.code)
        assertEquals(11.2, saved.adi, 0.0)
        assertEquals("user-1", saved.userId)
        assertEquals("dev-1", saved.deviceId)
        assertEquals("SYNCED", saved.syncStatus)
        assertEquals(555L, saved.syncedAt)

        assertTrue(queuedGuids().isEmpty())
        assertEquals(1, server.syncRequests.size)
        assertEquals(saved.recordGuid, server.syncRequests.single().records.single().recordguid)
        assertEquals(11.2, dao.cribs.value.first { it.cribNumber == "A01" }.currentAdi!!, 0.0)
    }

    @Test
    fun `a reading before 11h00 is Morning and one at 20h30 is Evening`() = runTest {
        clockMillis = at(10, 59)
        assertEquals(FeedSlots.MORNING, repository().saveEntry("A01", 1, 10.0).entry!!.slot)

        clockMillis = at(20, 30)
        assertEquals(FeedSlots.EVENING, repository().saveEntry("A01", 2, 10.0).entry!!.slot)
    }

    @Test
    fun `the crib number is matched without regard to case or spaces`() = runTest {
        assertEquals("A01", repository().saveEntry("  a01 ", 1, 10.0).entry!!.cribNumber)
    }

    @Test
    fun `an ADI only reading has no code`() = runTest {
        val outcome = repository().saveEntry("A01", code = null, adi = 10.4)

        assertNull(outcome.entry!!.code)
        assertNull(server.syncRequests.single().records.single().code)
    }

    @Test
    fun `saving offline keeps the reading, queues it and says why it did not send`() = runTest {
        server.offline = true

        val outcome = repository().saveEntry("A01", code = 4, adi = 10.6)

        assertTrue(outcome.saved)
        assertNotNull(outcome.syncErrorMessage)
        assertFalse(outcome.needsAttention)
        assertEquals("PENDING", outcome.entry!!.syncStatus)
        assertEquals(listOf(outcome.entry!!.recordGuid), queuedGuids())
        /* The crib already shows the new ADI. */
        assertEquals(10.6, dao.cribs.value.first { it.cribNumber == "A01" }.currentAdi!!, 0.0)
    }

    @Test
    fun `a server error keeps the reading queued`() = runTest {
        server.syncHttpStatus = HttpStatusCode.InternalServerError

        val outcome = repository().saveEntry("A01", 1, 10.0)

        assertTrue(outcome.saved)
        assertEquals("PENDING", outcome.entry!!.syncStatus)
        assertEquals(1, queuedGuids().size)
    }

    @Test
    fun `input that cannot be saved stores nothing`() = runTest {
        val repo = repository()

        assertNotNull(repo.saveEntry("Z99", 1, 10.0).validationError)
        assertNotNull(repo.saveEntry("OFF", 1, 10.0).validationError)
        assertNotNull(repo.saveEntry("A01", 9, 10.0).validationError)
        assertNotNull(repo.saveEntry("A01", 1, -0.1).validationError)
        assertNotNull(repo.saveEntry("A01", 1, Double.NaN).validationError)

        userId = null
        assertNotNull(repo.saveEntry("A01", 1, 10.0).validationError)

        assertTrue(dao.entries.value.isEmpty())
        assertTrue(queuedGuids().isEmpty())
        assertTrue(server.syncRequests.isEmpty())
    }

    @Test
    fun `a rejected reading is counted, then stops retrying and leaves the queue`() = runTest {
        val repo = repository()
        server.defaultVerdict = "ERROR" to "Crib A01 is not active."

        val first = repo.saveEntry("A01", 1, 10.0)
        val guid = first.entry!!.recordGuid

        assertEquals("PENDING", first.entry!!.syncStatus)
        assertEquals("Crib A01 is not active.", first.syncErrorMessage)
        assertFalse(first.needsAttention)
        assertEquals(listOf(guid), queuedGuids())

        repo.syncPending()
        val last = repo.syncPending()

        assertEquals("REJECTED", dao.entry(guid)!!.syncStatus)
        assertEquals(FeedCribRepository.MAX_SERVER_REJECTIONS, dao.entry(guid)!!.syncAttempts)
        assertEquals("Crib A01 is not active.", dao.entry(guid)!!.syncError)
        assertEquals(mapOf(guid to "Crib A01 is not active."), last.rejectedByRecordGuid)
        assertTrue(last.errorMessagesByRecordGuid.isEmpty())
        assertTrue(queuedGuids().isEmpty())

        /* A rejected reading is not sent again by the background run. */
        server.syncRequests.clear()
        repo.syncPending()
        assertTrue(server.syncRequests.isEmpty())
    }

    @Test
    fun `the third rejection reports the reading as rejected`() = runTest {
        val repo = repository()
        server.defaultVerdict = "ERROR" to "Nope"
        val guid = repo.saveEntry("A01", 1, 10.0).entry!!.recordGuid
        repo.syncPending()

        val third = repo.syncPending()

        assertEquals(setOf(guid), third.rejectedByRecordGuid.keys)
    }

    @Test
    fun `the manual retry gives rejected readings a fresh set of attempts`() = runTest {
        val repo = repository()
        server.defaultVerdict = "ERROR" to "Nope"
        val guid = repo.saveEntry("A01", 1, 10.0).entry!!.recordGuid
        repo.syncPending()
        repo.syncPending()
        assertEquals("REJECTED", dao.entry(guid)!!.syncStatus)

        server.defaultVerdict = "SYNCED" to null
        val outcome = repo.syncPending(retryRejected = true)

        assertEquals(1, outcome.syncedCount)
        assertEquals("SYNCED", dao.entry(guid)!!.syncStatus)
        assertTrue(queuedGuids().isEmpty())
    }

    // --- background sync ---

    @Test
    fun `syncPending sends only the signed in user's readings`() = runTest {
        dao.entries.value = listOf(pendingEntry("mine"), pendingEntry("theirs", user = "user-2"))

        val outcome = repository().syncPending()

        assertEquals(1, outcome.syncedCount)
        assertEquals(listOf("mine"), server.syncRequests.single().records.map { it.recordguid })
        assertEquals("SYNCED", dao.entry("mine")!!.syncStatus)
        assertEquals("PENDING", dao.entry("theirs")!!.syncStatus)
    }

    @Test
    fun `readings from more than one device go out as one request per device`() = runTest {
        dao.entries.value = listOf(
            pendingEntry("a", device = "dev-1", capturedAt = 1),
            pendingEntry("b", device = "dev-2", capturedAt = 2),
            pendingEntry("c", device = "dev-1", capturedAt = 3)
        )

        val outcome = repository().syncPending()

        assertEquals(3, outcome.syncedCount)
        assertEquals(setOf("dev-1", "dev-2"), server.syncRequests.map { it.deviceId }.toSet())
        assertEquals(2, server.syncRequests.size)
    }

    @Test
    fun `a reading with no queue row is sent anyway and the queue row is repaired then cleared`() = runTest {
        dao.entries.value = listOf(pendingEntry("orphan"))

        val outcome = repository().syncPending()

        assertEquals(1, outcome.syncedCount)
        assertTrue(queuedGuids().isEmpty())
    }

    @Test
    fun `a stale queue row for a reading that is already synced is cleared`() = runTest {
        dao.entries.value = listOf(pendingEntry("done").copy(syncStatus = "SYNCED"))
        queue.queueOperation(FeedCribRepository.ENTITY_TYPE, "done", "UPSERT", "")
        queue.queueOperation(FeedCribRepository.ENTITY_TYPE, "gone", "UPSERT", "")

        val outcome = repository().syncPending()

        assertEquals(0, outcome.syncedCount)
        assertTrue(queuedGuids().isEmpty())
        assertTrue(server.syncRequests.isEmpty())
    }

    @Test
    fun `an unreachable server leaves everything pending and reports it per reading`() = runTest {
        dao.entries.value = listOf(pendingEntry("a"), pendingEntry("b"))
        server.offline = true

        val outcome = repository().syncPending()

        assertEquals(0, outcome.syncedCount)
        assertEquals(setOf("a", "b"), outcome.errorMessagesByRecordGuid.keys)
        assertEquals(listOf("PENDING", "PENDING"), dao.entries.value.map { it.syncStatus })
        assertEquals(0, dao.entries.value.sumOf { it.syncAttempts })
    }

    @Test
    fun `nothing is sent when nobody is signed in`() = runTest {
        dao.entries.value = listOf(pendingEntry("a"))
        userId = null

        assertEquals(0, repository().syncPending().syncedCount)
        assertTrue(server.syncRequests.isEmpty())
    }

    // --- reads ---

    @Test
    fun `the grid reads the last 3 days of the open crib, today included`() = runTest {
        dao.entries.value = listOf(
            pendingEntry("today").copy(readingDate = "2026-10-10"),
            pendingEntry("two-days").copy(readingDate = "2026-10-08", slot = FeedSlots.EVENING),
            pendingEntry("three-days").copy(readingDate = "2026-10-07")
        )

        val shown = repository().observeLastNineSlots("A01").first()

        assertEquals(listOf("today", "two-days"), shown.map { it.recordGuid })
    }

    @Test
    fun `today's session lists only the signed in user's readings from today`() = runTest {
        dao.entries.value = listOf(
            pendingEntry("mine"),
            pendingEntry("theirs", user = "user-2"),
            pendingEntry("yesterday").copy(readingDate = "2026-10-09")
        )

        val sessions = repository().observeTodaySessions().first()

        assertEquals(listOf("A01"), sessions.map { it.cribNumber })
        assertEquals(1, sessions.single().unsyncedCount)
        assertEquals(listOf("mine"), repository().observeTodayEntries("A01").first().map { it.recordGuid })

        userId = null
        assertTrue(repository().observeTodaySessions().first().isEmpty())
    }
}
