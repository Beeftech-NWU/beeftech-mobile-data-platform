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
 * R3, bullet 1: `animals.recordguid` and `treatments.recordguid` become
 * `record_guid`, via a table rebuild. Every row and every GUID value must
 * survive unchanged.
 */
@RunWith(AndroidJUnit4::class)
class Migration18To19Test {

    private val TEST_DB = "migration-test-18-19"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration18To19_renamesRecordGuid_andPreservesRows() {
        val animal1 = UUID.randomUUID().toString()
        val animal2 = UUID.randomUUID().toString()
        val animal1Guid = UUID.randomUUID().toString()
        val animal2Guid = UUID.randomUUID().toString()
        val treatment1Guid = UUID.randomUUID().toString()
        val treatment2Guid = UUID.randomUUID().toString()

        helper.createDatabase(TEST_DB, 18).use { db ->
            db.execSQL(
                "INSERT INTO `animals` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animal1, 1000L, "Bonsmara", 0.0, 0.0, 1000L, "device1", animal1Guid, "PENDING")
            )
            db.execSQL(
                "INSERT INTO `animals` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animal2, 2000L, "Angus", 0.0, 0.0, 2000L, "device1", animal2Guid, "PENDING")
            )

            db.execSQL(
                "INSERT INTO `treatments` (`animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`, `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animal1, "Foot Rot", "Antibiotic", "B1", "10ml", 50.0, 0.0, 0.0, 1000L, "device1", treatment1Guid, "PENDING")
            )
            db.execSQL(
                "INSERT INTO `treatments` (`animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`, `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animal2, "Pinkeye", "Eye Spray", "B2", "5ml", 20.0, 0.0, 0.0, 2000L, "device1", treatment2Guid, "PENDING")
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 19, true, BeefTechDatabase.MIGRATION_18_19)

        // animals: row count, and every GUID carried across under the new column name.
        val animalGuids = mutableSetOf<String>()
        db.query("SELECT `animalId`, `record_guid` FROM `animals`").use { c ->
            while (c.moveToNext()) {
                animalGuids.add(c.getString(1))
            }
            assertEquals(2, animalGuids.size)
        }
        assertTrue(animalGuids.contains(animal1Guid))
        assertTrue(animalGuids.contains(animal2Guid))

        db.query("SELECT `record_guid` FROM `animals` WHERE `animalId` = ?", arrayOf<Any>(animal1)).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(animal1Guid, c.getString(0))
        }

        // treatments: same checks.
        val treatmentGuids = mutableSetOf<String>()
        db.query("SELECT `id`, `record_guid` FROM `treatments`").use { c ->
            while (c.moveToNext()) {
                val guid = c.getString(1)
                assertTrue("record_guid must not be blank", guid.isNotBlank())
                treatmentGuids.add(guid)
            }
        }
        assertEquals("every treatment record_guid must be unique", 2, treatmentGuids.size)
        assertTrue(treatmentGuids.contains(treatment1Guid))
        assertTrue(treatmentGuids.contains(treatment2Guid))

        // The unique indices Room expects must exist under their new names.
        db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_animals_record_guid'"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_treatments_record_guid'"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
    }
}
