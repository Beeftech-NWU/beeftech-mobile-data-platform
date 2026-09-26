package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * D1: renames the tag-holding animal_id column to tag_number and adds the
 * animal UUID columns. Runs once; a no-op on a fresh database or when
 * tag_number already exists.
 *
 * The new table is created by Exposed from [CalfRegistrationTable] so the
 * DDL and index names always match the schema definition.
 */
object CalfRegistrationSchemaMigration {

    fun run(database: Database) = transaction(database) {
        val columns = exec("PRAGMA table_info(calf_registrations)") { rs ->
            buildList { while (rs.next()) add(rs.getString("name")) }
        } ?: emptyList()

        if (columns.isEmpty() || "tag_number" in columns) return@transaction

        // Index-free copy, so the legacy index names cannot clash with the new table's.
        exec("CREATE TABLE calf_registrations_legacy AS SELECT * FROM calf_registrations")
        exec("DROP TABLE calf_registrations")

        SchemaUtils.create(CalfRegistrationTable)

        exec(
            """
            INSERT INTO calf_registrations
              (id, tag_number, animal_uuid, birthdate, breed, dam_tag_number, sire_tag_number,
               dam_animal_uuid, sire_animal_uuid, photo_path, video_path, gps_lat, gps_lng,
               capture_at, device_id, recordguid, sync_status, synced_at)
            SELECT id, animal_id, NULL, birthdate, breed, dam_id, sire_id,
                   NULL, NULL, photo_path, video_path, gps_lat, gps_lng,
                   capture_at, device_id, recordguid, sync_status, synced_at
            FROM calf_registrations_legacy
            """.trimIndent()
        )
        exec("DROP TABLE calf_registrations_legacy")
    }
}
