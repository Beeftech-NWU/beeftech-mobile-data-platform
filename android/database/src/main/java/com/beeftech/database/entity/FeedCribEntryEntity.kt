package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One saved crib reading. Append-only: a later code in the same crib, date and slot overrides an
 * earlier one when displayed (the newest captured_at wins) but the earlier row is kept.
 *
 * Also holds history downloaded from the server (other phones' readings): those rows have
 * origin = SERVER and sync_status = SYNCED, and are never uploaded.
 */
@Entity(
    tableName = "feed_crib_entries",
    indices = [
        Index(value = ["crib_number", "reading_date"]),
        Index(value = ["sync_status"]),
        Index(value = ["reading_date", "user_id"])
    ]
)
data class FeedCribEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "crib_number")
    val cribNumber: String,

    @ColumnInfo(name = "site_id")
    val siteId: String? = null,

    /** The device's local date, yyyy-MM-dd. */
    @ColumnInfo(name = "reading_date")
    val readingDate: String,

    /** MORNING, MIDDAY or EVENING. See [FeedSlots]. */
    val slot: String,

    /** Null for an ADI-only change. */
    val code: Int? = null,

    val adi: Double,

    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,

    @ColumnInfo(name = "device_id", defaultValue = "''")
    val deviceId: String = "",

    @ColumnInfo(name = "gps_lat")
    val gpsLat: Double? = null,

    @ColumnInfo(name = "gps_lng")
    val gpsLng: Double? = null,

    /** Who took the reading: the signed-in user for LOCAL rows, the server's submitter for SERVER rows. */
    @ColumnInfo(name = "user_id", defaultValue = "''")
    val userId: String = "",

    /** LOCAL or SERVER. See [FeedEntryOrigin]. */
    @ColumnInfo(defaultValue = "'LOCAL'")
    val origin: String = FeedEntryOrigin.LOCAL,

    @ColumnInfo(name = "sync_status", defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,

    /** Last message the server gave when it rejected this record, shown to the user. */
    @ColumnInfo(name = "sync_error")
    val syncError: String? = null,

    /** Times the server explicitly rejected this record; reaching the cap makes it REJECTED. */
    @ColumnInfo(name = "sync_attempts", defaultValue = "0")
    val syncAttempts: Int = 0
)

/** The three blocks of a feeding day. The same words the backend validates. */
object FeedSlots {
    const val MORNING = "MORNING"
    const val MIDDAY = "MIDDAY"
    const val EVENING = "EVENING"

    val ALL = listOf(MORNING, MIDDAY, EVENING)
}

object FeedEntryOrigin {
    const val LOCAL = "LOCAL"
    const val SERVER = "SERVER"
}
