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
class Migration40To41Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            BeefTechDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migration40To41_keepsExistingPhotosAsPendingUploads() {

        helper.createDatabase(TEST_DATABASE, 40).apply {
            execSQL(
                "INSERT INTO animals (animalId, birthdate, breed, gpsLat, gpsLng, captureAt, deviceId, record_guid, syncStatus) " +
                    "VALUES ('a-1', 1700000000000, 'Brangus', 0.0, 0.0, 1700000100000, 'dev', 'animal-guid-1', 'PENDING')"
            )
            execSQL(
                "INSERT INTO animal_media (media_id, animal_id, file_path, media_type, created_at, record_guid) " +
                    "VALUES ('m-1', 'a-1', '/files/calf-photos/p.jpg', 'PHOTO', 1700000100000, 'media-guid-1')"
            )
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                41,
                true,
                BeefTechDatabase.MIGRATION_40_41
            )

        migrated
            .query("SELECT media_id, file_path, upload_status, upload_error FROM animal_media")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("m-1", cursor.getString(0))
                assertEquals("/files/calf-photos/p.jpg", cursor.getString(1))
                assertEquals("PENDING", cursor.getString(2))
                assertTrue(cursor.isNull(3))
            }
    }

    companion object {
        private const val TEST_DATABASE = "migration-40-41-test"
    }
}
