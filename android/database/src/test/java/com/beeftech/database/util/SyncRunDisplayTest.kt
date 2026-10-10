package com.beeftech.database.util

import com.beeftech.database.entity.SyncRunEntity
import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunResult
import com.beeftech.database.entity.SyncRunTrigger
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class SyncRunDisplayTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // 2026-10-08T14:05:09Z
    private val now = 1_791_468_309_000L

    private fun run(module: String = SyncRunModule.CALF, result: String = SyncRunResult.SUCCESS, finishedAt: Long = now) =
        SyncRunEntity(
            userId = "u", module = module, trigger = SyncRunTrigger.MANUAL, startedAt = finishedAt - 1000,
            finishedAt = finishedAt, syncedCount = 3, failedCount = 1, result = result
        )

    @Test
    fun `waiting records are grouped by module in a fixed order and empty modules are left out`() {
        val counts = mapOf(
            "ANIMAL_COST" to 2,
            "CALF_REGISTRATION" to 4,
            "MORTALITY" to 0,
            "ANIMAL_PURCHASE" to 1,
            "LOCATION_FEED" to 2,
            "FEED_CRIB_ENTRY" to 6,
            "SOMETHING_NEW" to 5
        )

        assertEquals(
            listOf(
                ModulePending("Calves", 4),
                ModulePending("Costs", 2),
                ModulePending("Traceability", 3),
                ModulePending("Feed", 6),
                ModulePending("Other", 5)
            ),
            SyncRunDisplay.pendingByModule(counts)
        )
    }

    @Test
    fun `nothing waiting gives an empty list`() {
        assertEquals(emptyList<ModulePending>(), SyncRunDisplay.pendingByModule(emptyMap()))
        assertEquals(emptyList<ModulePending>(), SyncRunDisplay.pendingByModule(mapOf("TREATMENT" to 0)))
    }

    @Test
    fun `a run today shows only the time and an older one shows the date`() {
        assertEquals("14:05", SyncRunDisplay.timeLabel(now, now, utc))
        assertEquals("7 Oct 14:05", SyncRunDisplay.timeLabel(now - 86_400_000L, now, utc))
    }

    @Test
    fun `the last run line names the module and the result`() {
        assertEquals("Last sync 14:05 · Calves · All sent", SyncRunDisplay.lastRunLine(run(), now, utc))
        assertEquals(
            "Last sync 14:05 · Traceability · Offline",
            SyncRunDisplay.lastRunLine(run(SyncRunModule.TRACEABILITY, SyncRunResult.OFFLINE), now, utc)
        )
        assertEquals("No sync has run yet", SyncRunDisplay.lastRunLine(null, now, utc))
    }

    @Test
    fun `the counts line mentions what is still waiting`() {
        assertEquals("3 sent, 1 waiting", SyncRunDisplay.countsLine(run()))
        assertEquals("3 sent", SyncRunDisplay.countsLine(run().copy(failedCount = 0)))
    }

    @Test
    fun `triggers and results have plain words`() {
        assertEquals("Sync now", SyncRunDisplay.triggerLabel(SyncRunTrigger.MANUAL))
        assertEquals("Morning sync", SyncRunDisplay.triggerLabel(SyncRunTrigger.MORNING))
        assertEquals("Automatic", SyncRunDisplay.triggerLabel(SyncRunTrigger.AUTO))
        assertEquals("Partly sent", SyncRunDisplay.resultLabel(SyncRunResult.PARTIAL))
    }
}
