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
class Migration42To43Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            BeefTechDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migration42To43_keepsExistingFarmersWithNoCapacityOrInterest() {

        helper.createDatabase(TEST_DATABASE, 42).apply {
            execSQL(
                "INSERT INTO farmers (farmer_id, client_code, organisation_name, vat_number, email_address, gps_latitude, gps_longitude, sync_status, record_guid) " +
                    "VALUES ('f-1', 'C1', 'Org', NULL, NULL, 0.0, 0.0, 'PENDING', 'farmer-guid-1')"
            )
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                43,
                true,
                BeefTechDatabase.MIGRATION_42_43
            )

        migrated
            .query("SELECT farmer_id, sync_status, herd_capacity, interest_status FROM farmers")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("f-1", cursor.getString(0))
                assertEquals("PENDING", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
            }
    }

    companion object {
        private const val TEST_DATABASE = "migration-42-43-test"
    }
}
