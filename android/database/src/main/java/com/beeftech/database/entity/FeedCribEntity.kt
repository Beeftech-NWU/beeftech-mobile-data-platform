package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

/**
 * A UUID identifies a physical crib. Display codes are unique within a site.
 * Pending creations survive reference downloads. Legacy flattened pen fields are retained for
 * compatibility; newly registered cribs obtain their pen details from cached references.
 */
@Entity(tableName = "feed_cribs", indices = [Index(value = ["site_id", "crib_number"], unique = true), Index(value = ["rfid"], unique = true)])
data class FeedCribEntity(
    @ColumnInfo(name = "crib_number")
    val cribNumber: String,

    @ColumnInfo(name = "site_id")
    val siteId: String,

    @ColumnInfo(name = "pen_description", defaultValue = "''")
    val penDescription: String = "",

    @ColumnInfo(defaultValue = "''")
    val ration: String = "",

    @ColumnInfo(defaultValue = "''")
    val method: String = "",

    @ColumnInfo(defaultValue = "''")
    val description: String = "",

    @ColumnInfo(name = "required_kg")
    val requiredKg: Double? = null,

    @ColumnInfo(name = "animals_begin", defaultValue = "0")
    val animalsBegin: Int? = 0,

    @ColumnInfo(name = "animals_in", defaultValue = "0")
    val animalsIn: Int? = 0,

    @ColumnInfo(name = "animals_out", defaultValue = "0")
    val animalsOut: Int? = 0,

    @ColumnInfo(name = "animals_close", defaultValue = "0")
    val animalsClose: Int? = 0,

    /** The newest entry's ADI, kept in step with local saves and with the server's value. */
    @ColumnInfo(name = "current_adi")
    val currentAdi: Double? = null,

    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0,

    @ColumnInfo(name = "last_downloaded_at", defaultValue = "0")
    val lastDownloadedAt: Long = 0,

    @PrimaryKey
    @ColumnInfo(name = "crib_uuid")
    val cribUuid: String = UUID.nameUUIDFromBytes(("$siteId|$cribNumber").toByteArray()).toString(),
    @ColumnInfo(name = "pen_id") val penId: String? = null,
    @ColumnInfo(name = "ration_id") val rationId: String? = null,
    @ColumnInfo(name = "initial_adi") val initialAdi: Double? = null,
    val rfid: String? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long? = null,
    @ColumnInfo(name = "creation_device_id") val creationDeviceId: String? = null,
    @ColumnInfo(name = "creation_gps_lat") val creationGpsLat: Double? = null,
    @ColumnInfo(name = "creation_gps_lng") val creationGpsLng: Double? = null,
    @ColumnInfo(name = "sync_status", defaultValue = "'SYNCED'") val syncStatus: String = "SYNCED",
    @ColumnInfo(name = "sync_error") val syncError: String? = null
)
