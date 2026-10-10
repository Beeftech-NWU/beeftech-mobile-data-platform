package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One crib on the signed-in user's site, downloaded from the server. The whole table is replaced
 * on every download, so it only ever holds one site's cribs.
 */
@Entity(tableName = "feed_cribs")
data class FeedCribEntity(
    @PrimaryKey
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
    val animalsBegin: Int = 0,

    @ColumnInfo(name = "animals_in", defaultValue = "0")
    val animalsIn: Int = 0,

    @ColumnInfo(name = "animals_out", defaultValue = "0")
    val animalsOut: Int = 0,

    @ColumnInfo(name = "animals_close", defaultValue = "0")
    val animalsClose: Int = 0,

    /** The newest entry's ADI, kept in step with local saves and with the server's value. */
    @ColumnInfo(name = "current_adi")
    val currentAdi: Double? = null,

    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0,

    @ColumnInfo(name = "last_downloaded_at", defaultValue = "0")
    val lastDownloadedAt: Long = 0
)
