package com.beeftech.feedcrib.ui

import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedSlots
import com.beeftech.feedcrib.data.SlotAllocator
import java.util.Locale

/** The columns of the readings grid, left to right. */
val GRID_SLOTS = listOf(FeedSlots.MORNING, FeedSlots.MIDDAY, FeedSlots.EVENING)

/** M / D / E. */
fun slotInitial(slot: String): String =
    when (slot) {
        FeedSlots.MORNING -> "M"
        FeedSlots.MIDDAY -> "D"
        else -> "E"
    }

/** "Goes to: Mid-Day (before 14h00)" — tells the worker where a reading saved now will land. */
fun goesToText(slot: String): String {
    val rule = when (slot) {
        FeedSlots.MORNING -> "before 11h00"
        FeedSlots.MIDDAY -> "before 14h00"
        else -> "from 14h00"
    }
    return "Goes to: ${SlotAllocator.label(slot)} ($rule)"
}

/** The dates of the grid rows, newest (today) first. */
fun gridDates(currentDate: String, epochMillis: Long): List<String> =
    listOf(
        currentDate,
        SlotAllocator.dateDaysBefore(epochMillis, 1),
        SlotAllocator.dateDaysBefore(epochMillis, 2)
    )

/** The code shown in one grid cell, or null when there is no reading (or it was ADI only). */
fun gridCode(slots: List<FeedCribEntryEntity>, date: String, slot: String): Int? =
    slots.firstOrNull { it.readingDate == date && it.slot == slot }?.code

/** "11.06 kg", always two decimals and a dot, whatever the phone's locale. */
fun formatKg(value: Double?): String =
    if (value == null) "—" else String.format(Locale.US, "%.2f kg", value)

/** "dd MMM"-style label for a yyyy-MM-dd date; the raw text if it is not one. */
fun shortDate(date: String): String {
    val parts = date.split("-")
    if (parts.size != 3) return date
    val month = MONTHS.getOrNull((parts[1].toIntOrNull() ?: 0) - 1) ?: return date
    return "${parts[2]} $month"
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
