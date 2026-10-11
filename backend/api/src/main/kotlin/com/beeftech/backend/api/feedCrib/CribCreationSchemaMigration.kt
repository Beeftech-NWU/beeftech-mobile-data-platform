package com.beeftech.backend.api.feedcrib

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.UUID

object CribCreationSchemaMigration {
    fun run(database: Database) = transaction(database) {
        val columns = exec("PRAGMA table_info(feed_cribs)") { rs ->
            buildSet { while (rs.next()) add(rs.getString("name")) }
        }.orEmpty()
        if (columns.isNotEmpty()) {
            mapOf(
                "crib_uuid" to "VARCHAR(64)", "pen_id" to "VARCHAR(64)", "ration_id" to "VARCHAR(64)",
                "initial_adi" to "DOUBLE", "rfid" to "VARCHAR(255)", "created_by" to "VARCHAR(64)",
                "created_at" to "BIGINT", "creation_device_id" to "VARCHAR(255)",
                "creation_gps_lat" to "DOUBLE", "creation_gps_lng" to "DOUBLE"
            ).filterKeys { it !in columns }.forEach { (name, type) ->
                exec("ALTER TABLE feed_cribs ADD COLUMN $name $type")
            }
            val legacy = exec("SELECT id, site_id, crib_number FROM feed_cribs WHERE crib_uuid IS NULL") { rs ->
                buildList {
                    while (rs.next()) add(rs.getInt("id") to
                        UUID.nameUUIDFromBytes((rs.getString("site_id") + "|" + rs.getString("crib_number")).toByteArray()).toString())
                }
            }.orEmpty()
            legacy.forEach { (id, uuid) -> exec("UPDATE feed_cribs SET crib_uuid = '$uuid' WHERE id = $id") }
            /* Existing data may already hold codes that differ only by case or spaces; never stop start-up over it. */
            val duplicates = exec("SELECT site_id, UPPER(TRIM(crib_number)) AS code FROM feed_cribs GROUP BY site_id, UPPER(TRIM(crib_number)) HAVING COUNT(*) > 1") { rs ->
                buildList { while (rs.next()) add(rs.getString("site_id") + "/" + rs.getString("code")) }
            }.orEmpty()
            if (duplicates.isEmpty()) {
                exec("CREATE UNIQUE INDEX IF NOT EXISTS feed_cribs_site_code_normalized ON feed_cribs(site_id, UPPER(TRIM(crib_number)))")
            } else {
                LoggerFactory.getLogger(CribCreationSchemaMigration::class.java).warn(
                    "Skipping unique crib code index: duplicate codes (ignoring case and spaces) exist on a site: {}", duplicates)
            }
        }
        val entries = exec("PRAGMA table_info(feed_crib_entries)") { rs ->
            buildSet { while (rs.next()) add(rs.getString("name")) }
        }.orEmpty()
        if (entries.isNotEmpty() && "crib_uuid" !in entries) {
            exec("ALTER TABLE feed_crib_entries ADD COLUMN crib_uuid VARCHAR(64)")
            exec("UPDATE feed_crib_entries SET crib_uuid = (SELECT crib_uuid FROM feed_cribs c WHERE c.site_id = feed_crib_entries.site_id AND c.crib_number = feed_crib_entries.crib_number)")
        }
    }
}
