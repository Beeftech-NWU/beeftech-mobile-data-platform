package com.beeftech.database.repository

import com.beeftech.database.entity.SyncRunResult
import com.beeftech.database.entity.SyncRunTrigger

/**
 * How a sync run is judged and whether it is worth a history row. Kept free of Room so it can be
 * tested on the JVM.
 *
 * A run is measured by the queue, not by each module's own result type: [before] records waiting
 * when it started, [after] when it ended.
 */
data class SyncRunSummary(
    val syncedCount: Int,
    val failedCount: Int,
    val result: String
) {
    companion object {

        /** Key of the WorkManager input data that carries the [SyncRunTrigger]. */
        const val TRIGGER_INPUT_KEY = "sync_trigger"

        fun of(before: Int, after: Int, error: String? = null): SyncRunSummary {
            val synced = (before - after).coerceAtLeast(0)
            val result = when {
                after == 0 && error == null -> SyncRunResult.SUCCESS
                synced > 0 -> SyncRunResult.PARTIAL
                else -> SyncRunResult.FAILED
            }
            return SyncRunSummary(synced, after, result)
        }

        /**
         * A scheduled or automatic run that found nothing to do is not worth a row, or the history
         * fills with empty runs. A manual run always is, so "Sync now" shows what happened.
         */
        fun worthRecording(trigger: String, before: Int, after: Int, error: String?): Boolean =
            trigger == SyncRunTrigger.MANUAL || before > 0 || after > 0 || error != null

        /** A tapped Sync now with no session or connection; automatic retries stay out of the history. */
        fun worthRecordingOffline(trigger: String, firstAttempt: Boolean): Boolean =
            trigger != SyncRunTrigger.AUTO && firstAttempt
    }
}
