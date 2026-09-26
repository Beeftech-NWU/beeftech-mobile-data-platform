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

@RunWith(AndroidJUnit4::class)
class Migration14To15Test {

    private val TEST_DB = "migration-test-14-15"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration14To15_addsColumns_andPreservesRows() {
        val animal1 = UUID.randomUUID().toString()
        val animal2 = UUID.randomUUID().toString()
        val reg1 = UUID.randomUUID().toString()
        val reg2 = UUID.randomUUID().toString()

        helper.createDatabase(TEST_DB, 14).use { db ->
            // Insert 2 animals
            db.execSQL(
                "INSERT INTO `animals` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animal1, 1000L, "Bonsmara", 0.0, 0.0, 1000L, "device1", UUID.randomUUID().toString(), "PENDING")
            )
            db.execSQL(
                "INSERT INTO `animals` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animal2, 2000L, "Angus", 0.0, 0.0, 2000L, "device1", UUID.randomUUID().toString(), "PENDING")
            )

            // Insert 2 registrations
            db.execSQL(
                "INSERT INTO `calf_registrations` (`registration_id`, `registered_animal_id`, `registration_date`) VALUES (?, ?, ?)",
                arrayOf<Any>(reg1, animal1, "2025-01-01")
            )
            db.execSQL(
                "INSERT INTO `calf_registrations` (`registration_id`, `registered_animal_id`, `registration_date`) VALUES (?, ?, ?)",
                arrayOf<Any>(reg2, animal2, "2025-01-02")
            )
        }

        // Run migration to v15 and validate
        val db = helper.runMigrationsAndValidate(TEST_DB, 15, true, BeefTechDatabase.MIGRATION_14_15)

        val regIds = mutableListOf<String>()

        db.query("SELECT `registration_id`, `record_guid`, `sync_status` FROM `calf_registrations`").use { c ->
            while (c.moveToNext()) {
                regIds.add(c.getString(0))
            }
        }

        assertEquals(2, regIds.size)
        assertTrue(regIds.contains(reg1))
        assertTrue(regIds.contains(reg2))

        // Assert record_guid == registration_id and sync_status == 'PENDING'
        db.query("SELECT `registration_id`, `record_guid`, `sync_status` FROM `calf_registrations` WHERE `registration_id` = '$reg1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(reg1, c.getString(1))
            assertEquals("PENDING", c.getString(2))
        }
        db.query("SELECT `registration_id`, `record_guid`, `sync_status` FROM `calf_registrations` WHERE `registration_id` = '$reg2'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(reg2, c.getString(1))
            assertEquals("PENDING", c.getString(2))
        }
    }
}
