package com.beeftech.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * R7: Lookups and compliance fields migration test (v23 -> v24).
 * Verifies controlled vocabulary lookup tables are populated, FK constraints are enforced,
 * and existing records migrate cleanly.
 */
@RunWith(AndroidJUnit4::class)
class Migration23To24Test {

    private val TEST_DB = "migration-test-23-24"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate23To24_createsLookupTablesAndEnforcesForeignKeys() {
        helper.createDatabase(TEST_DB, 23).apply {
            // Seed a farmer and address
            execSQL(
                """
                INSERT INTO farmers (farmer_id, organisation_name, sync_status, record_guid)
                VALUES ('FARMER-1', 'Doe Farms', 'PENDING', 'guid-farmer-1')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO farmer_addresses (
                    address_id, farmer_id, address_type, address_line_1, province, postal_code, gps_latitude, gps_longitude, record_guid
                ) VALUES (
                    'ADDR-1', 'FARMER-1', 'PHYSICAL', '123 Farm Rd', 'Free State', '9300', 0.0, 0.0, 'guid-addr-1'
                )
                """.trimIndent()
            )

            // Seed animal
            execSQL(
                """
                INSERT INTO animals (
                    animalId, birthdate, breed, hideColour, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'ANIMAL-1', 1700000000000, 'Bonsmara', 'Red', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-animal-1', 'PENDING'
                )
                """.trimIndent()
            )

            // Seed treatment
            execSQL(
                """
                INSERT INTO treatments (
                    id, animalId, disease, treatmentName, batchNumber, volumeUsed, cost, gpsLat, gpsLng, timestamp, deviceId, record_guid, syncStatus
                ) VALUES (
                    1, 'ANIMAL-1', 'Bovine respiratory disease', 'Antibiotic', 'B1', '10ml', 100.0, 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-treatment-1', 'PENDING'
                )
                """.trimIndent()
            )

            // Seed mortality
            execSQL(
                """
                INSERT INTO mortalities (
                    id, animalId, causeOfDeath, responsibleWorker, notes, timestamp, record_guid
                ) VALUES (
                    1, 'ANIMAL-1', 'Bloat / Gastrointestinal Distress', 'Worker 1', 'Found dead in pen', 1700000000000, 'guid-mortality-1'
                )
                """.trimIndent()
            )

            // Seed movement
            execSQL(
                """
                INSERT INTO animal_movements (
                    movement_id, animal_id, destination_farm_id, destination_pen_id, movement_date, record_guid, gps_lat, gps_lng, device_id, captured_at, sync_status
                ) VALUES (
                    'MOVE-1', 'ANIMAL-1', 'FARM-1', 'PEN-1', 1700000000000, 'guid-movement-1', 0.0, 0.0, 'DEV-001', 1700000000000, 'PENDING'
                )
                """.trimIndent()
            )

            // Seed weight
            execSQL(
                """
                INSERT INTO animal_weights (
                    weight_id, animal_id, weight_kg, weigh_date, record_guid, gps_lat, gps_lng, device_id, captured_at, sync_status
                ) VALUES (
                    'WEIGHT-1', 'ANIMAL-1', 250.0, 1700000000000, 'guid-weight-1', 0.0, 0.0, 'DEV-001', 1700000000000, 'PENDING'
                )
                """.trimIndent()
            )

            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 24, true, BeefTechDatabase.MIGRATION_23_24)

        // Verify lookup tables exist and contain standard seeds
        db.query("SELECT COUNT(*) FROM breeds").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue("Breeds lookup table should be populated", cursor.getInt(0) > 0)
        }
        db.query("SELECT COUNT(*) FROM hide_colours").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue("Hide colours lookup table should be populated", cursor.getInt(0) > 0)
        }
        db.query("SELECT COUNT(*) FROM diseases").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue("Diseases lookup table should be populated", cursor.getInt(0) > 0)
        }
        db.query("SELECT COUNT(*) FROM devices WHERE deviceId = 'DEV-001'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        // Verify mortality cause was mapped to necropsy code
        db.query("SELECT necropsy_code_id FROM mortalities WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("N02", cursor.getString(0))
        }

        // Verify RESTRICT foreign key behavior on breeds
        db.execSQL("PRAGMA foreign_keys = ON")
        var constraintThrown = false
        try {
            db.execSQL("DELETE FROM breeds WHERE breedId = 'Bonsmara'")
        } catch (_: SQLiteConstraintException) {
            constraintThrown = true
        }
        assertTrue("Deleting a breed in use must throw SQLiteConstraintException (RESTRICT)", constraintThrown)

        // PRAGMA foreign_key_check passes
        db.query("PRAGMA foreign_key_check").use { cursor ->
            assertEquals("PRAGMA foreign_key_check must have 0 violations", 0, cursor.count)
        }
    }
}
