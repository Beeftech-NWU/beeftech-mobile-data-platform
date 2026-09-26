package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class CalfRegistrationSchemaMigrationTest {

    private fun legacyDatabase(): Database {
        val file = Files.createTempFile("beeftech-migration-test", ".db")
        file.toFile().deleteOnExit()
        val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")

        transaction(database) {
            exec(
                """
                CREATE TABLE calf_registrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    animal_id VARCHAR(255) NOT NULL,
                    birthdate BIGINT NOT NULL,
                    breed VARCHAR(255) NOT NULL,
                    dam_id VARCHAR(255) NULL,
                    sire_id VARCHAR(255) NULL,
                    photo_path VARCHAR(512) NULL,
                    video_path VARCHAR(512) NULL,
                    gps_lat REAL NOT NULL,
                    gps_lng REAL NOT NULL,
                    capture_at BIGINT NOT NULL,
                    device_id VARCHAR(255) NOT NULL,
                    recordguid VARCHAR(255) NOT NULL,
                    sync_status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
                    synced_at BIGINT NULL
                )
                """.trimIndent()
            )
            exec("CREATE UNIQUE INDEX calf_registrations_animal_id_unique ON calf_registrations (animal_id)")
            exec("CREATE UNIQUE INDEX calf_registrations_recordguid_unique ON calf_registrations (recordguid)")
            exec(
                """
                INSERT INTO calf_registrations
                  (animal_id, birthdate, breed, dam_id, sire_id, gps_lat, gps_lng,
                   capture_at, device_id, recordguid, sync_status, synced_at)
                VALUES
                  ('Blu0000064', 1700000000000, 'Brangus', 'Blu0000011', NULL, 0.0, 0.0, 1700000100000, 'dev', 'guid-1', 'SYNCED', 5),
                  ('Blu0000065', 1700000000001, 'Angus', NULL, NULL, 0.0, 0.0, 1700000100001, 'dev', 'guid-2', 'SYNCED', 6)
                """.trimIndent()
            )
        }
        return database
    }

    private fun rows(database: Database): List<Triple<String, String?, String?>> =
        transaction(database) {
            exec("SELECT tag_number, animal_uuid, dam_tag_number FROM calf_registrations ORDER BY id") { rs ->
                buildList { while (rs.next()) add(Triple(rs.getString(1), rs.getString(2), rs.getString(3))) }
            } ?: emptyList()
        }

    @Test
    fun `legacy table is migrated without losing rows`() {
        val database = legacyDatabase()

        CalfRegistrationSchemaMigration.run(database)

        assertEquals(
            listOf(
                Triple("Blu0000064", null, "Blu0000011"),
                Triple("Blu0000065", null, null)
            ),
            rows(database)
        )
    }

    @Test
    fun `running the migration twice is a no-op`() {
        val database = legacyDatabase()

        CalfRegistrationSchemaMigration.run(database)
        CalfRegistrationSchemaMigration.run(database)

        assertEquals(2, rows(database).size)
    }

    @Test
    fun `migrated table accepts new sync writes`() {
        val database = legacyDatabase()

        CalfRegistrationSchemaMigration.run(database)

        transaction(database) {
            exec(
                """
                INSERT INTO calf_registrations
                  (tag_number, animal_uuid, birthdate, breed, gps_lat, gps_lng, capture_at, device_id, recordguid)
                VALUES ('Blu0000099', 'uuid-99', 1, 'Angus', 0.0, 0.0, 1, 'dev', 'guid-99')
                """.trimIndent()
            )
        }

        assertEquals(3, rows(database).size)
    }
}
