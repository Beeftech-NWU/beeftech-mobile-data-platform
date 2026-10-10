package com.beeftech.feedcrib.data

import com.beeftech.database.entity.FeedSlots
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The clock rules for filing a reading, and the ADI step. Pure, so the boundaries are easy to test. */
object SlotAllocator {

    /** One key press on the ADI stepper. */
    const val ADI_STEP = 0.1

    private const val MIDDAY_FROM_HOUR = 11
    private const val EVENING_FROM_HOUR = 14

    /** Before 11h00 is Morning, before 14h00 Mid-Day, and everything from 14h00 on (late evening too) is Evening. */
    fun slotFor(hour: Int): String =
        when {
            hour < MIDDAY_FROM_HOUR -> FeedSlots.MORNING
            hour < EVENING_FROM_HOUR -> FeedSlots.MIDDAY
            else -> FeedSlots.EVENING
        }

    fun slotFor(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        slotFor(calendarAt(epochMillis, timeZone).get(Calendar.HOUR_OF_DAY))

    /** The local calendar date as yyyy-MM-dd, the way the server stores it. */
    fun dateFor(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        format(calendarAt(epochMillis, timeZone), timeZone)

    /** The local date [days] days before [epochMillis]'s date. */
    fun dateDaysBefore(epochMillis: Long, days: Int, timeZone: TimeZone = TimeZone.getDefault()): String {
        val calendar = calendarAt(epochMillis, timeZone)
        calendar.add(Calendar.DAY_OF_MONTH, -days)
        return format(calendar, timeZone)
    }

    fun label(slot: String): String =
        when (slot) {
            FeedSlots.MORNING -> "Morning"
            FeedSlots.MIDDAY -> "Mid-Day"
            FeedSlots.EVENING -> "Evening"
            else -> slot
        }

    /** Moves [current] by [steps] presses of 0.1 kg, rounded to 2 decimals, and never below zero. */
    fun adjustAdi(current: Double, steps: Int): Double {
        val moved = BigDecimal.valueOf(current)
            .add(BigDecimal.valueOf(ADI_STEP).multiply(BigDecimal(steps)))
            .setScale(2, RoundingMode.HALF_UP)
        return moved.max(BigDecimal.ZERO).toDouble()
    }

    private fun calendarAt(epochMillis: Long, timeZone: TimeZone): Calendar =
        Calendar.getInstance(timeZone, Locale.US).apply { timeInMillis = epochMillis }

    private fun format(calendar: Calendar, timeZone: TimeZone): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }.format(calendar.time)
}
