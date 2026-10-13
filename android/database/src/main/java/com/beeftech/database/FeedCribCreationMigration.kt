package com.beeftech.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

object FeedCribCreationMigration {
    private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            var found = false
            while (!found && cursor.moveToNext()) found = cursor.getString(nameIndex) == column
            found
        }

    val MIGRATION_47_48 = object : Migration(47, 48) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS feed_cribs_new")
            db.execSQL("CREATE TABLE feed_cribs_new (`crib_number` TEXT NOT NULL, `site_id` TEXT NOT NULL, `pen_description` TEXT NOT NULL DEFAULT '', `ration` TEXT NOT NULL DEFAULT '', `method` TEXT NOT NULL DEFAULT '', `description` TEXT NOT NULL DEFAULT '', `required_kg` REAL, `animals_begin` INTEGER DEFAULT 0, `animals_in` INTEGER DEFAULT 0, `animals_out` INTEGER DEFAULT 0, `animals_close` INTEGER DEFAULT 0, `current_adi` REAL, `active` INTEGER NOT NULL DEFAULT 1, `updated_at` INTEGER NOT NULL DEFAULT 0, `last_downloaded_at` INTEGER NOT NULL DEFAULT 0, `crib_uuid` TEXT NOT NULL PRIMARY KEY, `pen_id` TEXT, `ration_id` TEXT, `initial_adi` REAL, `rfid` TEXT, `created_by` TEXT, `created_at` INTEGER, `creation_device_id` TEXT, `creation_gps_lat` REAL, `creation_gps_lng` REAL, `sync_status` TEXT NOT NULL DEFAULT 'SYNCED', `sync_error` TEXT)")
            val legacy = mutableListOf<Triple<String, String, String>>()
            db.query("SELECT site_id, crib_number FROM feed_cribs").use { cursor ->
                while (cursor.moveToNext()) {
                    val site = cursor.getString(0)
                    val code = cursor.getString(1)
                    legacy += Triple(site, code, UUID.nameUUIDFromBytes(("$site|$code").toByteArray()).toString())
                }
            }
            legacy.forEach { (site, code, uuid) ->
                db.execSQL("INSERT INTO feed_cribs_new (crib_number, site_id, pen_description, ration, method, description, required_kg, animals_begin, animals_in, animals_out, animals_close, current_adi, active, updated_at, last_downloaded_at, crib_uuid) SELECT crib_number, site_id, pen_description, ration, method, description, required_kg, animals_begin, animals_in, animals_out, animals_close, current_adi, active, updated_at, last_downloaded_at, ? FROM feed_cribs WHERE site_id = ? AND crib_number = ?", arrayOf(uuid, site, code))
            }
            db.execSQL("DROP TABLE feed_cribs")
            db.execSQL("ALTER TABLE feed_cribs_new RENAME TO feed_cribs")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_feed_cribs_site_id_crib_number ON feed_cribs(site_id, crib_number)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_feed_cribs_rfid ON feed_cribs(rfid)")
            if (!hasColumn(db, "feed_crib_entries", "crib_uuid")) {
                db.execSQL("ALTER TABLE feed_crib_entries ADD COLUMN crib_uuid TEXT")
            }
            db.execSQL("UPDATE feed_crib_entries SET crib_uuid = (SELECT crib_uuid FROM feed_cribs c WHERE c.crib_number = feed_crib_entries.crib_number AND (feed_crib_entries.site_id IS NULL OR c.site_id = feed_crib_entries.site_id))")
            db.execSQL("CREATE TABLE IF NOT EXISTS feed_crib_setup (userId TEXT NOT NULL PRIMARY KEY, json TEXT NOT NULL)")
        }
    }
}
