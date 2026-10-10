package com.beeftech.backend.api.feedcrib

import org.jetbrains.exposed.sql.Table

/* New tables, so SchemaUtils.create makes them on existing databases; no ALTER migration needed. */

/** Crib master data, one row per crib per site. A real Beeftech parameter-file import fills it later. */
object FeedCribsTable : Table("feed_cribs") {
    val id = integer("id").autoIncrement()

    val cribNumber = varchar("crib_number", 32)
    val siteId = varchar("site_id", 64)
    val penDescription = varchar("pen_description", 255).default("")
    val ration = varchar("ration", 255).default("")
    val method = varchar("method", 255).default("")
    val description = varchar("description", 512).default("")
    val requiredKg = double("required_kg").nullable()
    val animalsBegin = integer("animals_begin").default(0)
    val animalsIn = integer("animals_in").default(0)
    val animalsOut = integer("animals_out").default(0)
    val animalsClose = integer("animals_close").default(0)

    /* Set from the newest synced entry for the crib. */
    val currentAdi = double("current_adi").nullable()
    val active = bool("active").default(true)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("feed_cribs_site_crib", siteId, cribNumber)
    }
}

/** The bunk score a worker picks for a reading. Seeded, 0-5. */
object CribReadingCodesTable : Table("crib_reading_codes") {
    val code = integer("code")
    val label = varchar("label", 64)
    val description = varchar("description", 255).default("")
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(code)
}

/**
 * One saved reading. Append-only: a later entry for the same crib, date and slot overrides an
 * earlier one when displayed (the latest captured_at wins), but the earlier row is kept.
 */
object FeedCribEntriesTable : Table("feed_crib_entries") {
    val id = integer("id").autoIncrement()

    val recordguid = varchar("recordguid", 255).uniqueIndex()
    val cribNumber = varchar("crib_number", 32)
    val siteId = varchar("site_id", 64).nullable()

    /* The device's local date (yyyy-MM-dd) and the block the app filed the reading under. */
    val readingDate = varchar("reading_date", 10)
    val slot = varchar("slot", 16)

    /* Null for an ADI-only change. */
    val code = integer("code").nullable()
    val adi = double("adi")

    val capturedAt = long("captured_at")
    val deviceId = varchar("device_id", 255)
    val gpsLat = double("gps_lat").nullable()
    val gpsLng = double("gps_lng").nullable()

    val submittedByUserId = varchar("submitted_by_user_id", 64).nullable()
    val syncStatus = varchar("sync_status", 32).default("SYNCED")
    val syncedAt = long("synced_at").nullable()

    /* A voided record is kept but hidden from lists and counts. See VoidRepository. */
    val voidedAt = long("voided_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index("feed_crib_entries_site_date", false, siteId, readingDate)
        index("feed_crib_entries_crib", false, siteId, cribNumber, capturedAt)
    }
}
