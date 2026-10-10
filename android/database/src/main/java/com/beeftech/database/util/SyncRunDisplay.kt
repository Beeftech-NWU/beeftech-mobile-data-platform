package com.beeftech.database.util

import com.beeftech.database.entity.SyncRunEntity
import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunResult
import com.beeftech.database.entity.SyncRunTrigger
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Waiting records of one module, for the Home sync widget. */
data class ModulePending(val label: String, val count: Int)

/** Words for the sync history, shared by the Home widget and My activity. */
object SyncRunDisplay {

    private val ORDER = listOf("Calves", "Farmers", "Treatments", "Movements", "Mortality", "Costs", "Traceability", "Other")

    private fun moduleOfEntityType(entityType: String): String = when (entityType) {
        "CALF_REGISTRATION" -> "Calves"
        "FARMER_REGISTRATION" -> "Farmers"
        "TREATMENT" -> "Treatments"
        "ANIMAL_MOVEMENT" -> "Movements"
        "MORTALITY" -> "Mortality"
        "ANIMAL_COST" -> "Costs"
        "ANIMAL_PURCHASE", "LOCATION_FEED" -> "Traceability"
        else -> "Other"
    }

    /** Waiting records grouped by module, in a fixed order, leaving out modules with nothing waiting. */
    fun pendingByModule(countsByEntityType: Map<String, Int>): List<ModulePending> =
        countsByEntityType
            .filterValues { it > 0 }
            .entries
            .groupBy({ moduleOfEntityType(it.key) }, { it.value })
            .map { (label, counts) -> ModulePending(label, counts.sum()) }
            .sortedBy { ORDER.indexOf(it.label) }

    fun moduleLabel(module: String): String = when (module) {
        SyncRunModule.CALF -> "Calves"
        SyncRunModule.FARMER -> "Farmers"
        SyncRunModule.TREATMENT -> "Treatments"
        SyncRunModule.MOVEMENT -> "Movements"
        SyncRunModule.MORTALITY -> "Mortality"
        SyncRunModule.COST -> "Costs"
        SyncRunModule.TRACEABILITY -> "Traceability"
        else -> module.lowercase().replaceFirstChar { it.uppercase() }
    }

    fun triggerLabel(trigger: String): String = when (trigger) {
        SyncRunTrigger.MORNING -> "Morning sync"
        SyncRunTrigger.EVENING -> "Evening sync"
        SyncRunTrigger.MANUAL -> "Sync now"
        else -> "Automatic"
    }

    fun resultLabel(result: String): String = when (result) {
        SyncRunResult.SUCCESS -> "All sent"
        SyncRunResult.PARTIAL -> "Partly sent"
        SyncRunResult.FAILED -> "Failed"
        SyncRunResult.OFFLINE -> "Offline"
        else -> result
    }

    /** "14:05" for today, otherwise "8 Oct 14:05". */
    fun timeLabel(
        epochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        fun format(pattern: String) =
            SimpleDateFormat(pattern, Locale.US).apply { this.timeZone = timeZone }

        val sameDay = format("yyyyMMdd").format(epochMillis) == format("yyyyMMdd").format(nowMillis)
        return format(if (sameDay) "HH:mm" else "d MMM HH:mm").format(epochMillis)
    }

    /** "Last sync 14:05 · Calves · All sent", or a prompt when nothing has run yet. */
    fun lastRunLine(
        run: SyncRunEntity?,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String =
        if (run == null) {
            "No sync has run yet"
        } else {
            "Last sync ${timeLabel(run.finishedAt, nowMillis, timeZone)} · " +
                "${moduleLabel(run.module)} · ${resultLabel(run.result)}"
        }

    /** "3 sent, 1 waiting", or just the sent count when nothing is left. */
    fun countsLine(run: SyncRunEntity): String = buildString {
        append("${run.syncedCount} sent")
        if (run.failedCount > 0) append(", ${run.failedCount} waiting")
    }
}
