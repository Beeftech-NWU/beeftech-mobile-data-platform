package com.beeftech.feedcrib

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedSlots
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.feedcrib.data.FeedCribCaptureContext
import com.beeftech.feedcrib.data.FeedCribRepository
import com.beeftech.feedcrib.fakes.FakeFeedCribDao
import com.beeftech.feedcrib.fakes.FakeFeedCribServer
import com.beeftech.feedcrib.fakes.FakePendingSyncDao
import com.beeftech.feedcrib.viewmodel.CribDetailState
import com.beeftech.feedcrib.viewmodel.FeedCribViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class FeedCribViewModelTest {

    private val mockContext: Context = object : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    private val utc = TimeZone.getTimeZone("UTC")

    private fun at(hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply { clear(); set(2026, Calendar.OCTOBER, 10, hour, minute, 0) }.timeInMillis

    private lateinit var dao: FakeFeedCribDao
    private lateinit var server: FakeFeedCribServer
    private var clockMillis = at(12, 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)

        dao = FakeFeedCribDao()
        server = FakeFeedCribServer()
        clockMillis = at(12, 0)

        dao.cribs.value = listOf(
            FeedCribEntity(cribNumber = "A01", siteId = "site-1", currentAdi = 10.0),
            FeedCribEntity(cribNumber = "A02", siteId = "site-1")
        )
        dao.codes.value = (0..5).map { CribReadingCodeEntity(it, "Code $it") }
    }

    private val viewModelJobs = mutableListOf<Job>()

    @After
    fun tearDown() {
        runBlocking { viewModelJobs.forEach { it.cancelAndJoin() } }
        Dispatchers.resetMain()
    }

    private fun viewModel(manageCribs: Boolean = false): FeedCribViewModel {
        val repository = FeedCribRepository(
            feedCribDao = dao,
            pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao()) { "user-1" },
            apiClient = server.client(),
            captureContextProvider = { FeedCribCaptureContext(deviceId = "dev-1", captureAt = clockMillis) },
            userIdProvider = { "user-1" },
            timeZone = utc,
            now = { clockMillis },
            canManageCribs = { manageCribs }
        )
        return FeedCribViewModel(repository, mockContext, { clockMillis }, utc, { manageCribs }).also {
            viewModelJobs += it.viewModelScope.coroutineContext[Job]!!
        }
    }

    @Test
    fun `adding a crib reports success only after persistence and makes it selectable offline`() = runBlocking {
        val vm = viewModel(manageCribs = true)
        vm.prepareAddCrib()
        withTimeout(5_000) { vm.cribSetup.first { it.sites.isNotEmpty() } }
        server.offline = true
        val saved = CompletableDeferred<Unit>()
        vm.addCrib(com.beeftech.feedcrib.data.AddCribForm("A09", "site-1", "pen-1", "ration-1", "1.5")) {
            assertTrue(dao.cribs.value.any { it.cribNumber == "A09" })
            saved.complete(Unit)
        }
        withTimeout(5_000) { saved.await() }
        assertFalse(vm.addingCrib.value)
        assertTrue(vm.cribErrors.value.isEmpty())
        val crib = withTimeout(5_000) { vm.cribs.first { list -> list.any { it.cribNumber == "A09" } } }
            .single { it.cribNumber == "A09" }
        assertTrue(vm.open(crib.cribUuid))
        assertTrue(dao.entries.value.isEmpty())
    }

    @Test
    fun `failed add retains errors and allows a corrected retry without a partial crib`() = runBlocking {
        val vm = viewModel(manageCribs = true)
        vm.prepareAddCrib()
        withTimeout(5_000) { vm.cribSetup.first { it.sites.isNotEmpty() } }
        vm.addCrib(com.beeftech.feedcrib.data.AddCribForm()) { error("Invalid crib reported as saved") }
        withTimeout(5_000) { vm.cribErrors.first { it.isNotEmpty() } }
        assertFalse(vm.addingCrib.value)
        assertEquals(2, dao.cribs.value.size)
        val saved = CompletableDeferred<Unit>()
        vm.addCrib(com.beeftech.feedcrib.data.AddCribForm("A09", "site-1", "pen-1", "ration-1", "0")) {
            saved.complete(Unit)
        }
        withTimeout(5_000) { saved.await() }
        assertTrue(vm.cribErrors.value.isEmpty())
        assertEquals(3, dao.cribs.value.size)
    }

    private suspend fun FeedCribViewModel.open(number: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        openCrib(number) { result.complete(it) }
        val opened = withTimeout(5_000) { result.await() }
        if (opened) awaitDetail { it != null }
        return opened
    }

    /** The detail flow hops threads between its operators, so wait for it to show what the test expects. */
    private suspend fun FeedCribViewModel.awaitDetail(until: (CribDetailState?) -> Boolean): CribDetailState? =
        withTimeout(5_000) { detail.first(until) }

    private suspend fun FeedCribViewModel.saveNow(): Pair<Boolean, String> {
        val result = CompletableDeferred<Pair<Boolean, String>>()
        save { ok, message -> result.complete(ok to message) }
        return withTimeout(5_000) { result.await() }
    }

    private fun entry(guid: String, date: String, slot: String, code: Int, capturedAt: Long) = FeedCribEntryEntity(
        recordGuid = guid, cribNumber = "A01", readingDate = date, slot = slot, code = code,
        adi = 10.0, capturedAt = capturedAt, userId = "user-1"
    )

    @Test
    fun `the lists come from the database`() = runBlocking {
        val vm = viewModel()

        assertEquals(listOf("A01", "A02"), withTimeout(5_000) { vm.cribs.first { it.size == 2 } }.map { it.cribNumber })
        assertEquals((0..5).toList(), withTimeout(5_000) { vm.codes.first { it.size == 6 } }.map { it.code })
        assertNull(vm.detail.value)
    }

    @Test
    fun `opening a crib starts the draft at its ADI with no code chosen`() = runBlocking {
        val vm = viewModel()

        assertTrue(vm.open("a01"))

        val detail = vm.detail.value!!
        assertEquals("A01", detail.crib.cribNumber)
        assertEquals(10.0, detail.draft.adi, 0.0)
        assertNull(detail.draft.code)
        assertFalse(detail.draft.changed)
    }

    @Test
    fun `a crib with no ADI yet starts at zero`() = runBlocking {
        val vm = viewModel()

        vm.open("A02")

        assertEquals(0.0, vm.detail.value!!.draft.adi, 0.0)
    }

    @Test
    fun `opening a crib the phone does not have answers false and opens nothing`() = runBlocking {
        val vm = viewModel()

        assertFalse(vm.open("Z99"))
        assertNull(vm.detail.value)
    }

    @Test
    fun `the open crib knows where a reading saved now would go`() = runBlocking {
        val vm = viewModel()

        clockMillis = at(10, 59)
        vm.open("A01")
        assertEquals(FeedSlots.MORNING, vm.detail.value!!.currentSlot)
        assertEquals("2026-10-10", vm.detail.value!!.currentDate)

        /* The slot is worked out when the draft changes, so the screen shows the clock as it nudges ADI. */
        clockMillis = at(14, 0)
        vm.adjustAdi(1)
        assertEquals(FeedSlots.EVENING, vm.awaitDetail { it?.currentSlot == FeedSlots.EVENING }!!.currentSlot)
    }

    @Test
    fun `the grid shows the codes of the last 3 days`() = runBlocking {
        dao.entries.value = listOf(
            entry("e1", "2026-10-10", FeedSlots.MORNING, 2, 1),
            entry("e2", "2026-10-09", FeedSlots.EVENING, 4, 2),
            entry("old", "2026-10-06", FeedSlots.MORNING, 1, 3)
        )
        val vm = viewModel()

        vm.open("A01")

        assertEquals(listOf("e1", "e2"), vm.detail.value!!.lastSlots.map { it.recordGuid })
    }

    @Test
    fun `picking a code twice clears it`() = runBlocking {
        val vm = viewModel()
        vm.open("A01")

        vm.selectCode(3)
        assertTrue(vm.awaitDetail { it?.draft?.code == 3 }!!.draft.changed)

        vm.selectCode(5)
        vm.awaitDetail { it?.draft?.code == 5 }

        vm.selectCode(5)
        assertFalse(vm.awaitDetail { it != null && it.draft.code == null }!!.draft.changed)
    }

    @Test
    fun `the ADI stepper moves 0 point 1 at a time and stops at zero`() = runBlocking {
        val vm = viewModel()
        vm.open("A02")

        vm.adjustAdi(-1)
        assertEquals(0.0, vm.awaitDetail { it?.draft?.adiChanged == true }!!.draft.adi, 0.0)

        vm.adjustAdi(1)
        vm.adjustAdi(1)
        vm.adjustAdi(1)
        val detail = vm.awaitDetail { it?.draft?.adi == 0.3 }!!
        assertEquals(0.3, detail.draft.adi, 0.0)
        assertTrue(detail.draft.changed)
    }

    @Test
    fun `discard closes the crib and forgets the draft`() = runBlocking {
        val vm = viewModel()
        vm.open("A01")
        vm.selectCode(3)
        vm.awaitDetail { it?.draft?.code == 3 }

        vm.discard()

        vm.awaitDetail { it == null }
        assertTrue(dao.entries.value.isEmpty())

        vm.open("A01")
        assertNull(vm.detail.value!!.draft.code)
    }

    @Test
    fun `saving with nothing changed is refused and the crib stays open`() = runBlocking {
        val vm = viewModel()
        vm.open("A01")

        val (ok, message) = vm.saveNow()

        assertFalse(ok)
        assertTrue(message.contains("code"))
        assertNotNull(vm.detail.value)
        assertTrue(dao.entries.value.isEmpty())
    }

    @Test
    fun `saving with no crib open is refused`() = runBlocking {
        val (ok, _) = viewModel().saveNow()

        assertFalse(ok)
    }

    @Test
    fun `saving appends the reading, closes the crib and says where it went`() = runBlocking {
        val vm = viewModel()
        clockMillis = at(12, 30)
        vm.open("A01")
        vm.selectCode(3)
        vm.adjustAdi(2)
        vm.awaitDetail { it?.draft?.code == 3 && it.draft.adi == 10.2 }

        val (ok, message) = vm.saveNow()

        assertTrue(ok)
        assertEquals("Saved A01 · Mid-Day 3", message)
        vm.awaitDetail { it == null }

        val saved = dao.entries.value.single()
        assertEquals(FeedSlots.MIDDAY, saved.slot)
        assertEquals(3, saved.code)
        assertEquals(10.2, saved.adi, 0.0)
        assertEquals("SYNCED", saved.syncStatus)
    }

    @Test
    fun `ADI changes cannot be saved until a reading code is selected`() = runBlocking {
        val vm = viewModel()
        vm.open("A01")
        vm.adjustAdi(-3)
        vm.awaitDetail { it?.draft?.adi == 9.7 }

        val (ok, message) = vm.saveNow()

        assertFalse(ok)
        assertEquals("Select a reading code before saving.", message)
        assertTrue(dao.entries.value.isEmpty())
        assertEquals(9.7, vm.detail.value!!.draft.adi, 0.0)
        vm.selectCode(0)
        assertTrue(vm.saveNow().first)
        assertEquals(0, dao.entries.value.single().code)
        assertEquals(9.7, dao.entries.value.single().adi, 0.0)
    }

    @Test
    fun `saving offline still saves and says it will send when online`() = runBlocking {
        server.offline = true
        val vm = viewModel()
        vm.open("A01")
        vm.selectCode(1)
        vm.awaitDetail { it?.draft?.code == 1 }

        val (ok, message) = vm.saveNow()

        assertTrue(ok)
        assertTrue(message.endsWith("will send when online"))
        assertEquals("PENDING", dao.entries.value.single().syncStatus)
        assertNull(vm.awaitDetail { it == null })
    }

    @Test
    fun `a second Save tap while the first is still sending stores one reading`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        server.gate = gate
        val vm = viewModel()
        vm.open("A01")
        vm.selectCode(1)
        vm.awaitDetail { it?.draft?.code == 1 }

        val first = CompletableDeferred<Pair<Boolean, String>>()
        vm.save { ok, message -> first.complete(ok to message) }
        assertTrue(vm.saving.value)

        // A save already stored locally cannot be discarded or changed during upload.
        vm.discard()
        vm.selectCode(4)
        vm.adjustAdi(1)
        assertEquals(1, vm.detail.value?.draft?.code)
        assertEquals("A01", vm.detail.value?.crib?.cribNumber)

        var secondAnswered = false
        vm.save { _, _ -> secondAnswered = true }

        gate.complete(Unit)
        withTimeout(5_000) { first.await() }

        assertEquals(1, dao.entries.value.size)
        assertEquals(false, secondAnswered)
        assertEquals(false, vm.saving.value)
    }

    @Test
    fun `a saved reading appears in today's session`() = runBlocking {
        val vm = viewModel()
        vm.open("A01")
        vm.selectCode(2)
        vm.awaitDetail { it?.draft?.code == 2 }
        vm.saveNow()

        vm.loadSessions()

        val session = withTimeout(5_000) { vm.sessions.first { it.isNotEmpty() } }.single()
        assertEquals("A01", session.cribNumber)
        assertEquals(2, session.midDayCode)
        assertEquals(1, vm.entriesToday("A01").first().size)
    }

    @Test
    fun `refresh reports how many cribs it loaded`() = runBlocking {
        server.downloadJson = FakeFeedCribServer.sampleDownload()
        val vm = viewModel()
        val result = CompletableDeferred<Pair<Boolean, String>>()

        vm.refresh { ok, message -> result.complete(ok to message) }

        assertEquals(true to "2 cribs loaded.", withTimeout(5_000) { result.await() })
        assertFalse(vm.refreshing.value)
        /* The StateFlows hop threads, so wait for them rather than read them the instant the callback fires. */
        assertNotNull(withTimeout(5_000) { vm.lastDownloadedAt.first { it != null } })
    }

    @Test
    fun `refresh offline says so and keeps the list`() = runBlocking {
        server.offline = true
        val vm = viewModel()
        val result = CompletableDeferred<Pair<Boolean, String>>()

        vm.refresh { ok, message -> result.complete(ok to message) }

        val (ok, message) = withTimeout(5_000) { result.await() }
        assertFalse(ok)
        assertTrue(message.isNotBlank())
        assertFalse(vm.refreshing.value)
        assertEquals(2, withTimeout(5_000) { vm.cribs.first { it.size == 2 } }.size)
    }

    @Test
    fun `retry sync sends what is waiting`() = runBlocking {
        server.offline = true
        val vm = viewModel()
        vm.open("A01")
        vm.selectCode(1)
        vm.awaitDetail { it?.draft?.code == 1 }
        vm.saveNow()
        server.offline = false
        val result = CompletableDeferred<Pair<Boolean, String>>()

        vm.retrySync { ok, message -> result.complete(ok to message) }

        assertEquals(true to "1 reading sent. Cribs refreshed.", withTimeout(5_000) { result.await() })
        assertEquals("SYNCED", dao.entries.value.single().syncStatus)
        assertEquals(1, server.downloadUrls.size)
        assertEquals("1 reading sent. Cribs refreshed.", vm.syncMessage.value)
    }

    @Test
    fun `retry sync with nothing waiting says so`() = runBlocking {
        val result = CompletableDeferred<Pair<Boolean, String>>()

        viewModel().retrySync { ok, message -> result.complete(ok to message) }

        assertEquals(true to "There are no readings waiting to send. Cribs refreshed.", withTimeout(5_000) { result.await() })
        assertEquals(1, server.downloadUrls.size)
    }

    @Test
    fun `sync with no pending readings still reports a failed reference download`() = runBlocking {
        server.offline = true
        val vm = viewModel()
        val result = CompletableDeferred<Pair<Boolean, String>>()
        vm.retrySync { ok, message -> result.complete(ok to message) }

        val (ok, message) = withTimeout(5_000) { result.await() }
        assertFalse(ok)
        assertTrue(message.contains("Could not refresh cribs"))
        assertEquals(message, vm.syncMessage.value)
        assertFalse(vm.syncing.value)
        assertEquals(2, dao.cribs.value.size)
    }

    @Test
    fun `repeated sync tap while downloading starts only one download`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        server.gate = gate
        val vm = viewModel()
        val result = CompletableDeferred<Pair<Boolean, String>>()
        vm.retrySync { ok, message -> result.complete(ok to message) }
        assertTrue(vm.syncing.value)
        var secondAnswered = false
        vm.retrySync { _, _ -> secondAnswered = true }
        gate.complete(Unit)
        withTimeout(5_000) { result.await() }
        assertFalse(secondAnswered)
        assertEquals(1, server.downloadUrls.size)
        assertFalse(vm.syncing.value)
    }

    @Test
    fun `retry sync while offline reports why`() = runBlocking {
        server.offline = true
        val vm = viewModel()
        vm.open("A01")
        vm.selectCode(1)
        vm.awaitDetail { it?.draft?.code == 1 }
        vm.saveNow()
        val result = CompletableDeferred<Pair<Boolean, String>>()

        vm.retrySync { ok, message -> result.complete(ok to message) }

        val (ok, message) = withTimeout(5_000) { result.await() }
        assertFalse(ok)
        assertTrue(message.isNotBlank())
    }
}
