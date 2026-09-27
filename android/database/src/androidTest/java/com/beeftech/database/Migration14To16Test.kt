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
class Migration14To16Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate14To16_cleansOrphans_andEnforcesForeignKeys() {
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

        // 2. Run migration 14 -> 16 and validate schema.
        // validateDroppedTables is false because R0.5's quarantine_<table>
        // tables are intentionally not part of the Room schema.
        val db = helper.runMigrationsAndValidate(TEST_DB, 16, false, BeefTechDatabase.MIGRATION_14_16)

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

    // R0.4 (N1): before #47, `roles` was only ever seeded in SEED_CALLBACK.onOpen,
    // which runs after every migration finishes. So `roles` was empty while
    // 14->16 ran, and every farmer_roles row looked orphaned and was deleted.
    @Test
    fun migrate14To16_seedsRolesFirst_soFarmerRolesSurviveWhenRolesStartsEmpty() {
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL(
                """
                INSERT INTO farmers (farmer_id, sync_status, record_guid)
                VALUES ('FARMER-2', 'PENDING', 'guid-farmer-2')
                """.trimIndent()
            )

            // `roles` is empty here -- nothing seeds it before this migration ran, pre-#47.
            execSQL(
                """
                INSERT INTO farmer_roles (farmer_role_id, farmer_id, role_id)
                VALUES ('FR-2', 'FARMER-2', '1')
                """.trimIndent()
            )

            // A genuinely bad role_id, which must still be rejected.
            execSQL(
                """
                INSERT INTO farmer_roles (farmer_role_id, farmer_id, role_id)
                VALUES ('FR-3', 'FARMER-2', '999')
                """.trimIndent()
            )

            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 16, false, BeefTechDatabase.MIGRATION_14_16)

        db.query("SELECT role_id FROM roles").use { cursor ->
            assertEquals("RoleSeed should have run inside the migration", 3, cursor.count)
        }

        db.query("SELECT role_id FROM farmer_roles WHERE farmer_role_id = 'FR-2'").use { cursor ->
            assertTrue("A farmer_role with a real role_id must survive even though roles started empty", cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }

        db.query("SELECT role_id FROM farmer_roles WHERE farmer_role_id = 'FR-3'").use { cursor ->
            assertFalse("A farmer_role with a bad role_id must still be rejected", cursor.moveToFirst())
        }
    }

    // R0.5 (N2): deleted rows must be quarantined, never silently dropped, and
    // a row keyed on a tag by a build before D1 must be re-keyed to the
    // animal's UUID before it's treated as an orphan.
    @Test
    fun migrate14To16_quarantinesDeletedRows_andRekeysTagsBeforeTreatingThemAsOrphans() {
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL(
                """
                INSERT INTO animals (
                    animalId, tagNumber, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, recordguid, syncStatus
                ) VALUES (
                    'ANIMAL-UUID-1', 'Blu0000001', 1700000000000, 'Angus', 0.0, 0.0, 1700000000000, 'device-1', 'guid-animal-3', 'PENDING'
                )
                """.trimIndent()
            )

            // Written by a build before D1: keyed on the tag, not the UUID.
            execSQL(
                """
                INSERT INTO treatments (
                    id, animalId, disease, treatmentName, batchNumber, volumeUsed, cost, gpsLat, gpsLng, timestamp, recordguid
                ) VALUES (
                    10, 'Blu0000001', 'Flu', 'Vaccine C', 'BATCH3', '5ml', 15.0, 0.0, 0.0, 1700000000000, 'guid-treat-tagged'
                )
                """.trimIndent()
            )

            // A true orphan: no animal, no tag match anywhere.
            execSQL(
                """
                INSERT INTO treatments (
                    id, animalId, disease, treatmentName, batchNumber, volumeUsed, cost, gpsLat, gpsLng, timestamp, recordguid
                ) VALUES (
                    11, 'NO-SUCH-ANIMAL', 'Flu', 'Vaccine D', 'BATCH4', '5ml', 15.0, 0.0, 0.0, 1700000000000, 'guid-treat-orphan'
                )
                """.trimIndent()
            )

            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 16, false, BeefTechDatabase.MIGRATION_14_16)

        // The tag-keyed row was re-keyed to the animal's UUID and survives.
        db.query("SELECT animalId FROM treatments WHERE id = 10").use { cursor ->
            assertTrue("A tag-keyed treatment must be re-keyed instead of deleted", cursor.moveToFirst())
            assertEquals("ANIMAL-UUID-1", cursor.getString(0))
        }

        // The true orphan is gone from treatments...
        db.query("SELECT id FROM treatments WHERE id = 11").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
        // ...but was quarantined, not silently dropped.
        db.query("SELECT animalId FROM quarantine_treatments WHERE id = 11").use { cursor ->
            assertTrue("A deleted treatment must survive in quarantine_treatments", cursor.moveToFirst())
            assertEquals("NO-SUCH-ANIMAL", cursor.getString(0))
        }
    }
}
