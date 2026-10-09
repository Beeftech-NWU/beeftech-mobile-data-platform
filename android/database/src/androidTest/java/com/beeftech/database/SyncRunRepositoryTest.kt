package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunResult
import com.beeftech.database.entity.SyncRunTrigger
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.repository.SyncRunRepository
import com.beeftech.database.security.CurrentUserIdRegistry
import com.beeftech.database.security.SyncIdentityRegistry
import com.beeftech.database.util.BatchNaming
import com.beeftech.database.util.ProjectCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SyncRunRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: BeefTechDatabase
    private lateinit var repository: SyncRunRepository
    private var clock = 10_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CurrentUserIdRegistry.setCurrentUserId(USER)
        context.deleteDatabase(DATABASE_NAME)

        val result = DatabaseFactory.create(context, ByteArray(32) { (it + 1).toByte() })
        assertTrue(result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database

        repository = SyncRunRepository(
            syncRunDao = database.syncRunDao(),
            pendingSyncDao = database.pendingSyncDao(),
            now = { clock }
        )
    }

    @After
    fun tearDown() {
        SyncIdentityRegistry.clear()
        BatchNaming.forgetIssued()
        CurrentUserIdRegistry.clear()
        if (::database.isInitialized) database.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun recentRunsComeBackNewestFirstAndOnlyForTheSignedInUser() = runBlocking {
        /* Recent enough to survive the 30 day pruning that every save runs. */
        record(SyncRunModule.COST, started = clock - 3_000)
        record(SyncRunModule.CALF, started = clock - 1_000)
        record(SyncRunModule.MORTALITY, started = clock - 2_000)

        CurrentUserIdRegistry.setCurrentUserId("someone-else")
        record(SyncRunModule.FARMER, started = clock)

        CurrentUserIdRegistry.setCurrentUserId(USER)
        val modules = repository.observeRecent().first().map { it.module }

        assertEquals(listOf("CALF", "MORTALITY", "COST"), modules)
    }

    @Test
    fun runsOlderThanThirtyDaysAreDroppedWhenAnotherRunIsSaved() = runBlocking {
        record(SyncRunModule.COST, started = clock - TimeUnit.DAYS.toMillis(31))
        record(SyncRunModule.CALF, started = clock - TimeUnit.DAYS.toMillis(29))

        record(SyncRunModule.MORTALITY, started = clock)

        val modules = repository.observeRecent().first().map { it.module }
        assertEquals(listOf("MORTALITY", "CALF"), modules)
    }

    @Test
    fun nothingIsSavedWithoutASignedInUser() = runBlocking {
        CurrentUserIdRegistry.clear()
        val signedOut = SyncRunRepository(database.syncRunDao(), database.pendingSyncDao(), userIdProvider = { null })

        signedOut.recordRun(SyncRunModule.COST, SyncRunTrigger.AUTO, 1, 1, 0, SyncRunResult.SUCCESS)

        assertEquals(0, database.syncRunDao().getRecent(USER).size)
    }

    @Test
    fun pendingCountsAreGroupedByModuleType() = runBlocking {
        val pending = PendingSyncRepository(database.pendingSyncDao())
        pending.queueOperation("ANIMAL_COST", "c-1", "INSERT", "{}")
        pending.queueOperation("ANIMAL_COST", "c-2", "INSERT", "{}")
        pending.queueOperation("MORTALITY", "m-1", "INSERT", "{}")

        val counts = repository.observePendingByType().first()

        assertEquals(mapOf("ANIMAL_COST" to 2, "MORTALITY" to 1), counts)
    }

    @Test
    fun aRunKeepsTheBatchNameItsUploadUsed() = runBlocking {
        SyncIdentityRegistry.set("BF01", "MOB_DEV_1")
        val started = clock - 1_000
        val name = BatchNaming.nameFor(ProjectCode.COST, nowMillis = clock - 500)

        record(SyncRunModule.COST, started = started)
        record(SyncRunModule.CALF, started = started)

        val byModule = repository.observeRecent().first().associate { it.module to it.batchName }
        assertEquals(name, byModule["COST"])
        assertEquals(null, byModule["CALF"])
    }

    private suspend fun record(module: String, started: Long) =
        repository.recordRun(module, SyncRunTrigger.MANUAL, started, 1, 0, SyncRunResult.SUCCESS)

    private companion object {
        const val DATABASE_NAME = "beeftech.db"
        const val USER = "user-1"
    }
}
