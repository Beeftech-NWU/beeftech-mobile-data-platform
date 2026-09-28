package com.beeftech.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * R3, bullet 3: `animal_movements` and `animal_weights` gain audit fields
 * (`gps_lat`, `gps_lng`, `device_id`, `captured_at`, `sync_status`,
 * `synced_at`). `captured_at` is backfilled from `movement_date` on every
 * `animal_movements` row. A row whose `movement_id` matches a
 * `legacy_animal_movements.recordguid` also gets its GPS/device/sync fields
 * restored from that legacy row; a row with no match (or no legacy table at
 * all) keeps the plain defaults.
 */
@RunWith(AndroidJUnit4::class)
class Migration20To21Test {

    private val TEST_DB = "migration-test-20-21"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration20To21_addsAuditFields_backfillsCapturedAt_andRestoresFromLegacy() {
        val animalId = UUID.randomUUID().toString()
        val matchedMovementId = UUID.randomUUID().toString()
        val unmatchedMovementId = UUID.randomUUID().toString()
        val weightId = UUID.randomUUID().toString()

        helper.createDatabase(TEST_DB, 20).use { db ->
            db.execSQL(
                "INSERT INTO `animals` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animalId, 1000L, "Bonsmara", 0.0, 0.0, 1000L, "device1", UUID.randomUUID().toString(), "PENDING")
            )

            // A row whose movement_id matches a legacy_animal_movements.recordguid.
            db.execSQL(
                "INSERT INTO `animal_movements` (`movement_id`, `animal_id`, `destination_farm_id`, `destination_pen_id`, `movement_date`, `record_guid`) VALUES (?, ?, 'FARM-A', 'PEN-1', '5000', ?)",
                arrayOf<Any>(matchedMovementId, animalId, UUID.randomUUID().toString())
            )
            // A row with no legacy match (created after the v10 upgrade).
            db.execSQL(
                "INSERT INTO `animal_movements` (`movement_id`, `animal_id`, `destination_farm_id`, `destination_pen_id`, `movement_date`, `record_guid`) VALUES (?, ?, 'FARM-B', 'PEN-2', '6000', ?)",
                arrayOf<Any>(unmatchedMovementId, animalId, UUID.randomUUID().toString())
            )

            db.execSQL(
                """
                CREATE TABLE `legacy_animal_movements` (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    animalId TEXT NOT NULL,
                    movementType TEXT NOT NULL,
                    responsibleWorker TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL,
                    gpsLat REAL NOT NULL DEFAULT 0.0,
                    gpsLng REAL NOT NULL DEFAULT 0.0,
                    deviceId TEXT NOT NULL DEFAULT '',
                    recordguid TEXT NOT NULL DEFAULT '',
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT INTO `legacy_animal_movements` (`animalId`, `movementType`, `responsibleWorker`, `timestamp`, `gpsLat`, `gpsLng`, `deviceId`, `recordguid`, `syncStatus`, `syncedAt`) VALUES (?, 'Moved to Feedlot A', 'Old Worker', 4000, -26.2, 28.0, 'legacy-device', ?, 'SYNCED', 4500)",
                arrayOf<Any>(animalId, matchedMovementId)
            )

            db.execSQL(
                "INSERT INTO `animal_weights` (`weight_id`, `animal_id`, `weight_kg`, `weigh_date`, `record_guid`) VALUES (?, ?, 250.0, '2026-01-01', ?)",
                arrayOf<Any>(weightId, animalId, UUID.randomUUID().toString())
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 21, false, BeefTechDatabase.MIGRATION_20_21)

        // captured_at backfilled from movement_date on every animal_movements row.
        db.query(
            "SELECT `captured_at` FROM `animal_movements` WHERE `movement_id` = ?",
            arrayOf<Any>(matchedMovementId)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(5000L, c.getLong(0))
        }
        db.query(
            "SELECT `captured_at` FROM `animal_movements` WHERE `movement_id` = ?",
            arrayOf<Any>(unmatchedMovementId)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(6000L, c.getLong(0))
        }

        // The matched row's GPS/device/sync fields are restored from legacy_animal_movements.
        db.query(
            "SELECT `gps_lat`, `gps_lng`, `device_id`, `sync_status`, `synced_at` FROM `animal_movements` WHERE `movement_id` = ?",
            arrayOf<Any>(matchedMovementId)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(-26.2, c.getDouble(0), 0.0001)
            assertEquals(28.0, c.getDouble(1), 0.0001)
            assertEquals("legacy-device", c.getString(2))
            assertEquals("SYNCED", c.getString(3))
            assertEquals(4500L, c.getLong(4))
        }

        // The unmatched row keeps the plain ADD COLUMN defaults.
        db.query(
            "SELECT `gps_lat`, `gps_lng`, `device_id`, `sync_status`, `synced_at` FROM `animal_movements` WHERE `movement_id` = ?",
            arrayOf<Any>(unmatchedMovementId)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0.0, c.getDouble(0), 0.0001)
            assertEquals(0.0, c.getDouble(1), 0.0001)
            assertEquals("", c.getString(2))
            assertEquals("PENDING", c.getString(3))
            assertTrue(c.isNull(4))
        }

        // animal_weights has no legacy source, so it just keeps the defaults.
        db.query(
            "SELECT `gps_lat`, `gps_lng`, `device_id`, `captured_at`, `sync_status`, `synced_at` FROM `animal_weights` WHERE `weight_id` = ?",
            arrayOf<Any>(weightId)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0.0, c.getDouble(0), 0.0001)
            assertEquals(0.0, c.getDouble(1), 0.0001)
            assertEquals("", c.getString(2))
            assertEquals(0L, c.getLong(3))
            assertEquals("PENDING", c.getString(4))
            assertTrue(c.isNull(5))
        }
    }
}
