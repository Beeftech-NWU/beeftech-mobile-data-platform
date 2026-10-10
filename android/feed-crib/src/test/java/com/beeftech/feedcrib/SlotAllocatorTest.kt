package com.beeftech.feedcrib

import com.beeftech.database.entity.FeedSlots
import com.beeftech.feedcrib.data.SlotAllocator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class SlotAllocatorTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val johannesburg = TimeZone.getTimeZone("Africa/Johannesburg")

    /** 2026-10-10 at the given time, in UTC. */
    private fun at(hour: Int, minute: Int): Long =
        java.util.Calendar.getInstance(utc).apply {
            clear()
            set(2026, java.util.Calendar.OCTOBER, 10, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `the clock files a reading into the right block at every boundary`() {
        val expected = listOf(
            Triple(0, 0, FeedSlots.MORNING),
            Triple(10, 59, FeedSlots.MORNING),
            Triple(11, 0, FeedSlots.MIDDAY),
            Triple(13, 59, FeedSlots.MIDDAY),
            Triple(14, 0, FeedSlots.EVENING),
            Triple(19, 59, FeedSlots.EVENING),
            Triple(20, 0, FeedSlots.EVENING),
            Triple(23, 30, FeedSlots.EVENING)
        )

        expected.forEach { (hour, minute, slot) ->
            assertEquals("$hour:$minute", slot, SlotAllocator.slotFor(at(hour, minute), utc))
        }
    }

    @Test
    fun `the block follows the device's own time zone`() {
        /* 09:30 UTC is 11:30 in Johannesburg. */
        assertEquals(FeedSlots.MORNING, SlotAllocator.slotFor(at(9, 30), utc))
        assertEquals(FeedSlots.MIDDAY, SlotAllocator.slotFor(at(9, 30), johannesburg))
    }

    @Test
    fun `the date is the device's local date`() {
        /* 23:30 UTC on the 10th is already the 11th in Johannesburg. */
        assertEquals("2026-10-10", SlotAllocator.dateFor(at(23, 30), utc))
        assertEquals("2026-10-11", SlotAllocator.dateFor(at(23, 30), johannesburg))
    }

    @Test
    fun `days before steps back across a month`() {
        assertEquals("2026-10-08", SlotAllocator.dateDaysBefore(at(12, 0), 2, utc))
        assertEquals("2026-09-30", SlotAllocator.dateDaysBefore(at(12, 0), 10, utc))
    }

    @Test
    fun `labels read well`() {
        assertEquals("Morning", SlotAllocator.label(FeedSlots.MORNING))
        assertEquals("Mid-Day", SlotAllocator.label(FeedSlots.MIDDAY))
        assertEquals("Evening", SlotAllocator.label(FeedSlots.EVENING))
    }

    @Test
    fun `ADI moves in steps of 0 point 1 kg`() {
        assertEquals(11.1, SlotAllocator.adjustAdi(11.0, 1), 0.0)
        assertEquals(10.9, SlotAllocator.adjustAdi(11.0, -1), 0.0)
        assertEquals(11.3, SlotAllocator.adjustAdi(11.0, 3), 0.0)
    }

    @Test
    fun `ADI does not drift when the same step is pressed many times`() {
        var adi = 0.0
        repeat(30) { adi = SlotAllocator.adjustAdi(adi, 1) }

        assertEquals(3.0, adi, 0.0)
    }

    @Test
    fun `ADI is rounded to 2 decimals`() {
        assertEquals(11.16, SlotAllocator.adjustAdi(11.06, 1), 0.0)
        assertEquals(11.18, SlotAllocator.adjustAdi(11.075, 1), 0.0)
    }

    @Test
    fun `ADI never goes below zero`() {
        assertEquals(0.0, SlotAllocator.adjustAdi(0.0, -1), 0.0)
        assertEquals(0.0, SlotAllocator.adjustAdi(0.05, -1), 0.0)
        assertEquals(0.1, SlotAllocator.adjustAdi(0.0, 1), 0.0)
    }
}
