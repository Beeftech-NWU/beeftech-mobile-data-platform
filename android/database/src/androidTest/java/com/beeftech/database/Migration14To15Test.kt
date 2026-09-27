package com.beeftech.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration14To15Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate14To15_cleansOrphans_andEnforcesForeignKeys() {
        // 1. Create database in version 14 schema
        helper.createDatabase(TEST_DB, 14).apply {
            // Insert parent animal
            execSQL(
                """
                INSERT INTO animals (
                    animalId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, recordguid, syncStatus
                ) VALUES (
                    'ANIMAL-1', 1700000000000, 'Angus', 0.0, 0.0, 1700000000000, 'device-1', 'guid-animal-1', 'PENDING'
                )
                """.trimIndent()
            )

            // Insert valid treatment for ANIMAL-1
            execSQL(
                """
                INSERT INTO treatments (
                    id, animalId, disease, treatmentName, batchNumber, volumeUsed, cost, gpsLat, gpsLng, timestamp, recordguid
                ) VALUES (
                    1, 'ANIMAL-1', 'Flu', 'Vaccine A', 'BATCH1', '10ml', 50.0, 0.0, 0.0, 1700000000000, 'guid-treat-1'
                )
                """.trimIndent()
            )

            // Insert ORPHAN treatment (for non-existent ANIMAL-999)
            execSQL(
                """
                INSERT INTO treatments (
                    id, animalId, disease, treatmentName, batchNumber, volumeUsed, cost, gpsLat, gpsLng, timestamp, recordguid
                ) VALUES (
                    2, 'ANIMAL-999', 'Flu', 'Vaccine B', 'BATCH2', '5ml', 25.0, 0.0, 0.0, 1700000000000, 'guid-treat-2'
                )
                """.trimIndent()
            )

            // Insert parent farmer
            execSQL(
                """
                INSERT INTO farmers (
                    farmer_id, sync_status, record_guid
                ) VALUES (
                    'FARMER-1', 'PENDING', 'guid-farmer-1'
                )
                """.trimIndent()
            )

            // Insert parent role
            execSQL(
                """
                INSERT INTO roles (
                    role_id, role_name
                ) VALUES (
                    101, 'Manager'
                )
                """.trimIndent()
            )

            // Insert farmer_role whose role_id is string "101"
            execSQL(
                """
                INSERT INTO farmer_roles (
                    farmer_role_id, farmer_id, role_id
                ) VALUES (
                    'FR-1', 'FARMER-1', '101'
                )
                """.trimIndent()
            )

            close()
        }

        // 2. Run migration 14 -> 15 and validate schema
        val db = helper.runMigrationsAndValidate(TEST_DB, 15, true, BeefTechDatabase.MIGRATION_14_15)

        // 3. Assert: orphan treatment is gone, valid treatment survived
        db.query("SELECT id FROM treatments WHERE animalId = 'ANIMAL-999'").use { cursor ->
            assertFalse("Orphan treatment should be deleted by migration", cursor.moveToFirst())
        }
        db.query("SELECT id FROM treatments WHERE animalId = 'ANIMAL-1'").use { cursor ->
            assertTrue("Valid treatment should survive migration", cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }

        // Assert: farmer_role survived and role_id is numeric 101
        db.query("SELECT role_id FROM farmer_roles WHERE farmer_role_id = 'FR-1'").use { cursor ->
            assertTrue("FarmerRole should survive migration", cursor.moveToFirst())
            assertEquals(101L, cursor.getLong(0))
        }

        // 4. Assert CASCADE onDelete: delete parent ANIMAL-1 -> valid treatment should be cascade deleted
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM animals WHERE animalId = 'ANIMAL-1'")
        db.query("SELECT id FROM treatments WHERE animalId = 'ANIMAL-1'").use { cursor ->
            assertFalse("Treatment should be deleted when parent animal is deleted (CASCADE)", cursor.moveToFirst())
        }

        // Assert RESTRICT onDelete: deleting role 101 which is in use by farmer_roles should throw SQLiteConstraintException
        var constraintThrown = false
        try {
            db.execSQL("DELETE FROM roles WHERE role_id = 101")
        } catch (_: SQLiteConstraintException) {
            constraintThrown = true
        }
        assertTrue("Deleting a role in use must throw SQLiteConstraintException (RESTRICT)", constraintThrown)
    }
}
