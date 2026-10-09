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
class Migration44To45Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            BeefTechDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migration44To45_keepsExistingFarmersWithNoContactOrHerdDetails() {

        helper.createDatabase(TEST_DATABASE, 44).apply {
            execSQL(
                "INSERT INTO farmers (farmer_id, client_code, organisation_name, vat_number, email_address, gps_latitude, gps_longitude, sync_status, herd_capacity, interest_status, record_guid) " +
                    "VALUES ('f-1', 'C1', 'Org', NULL, 'org@example.com', -25.7, 28.2, 'PENDING', 450, 'Interested', 'farmer-guid-1')"
            )
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                45,
                true,
                BeefTechDatabase.MIGRATION_44_45
            )

        migrated
            .query(
                "SELECT farmer_id, sync_status, email_address, herd_capacity, interest_status, record_guid, " +
                    "contact_name, contact_number, farm_size_ha, head_count, primary_breed FROM farmers"
            )
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("f-1", cursor.getString(0))
                assertEquals("PENDING", cursor.getString(1))
                assertEquals("org@example.com", cursor.getString(2))
                assertEquals(450, cursor.getInt(3))
                assertEquals("Interested", cursor.getString(4))
                assertEquals("farmer-guid-1", cursor.getString(5))
                for (column in 6..10) {
                    assertTrue(cursor.isNull(column))
                }
            }
    }

    companion object {
        private const val TEST_DATABASE = "migration-44-45-test"
    }
}
