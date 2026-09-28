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

/**
 * R5.1: `animal_costs.animalId` gains a foreign key to `animals`, the one
 * gap MIGRATION_14_16's referential-integrity pass never swept (animal_costs
 * didn't exist until MIGRATION_11_12, after that pass ran).
 */
@RunWith(AndroidJUnit4::class)
class Migration22To23Test {

    private val TEST_DB = "migration-test-22-23"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate22To23_addsAnimalIdForeignKey_rekeysTags_andQuarantinesOrphans() {
        helper.createDatabase(TEST_DB, 22).apply {
            execSQL(
                """
                INSERT INTO cost_types (code, display_name, sort_order, is_active)
                VALUES ('TRANSPORT', 'Transport', 0, 1)
                """.trimIndent()
            )

            // Normal row: already keyed on the animal's UUID.
            execSQL(
                """
                INSERT INTO animals (
                    animalId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'ANIMAL-UUID-1', 1700000000000, 'Angus', 0.0, 0.0, 1700000000000, 'device-1', 'guid-animal-1', 'PENDING'
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO animal_costs (
                    id, animalId, costType, amount, description, gpsLat, gpsLng, timestamp, record_guid
                ) VALUES (
                    1, 'ANIMAL-UUID-1', 'TRANSPORT', 100.0, 'Normal cost', 0.0, 0.0, 1000, 'guid-cost-1'
                )
                """.trimIndent()
            )

            // Tag-keyed row: written by a build before D1, keyed on the tag
            // rather than the UUID -- must be re-keyed, not quarantined.
            execSQL(
                """
                INSERT INTO animals (
                    animalId, tagNumber, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'ANIMAL-UUID-2', 'Blu0000002', 1700000000000, 'Angus', 0.0, 0.0, 1700000000000, 'device-1', 'guid-animal-2', 'PENDING'
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO animal_costs (
                    id, animalId, costType, amount, description, gpsLat, gpsLng, timestamp, record_guid
                ) VALUES (
                    2, 'Blu0000002', 'TRANSPORT', 50.0, 'Tag-keyed cost', 0.0, 0.0, 2000, 'guid-cost-2'
                )
                """.trimIndent()
            )

            // True orphan: no animal, no tag match anywhere.
            execSQL(
                """
                INSERT INTO animal_costs (
                    id, animalId, costType, amount, description, gpsLat, gpsLng, timestamp, record_guid
                ) VALUES (
                    3, 'NO-SUCH-ANIMAL', 'TRANSPORT', 25.0, 'Orphan cost', 0.0, 0.0, 3000, 'guid-cost-3'
                )
                """.trimIndent()
            )

            close()
        }

        // validateDroppedTables is false because quarantine_animal_costs is
        // intentionally not part of the Room schema (same reason as R0.5).
        val db = helper.runMigrationsAndValidate(TEST_DB, 23, false, BeefTechDatabase.MIGRATION_22_23)

        // Normal row survives unchanged.
        db.query("SELECT animalId FROM animal_costs WHERE id = 1").use { cursor ->
            assertTrue("Normal cost row should survive migration", cursor.moveToFirst())
            assertEquals("ANIMAL-UUID-1", cursor.getString(0))
        }

        // Tag-keyed row was re-keyed to the animal's UUID.
        db.query("SELECT animalId FROM animal_costs WHERE id = 2").use { cursor ->
            assertTrue("A tag-keyed cost must be re-keyed instead of deleted", cursor.moveToFirst())
            assertEquals("ANIMAL-UUID-2", cursor.getString(0))
        }

        // The true orphan is gone from animal_costs...
        db.query("SELECT id FROM animal_costs WHERE id = 3").use { cursor ->
            assertFalse("A true orphan cost must not survive in animal_costs", cursor.moveToFirst())
        }
        // ...but was quarantined, not silently dropped.
        db.query("SELECT animalId FROM quarantine_animal_costs WHERE id = 3").use { cursor ->
            assertTrue("A deleted cost row must survive in quarantine_animal_costs", cursor.moveToFirst())
            assertEquals("NO-SUCH-ANIMAL", cursor.getString(0))
        }

        // CASCADE: deleting the parent animal deletes its cost rows.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM animals WHERE animalId = 'ANIMAL-UUID-1'")
        db.query("SELECT id FROM animal_costs WHERE animalId = 'ANIMAL-UUID-1'").use { cursor ->
            assertFalse("animal_costs must be deleted when its parent animal is deleted (CASCADE)", cursor.moveToFirst())
        }

        // RESTRICT: the pre-existing costType FK must still hold.
        var constraintThrown = false
        try {
            db.execSQL("DELETE FROM cost_types WHERE code = 'TRANSPORT'")
        } catch (_: SQLiteConstraintException) {
            constraintThrown = true
        }
        assertTrue("Deleting a cost_type in use must still throw (RESTRICT)", constraintThrown)

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }
}
