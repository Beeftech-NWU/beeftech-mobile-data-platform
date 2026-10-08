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

@RunWith(AndroidJUnit4::class)
class Migration39To40Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            BeefTechDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migration39To40_keepsRegistrationsWithNoErrorAndZeroAttempts() {

        helper.createDatabase(TEST_DATABASE, 39).apply {
            execSQL(
                "INSERT INTO animals (animalId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus) " +
                    "VALUES ('a-1', 1700000000000, 'Brangus', 0.0, 0.0, 1700000100000, 'dev', 'animal-guid-1', 'PENDING')"
            )
            execSQL(
                "INSERT INTO calf_registrations (registration_id, registered_animal_id, registration_date, record_guid, sync_status) " +
                    "VALUES ('r-1', 'a-1', 1700000100000, 'reg-guid-1', 'PENDING')"
            )
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                40,
                true,
                BeefTechDatabase.MIGRATION_39_40
            )

        migrated
            .query("SELECT registration_id, sync_status, sync_error, sync_attempts FROM calf_registrations")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("r-1", cursor.getString(0))
                assertEquals("PENDING", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertEquals(0, cursor.getInt(3))
            }
    }

    companion object {
        private const val TEST_DATABASE = "migration-39-40-test"
    }
}
