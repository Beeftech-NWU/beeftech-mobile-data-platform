package com.beeftech.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    private fun seedLookupTables(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO breeds (breedId, name) VALUES ('Bonsmara', 'Bonsmara')")
        db.execSQL("INSERT OR IGNORE INTO devices (deviceId) VALUES ('DEV-001')")
    }

    @Test
    fun migrate24To25_remapsKnownTypesAndQuarantinesUnknownTypes() {
        helper.createDatabase(TEST_DB, 24).apply {
            seedLookupTables(this)
            execSQL(
                """
                INSERT INTO animals (
                    animalId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES 
                ('ANIMAL-1', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-animal-1', 'PENDING'),
                ('ANIMAL-2', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-animal-2', 'PENDING')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO animal_identifiers (
                    identifier_id, animal_id, identifier_type, identifier_value, record_guid
                ) VALUES 
                ('ID-RFID', 'ANIMAL-1', 'RFID', 'RF-100', 'guid-rfid'),
                ('ID-OLDTAG', 'ANIMAL-1', 'OLDTAG', 'OLD-100', 'guid-oldtag'),
                ('ID-UNKNOWN', 'ANIMAL-2', 'INVALID_TYPE', 'UNK-100', 'guid-unknown')
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 25, false, BeefTechDatabase.MIGRATION_24_25)

        // Verify remapped identifier types
        db.query("SELECT identifier_id, identifier_type FROM animal_identifiers").use { cursor ->
            val typeMap = mutableMapOf<String, String>()
            while (cursor.moveToNext()) {
                typeMap[cursor.getString(0)] = cursor.getString(1)
            }
            assertEquals("TRANSPONDER", typeMap["ID-RFID"])
            assertEquals("OLD_TAG", typeMap["ID-OLDTAG"])
            assertFalse(typeMap.containsKey("ID-UNKNOWN"))
        }

        // Verify quarantine_animal_identifiers contains the unrecognized type
        db.query("SELECT identifier_id, identifier_type FROM quarantine_animal_identifiers").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ID-UNKNOWN", cursor.getString(0))
            assertEquals("INVALID_TYPE", cursor.getString(1))
        }

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    @Test
    fun migrate24To28_handlesDuplicateTagsConditionWithoutMassAndPreservesInLegacyAnimals() {
        helper.createDatabase(TEST_DB, 24).apply {
            seedLookupTables(this)
            execSQL(
                """
                INSERT INTO animal_groups (animalGroupId, groupName) VALUES ('GRP-1', 'Feedlot A')
                """.trimIndent()
            )
            // Two animals sharing the same tagNumber 'DUP-TAG'
            // ANIMAL-A has massKg and condition
            // ANIMAL-B has condition ONLY (massKg is NULL)
            execSQL(
                """
                INSERT INTO animals (
                    animalId, tagNumber, massKg, condition, birthdate, breed, animalGroupId,
                    photoPath, videoPath, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES 
                ('ANIMAL-A', 'DUP-TAG', 350.0, 'Good (3/5)', 1700000000000, 'Bonsmara', 'GRP-1', '/photoA.jpg', NULL, 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-a', 'PENDING'),
                ('ANIMAL-B', 'DUP-TAG', NULL, 'Score 2', 1700000000000, 'Bonsmara', 'GRP-1', NULL, '/videoB.mp4', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-b', 'PENDING'),
                ('ANIMAL-C', 'UNIQUE-TAG', 420.0, 'Score 4', 1700000000000, 'Bonsmara', 'GRP-1', NULL, NULL, 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-c', 'PENDING')
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            28,
            false,
            BeefTechDatabase.MIGRATION_24_25,
            BeefTechDatabase.MIGRATION_25_26,
            BeefTechDatabase.MIGRATION_26_27,
            BeefTechDatabase.MIGRATION_27_28
        )

        // Neither ANIMAL-A nor ANIMAL-B gets DUP-TAG in animal_identifiers
        db.query("SELECT animal_id FROM animal_identifiers WHERE identifier_value = 'DUP-TAG'").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }

        // ANIMAL-C gets UNIQUE-TAG
        db.query("SELECT animal_id FROM animal_identifiers WHERE identifier_value = 'UNIQUE-TAG'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ANIMAL-C", cursor.getString(0))
        }

        // ANIMAL-A with massKg creates a weight row
        db.query("SELECT weight_kg, body_condition_score FROM animal_weights WHERE animal_id = 'ANIMAL-A'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(350.0, cursor.getDouble(0), 0.01)
            assertEquals("Good (3/5)", cursor.getString(1))
        }

        // ANIMAL-B with NULL massKg creates NO weight row
        db.query("SELECT COUNT(*) FROM animal_weights WHERE animal_id = 'ANIMAL-B'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }

        // Verify legacy_animals holds DUP-TAG for ANIMAL-A and ANIMAL-B, and condition = 'Score 2' for ANIMAL-B
        db.query("SELECT animalId, tagNumber, condition FROM legacy_animals").use { cursor ->
            val tagMap = mutableMapOf<String, String?>()
            val condMap = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                tagMap[id] = cursor.getString(1)
                condMap[id] = cursor.getString(2)
            }
            assertEquals("DUP-TAG", tagMap["ANIMAL-A"])
            assertEquals("DUP-TAG", tagMap["ANIMAL-B"])
            assertEquals("Score 2", condMap["ANIMAL-B"])
        }

        // Verify non-empty and unique record_guids across backfilled tables
        val tables = listOf("animal_identifiers", "animal_media", "animal_weights", "animal_group_memberships")
        for (table in tables) {
            db.query("SELECT record_guid FROM $table").use { cursor ->
                val guids = mutableSetOf<String>()
                var count = 0
                while (cursor.moveToNext()) {
                    val g = cursor.getString(0)
                    assertTrue("record_guid must be non-empty in $table", !g.isNullOrBlank())
                    guids.add(g)
                    count++
                }
                if (count > 0) {
                    assertEquals("record_guid values must be unique in $table", count, guids.size)
                }
            }
        }

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    @Test
    fun migrate25To26_skipsGroupMembershipBackfillWhenOpenMembershipAlreadyExists() {
        helper.createDatabase(TEST_DB, 24).apply {
            seedLookupTables(this)
            execSQL("INSERT INTO animal_groups (animalGroupId, groupName) VALUES ('GRP-1', 'Group 1'), ('GRP-2', 'Group 2')")
            execSQL(
                """
                INSERT INTO animals (
                    animalId, animalGroupId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES (
                    'ANIMAL-GRP', 'GRP-2', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-grp', 'PENDING'
                )
                """.trimIndent()
            )
            // Existing open membership in GRP-1 prior to v25/v26
            execSQL(
                """
                INSERT INTO animal_group_memberships (
                    membership_id, animal_id, group_id, joined_at, left_at, record_guid
                ) VALUES (
                    'MEM-1', 'ANIMAL-GRP', 'GRP-1', 1600000000000, NULL, 'guid-mem-1'
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            28,
            false,
            BeefTechDatabase.MIGRATION_24_25,
            BeefTechDatabase.MIGRATION_25_26,
            BeefTechDatabase.MIGRATION_26_27,
            BeefTechDatabase.MIGRATION_27_28
        )

        // Animal still has exactly one open membership to GRP-1
        db.query("SELECT group_id FROM animal_group_memberships WHERE animal_id = 'ANIMAL-GRP' AND left_at IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("GRP-1", cursor.getString(0))
            assertFalse(cursor.moveToNext())
        }

        // legacy_animals preserves GRP-2 for ANIMAL-GRP
        db.query("SELECT animalGroupId FROM legacy_animals WHERE animalId = 'ANIMAL-GRP'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("GRP-2", cursor.getString(0))
        }

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    @Test
    fun migrate26To27And27To28_createsLegacyAnimalsAndResolvesParentageDeterministically() {
        helper.createDatabase(TEST_DB, 24).apply {
            seedLookupTables(this)
            execSQL(
                """
                INSERT INTO animals (
                    animalId, tagNumber, parentId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus
                ) VALUES 
                ('REAL-DAM', 'DAM-TAG-1', NULL, 1600000000000, 'Bonsmara', 0.0, 0.0, 1600000000000, 'DEV-001', 'guid-dam', 'PENDING'),
                ('DAM-A', NULL, NULL, 1600000000000, 'Bonsmara', 0.0, 0.0, 1600000000000, 'DEV-001', 'guid-dama', 'PENDING'),
                ('DAM-B', NULL, NULL, 1600000000000, 'Bonsmara', 0.0, 0.0, 1600000000000, 'DEV-001', 'guid-damb', 'PENDING'),
                ('CALF-VALID', NULL, 'REAL-DAM', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-calf1', 'PENDING'),
                ('CALF-TAG-MATCH', NULL, 'DAM-TAG-1', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-calf2', 'PENDING'),
                ('CALF-SELF', NULL, 'CALF-SELF', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-calf3', 'PENDING'),
                ('CALF-AMBIGUOUS', NULL, 'TAG-AMBIG', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-calf4', 'PENDING'),
                ('CALF-UNRESOLVED', NULL, 'NONEXISTENT-TAG', 1700000000000, 'Bonsmara', 0.0, 0.0, 1700000000000, 'DEV-001', 'guid-calf5', 'PENDING')
                """.trimIndent()
            )
            // Insert two animals in animal_identifiers holding the same TAG value 'TAG-AMBIG'
            // (DAM-A holds it closed, DAM-B holds it active so trigger allows insertion)
            execSQL(
                """
                INSERT INTO animal_identifiers (
                    identifier_id, animal_id, identifier_type, identifier_value, valid_from, valid_to, record_guid
                ) VALUES 
                ('ID-AMBIG-1', 'DAM-A', 'TAG', 'TAG-AMBIG', 1600000000000, 1650000000000, 'guid-ambig1'),
                ('ID-AMBIG-2', 'DAM-B', 'TAG', 'TAG-AMBIG', 1660000000000, NULL, 'guid-ambig2')
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            28,
            false,
            BeefTechDatabase.MIGRATION_24_25,
            BeefTechDatabase.MIGRATION_25_26,
            BeefTechDatabase.MIGRATION_26_27,
            BeefTechDatabase.MIGRATION_27_28
        )

        // Verify legacy_animals snapshot preserves all parentId values
        db.query("SELECT animalId, parentId FROM legacy_animals").use { cursor ->
            val parentMap = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) {
                parentMap[cursor.getString(0)] = cursor.getString(1)
            }
            assertEquals("REAL-DAM", parentMap["CALF-VALID"])
            assertEquals("DAM-TAG-1", parentMap["CALF-TAG-MATCH"])
            assertEquals("CALF-SELF", parentMap["CALF-SELF"])
            assertEquals("TAG-AMBIG", parentMap["CALF-AMBIGUOUS"])
            assertEquals("NONEXISTENT-TAG", parentMap["CALF-UNRESOLVED"])
        }

        // Verify dam_id resolution in final schema
        db.query("SELECT animalId, dam_id FROM animals").use { cursor ->
            val damMap = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) {
                damMap[cursor.getString(0)] = cursor.getString(1)
            }
            assertEquals("REAL-DAM", damMap["CALF-VALID"])
            assertEquals("REAL-DAM", damMap["CALF-TAG-MATCH"])
            assertNull("Self-parentage must resolve to NULL", damMap["CALF-SELF"])
            assertNull("Ambiguous parentage must resolve to NULL", damMap["CALF-AMBIGUOUS"])
            assertNull("Unresolved parentage must resolve to NULL", damMap["CALF-UNRESOLVED"])
        }

        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    @Test
    fun seedCallback_createsAllFiveHistoryTriggersOnFreshInstall() {
        val context: Context = ApplicationProvider.getApplicationContext<Context>()

        val db = Room.inMemoryDatabaseBuilder(context, BeefTechDatabase::class.java)
            .addCallback(BeefTechDatabase.SEED_CALLBACK)
            .build()

        val sqliteDb = db.openHelper.writableDatabase

        sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'trigger'").use { cursor ->
            val triggers = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                triggers.add(cursor.getString(0))
            }
            assertTrue("trg_animal_identifiers_unique_active missing", triggers.contains("trg_animal_identifiers_unique_active"))
            assertTrue("trg_animal_identifiers_unique_active_update missing", triggers.contains("trg_animal_identifiers_unique_active_update"))
            assertTrue("trg_animal_identifiers_tag_permanence missing", triggers.contains("trg_animal_identifiers_tag_permanence"))
            assertTrue("trg_animal_group_memberships_single_open missing", triggers.contains("trg_animal_group_memberships_single_open"))
            assertTrue("trg_animal_group_memberships_single_open_update missing", triggers.contains("trg_animal_group_memberships_single_open_update"))
        }

        db.close()
    }
}
