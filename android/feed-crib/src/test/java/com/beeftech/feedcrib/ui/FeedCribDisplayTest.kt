package com.beeftech.feedcrib.ui

import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedSlots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedCribDisplayTest {

    private fun entry(date: String, slot: String, code: Int?) =
        FeedCribEntryEntity(cribNumber = "A06", readingDate = date, slot = slot, code = code, adi = 11.0, capturedAt = 0)

    @Test
    fun `goes-to text names the block and its cut-off`() {
        assertEquals("Goes to: Morning (before 11h00)", goesToText(FeedSlots.MORNING))
        assertEquals("Goes to: Mid-Day (before 14h00)", goesToText(FeedSlots.MIDDAY))
        assertEquals("Goes to: Evening (from 14h00)", goesToText(FeedSlots.EVENING))
    }

    @Test
    fun `grid cell finds the code for a date and block`() {
        val slots = listOf(entry("2026-10-10", FeedSlots.MIDDAY, 3), entry("2026-10-09", FeedSlots.MORNING, 2))

        assertEquals(3, gridCode(slots, "2026-10-10", FeedSlots.MIDDAY))
        assertEquals(2, gridCode(slots, "2026-10-09", FeedSlots.MORNING))
        assertNull(gridCode(slots, "2026-10-10", FeedSlots.MORNING))
    }

    @Test
    fun `an ADI-only reading leaves the cell empty`() {
        assertNull(gridCode(listOf(entry("2026-10-10", FeedSlots.MORNING, null)), "2026-10-10", FeedSlots.MORNING))
    }

    @Test
    fun `kg is two decimals with a dot`() {
        assertEquals("11.06 kg", formatKg(11.06))
        assertEquals("10.00 kg", formatKg(10.0))
        assertEquals("—", formatKg(null))
    }

    @Test
    fun `short date reads day and month`() {
        assertEquals("09 Oct", shortDate("2026-10-09"))
        assertEquals("junk", shortDate("junk"))
    }
}
