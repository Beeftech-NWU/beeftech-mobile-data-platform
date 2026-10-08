package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One sync attempt for one module, kept for 30 days so the user can see what ran, when, and how it went.
 * It holds counts and a short message only; no record data.
 */
@Entity(
    tableName = "sync_runs",
    indices = [Index(value = ["user_id", "started_at"])]
)
data class SyncRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: String,

    /** Which module synced, e.g. CALF or MORTALITY. See [SyncRunModule]. */
    val module: String,

    /** What started it: MORNING, EVENING, MANUAL or AUTO. See [SyncRunTrigger]. */
    val trigger: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "finished_at")
    val finishedAt: Long,

    @ColumnInfo(name = "synced_count")
    val syncedCount: Int,

    @ColumnInfo(name = "failed_count")
    val failedCount: Int,

    /** SUCCESS, PARTIAL, FAILED or OFFLINE. See [SyncRunResult]. */
    val result: String,

    val message: String? = null,

    /** The upload's batch name, when the module sent one. */
    @ColumnInfo(name = "batch_name")
    val batchName: String? = null
)

object SyncRunModule {
    const val CALF = "CALF"
    const val FARMER = "FARMER"
    const val TREATMENT = "TREATMENT"
    const val MOVEMENT = "MOVEMENT"
    const val MORTALITY = "MORTALITY"
    const val COST = "COST"
    const val TRACEABILITY = "TRACEABILITY"
}

object SyncRunTrigger {
    const val MORNING = "MORNING"
    const val EVENING = "EVENING"
    const val MANUAL = "MANUAL"
    const val AUTO = "AUTO"
}

object SyncRunResult {
    const val SUCCESS = "SUCCESS"
    const val PARTIAL = "PARTIAL"
    const val FAILED = "FAILED"
    const val OFFLINE = "OFFLINE"
}
