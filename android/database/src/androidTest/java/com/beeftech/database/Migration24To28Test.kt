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

@RunWith(AndroidJUnit4::class)
class Migration24To28Test {

    private val TEST_DB = "migration-test-24-28"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate24To25_addsBodyConditionScoreAndIdentifierTypes() {
        helper.createDatabase(TEST_DB, 24).apply {
            execSQL(
                """
                INSERT INTO animals (
                    animalId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'ANIMAL-1', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-animal-1', 'PENDING'
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 25, true, BeefTechDatabase.MIGRATION_24_25)

        // Verify identifier_types lookup table
        db.query("SELECT COUNT(*) FROM identifier_types").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(0))
        }

        // Verify body_condition_score column exists on animal_weights
        db.execSQL(
            """
            INSERT INTO animal_weights (
                weight_id, animal_id, weight_kg, weigh_date, body_condition_score, record_guid, gps_lat, gps_lng, device_id, captured_at, sync_status
            ) VALUES (
                'WEIGHT-1', 'ANIMAL-1', 300.0, 1700000000000, 'Good (3/5)', 'guid-weight-1', 0.0, 0.0, 'DEV-001', 1700000000000, 'PENDING'
            )
            """.trimIndent()
        )

        db.query("SELECT body_condition_score FROM animal_weights WHERE weight_id = 'WEIGHT-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Good (3/5)", cursor.getString(0))
        }

        // Verify trigger trg_animal_identifiers_unique_active prevents duplicate active identifier of same type
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            """
            INSERT INTO animal_identifiers (
                identifier_id, animal_id, identifier_type, identifier_value, valid_from, record_guid
            ) VALUES (
                'ID-1', 'ANIMAL-1', 'TAG', 'Blu0000001', 1700000000000, 'guid-id-1'
            )
            """.trimIndent()
        )

        var constraintThrown = false
        try {
            db.execSQL(
                """
                INSERT INTO animal_identifiers (
                    identifier_id, animal_id, identifier_type, identifier_value, valid_from, record_guid
                ) VALUES (
                    'ID-2', 'ANIMAL-1', 'TAG', 'Blu0000002', 1700000000000, 'guid-id-2'
                )
                """.trimIndent()
            )
        } catch (_: SQLiteConstraintException) {
            constraintThrown = true
        }
        assertTrue("Duplicate active identifier of same type must be rejected by trigger", constraintThrown)

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    @Test
    fun migrate25To26_backfillsNormalizedTablesFromLegacyColumns() {
        helper.createDatabase(TEST_DB, 24).apply {
            execSQL(
                """
                INSERT INTO animal_groups (animalGroupId, groupName) VALUES ('GRP-1', 'Feedlot A')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO animals (
                    animalId, tagNumber, oldTagNumber, referenceNumber, temperatureNumber,
                    massKg, condition, birthdate, breed, animalGroupId, photoPath, videoPath,
                    gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'ANIMAL-2', 'Blu0000002', 'OLD-TAG-2', 'REF-2', 'TEMP-2',
                    420.0, 'Score 3', 1700000000000, 'Bonsmara', 'GRP-1', '/photo.jpg', '/video.mp4',
                    0.0, 0.0, 1700000000000, 'DEV-001', 'guid-animal-2', 'PENDING'
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            26,
            true,
            BeefTechDatabase.MIGRATION_24_25,
            BeefTechDatabase.MIGRATION_25_26
        )

        // Verify backfilled identifiers
        db.query("SELECT identifier_type, identifier_value FROM animal_identifiers WHERE animal_id = 'ANIMAL-2'").use { cursor ->
            val map = mutableMapOf<String, String>()
            while (cursor.moveToNext()) {
                map[cursor.getString(0)] = cursor.getString(1)
            }
            assertEquals("Blu0000002", map["TAG"])
            assertEquals("OLD-TAG-2", map["OLD_TAG"])
            assertEquals("REF-2", map["REFERENCE"])
            assertEquals("TEMP-2", map["TEMPERATURE"])
        }

        // Verify backfilled media
        db.query("SELECT media_type, file_path FROM animal_media WHERE animal_id = 'ANIMAL-2'").use { cursor ->
            val map = mutableMapOf<String, String>()
            while (cursor.moveToNext()) {
                map[cursor.getString(0)] = cursor.getString(1)
            }
            assertEquals("/photo.jpg", map["PHOTO"])
            assertEquals("/video.mp4", map["VIDEO"])
        }

        // Verify backfilled weights
        db.query("SELECT weight_kg, body_condition_score FROM animal_weights WHERE animal_id = 'ANIMAL-2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(420.0, cursor.getDouble(0), 0.01)
            assertEquals("Score 3", cursor.getString(1))
        }

        // Verify backfilled group membership
        db.query("SELECT group_id FROM animal_group_memberships WHERE animal_id = 'ANIMAL-2' AND left_at IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("GRP-1", cursor.getString(0))
        }

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    @Test
    fun migrate26To27And27To28_replacesParentIdAndFinalizesSchema() {
        helper.createDatabase(TEST_DB, 24).apply {
            execSQL(
                """
                INSERT INTO animals (
                    animalId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'DAM-1', 1600000000000, 'Bonsmara', 0.0, 0.0, 1600000000000, 'DEV-001', 'guid-dam-1', 'PENDING'
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO animals (
                    animalId, parentId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'CALF-1', 'DAM-1', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-calf-1', 'PENDING'
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            28,
            true,
            BeefTechDatabase.MIGRATION_24_25,
            BeefTechDatabase.MIGRATION_25_26,
            BeefTechDatabase.MIGRATION_26_27,
            BeefTechDatabase.MIGRATION_27_28
        )

        // Verify dam_id on calf
        db.query("SELECT dam_id FROM animals WHERE animalId = 'CALF-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("DAM-1", cursor.getString(0))
        }

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }
}
