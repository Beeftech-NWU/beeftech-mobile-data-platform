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
class Migration45To46Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration45To46_preservesFarmerContactAndCreatesLinkTable() {
        helper.createDatabase("migration-45-46-test", 45).apply {
            execSQL(
                "INSERT INTO farmers (farmer_id, organisation_name, sync_status, " +
                    "contact_name, contact_number, farm_size_ha, head_count, " +
                    "primary_breed, record_guid) VALUES " +
                    "('farmer-1', 'Test Farm', 'SYNCED', 'Tester', '0123456789', " +
                    "123.5, 50, 'Bonsmara', 'guid-farmer-1')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "migration-45-46-test", 46, true, BeefTechDatabase.MIGRATION_45_46
        )
        db.query("SELECT contact_name, head_count FROM farmers WHERE farmer_id = 'farmer-1'")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Tester", cursor.getString(0))
                assertEquals(50, cursor.getInt(1))
            }
        db.query("PRAGMA table_info(farmer_animal_links)").use { cursor ->
            assertTrue(cursor.count >= 7)
        }
        db.close()
    }
}
