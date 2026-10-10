package com.beeftech.database

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

@RunWith(AndroidJUnit4::class)
class Migration46To47Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    private fun tableExists(db: androidx.sqlite.db.SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$name'").use { it.count == 1 }

    private fun columns(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Set<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val names = mutableSetOf<String>()
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) names += cursor.getString(nameIndex)
            names
        }

    @Test
    fun migration46To47_replacesTheStubTablesAndLeavesOtherDataAlone() {
        helper.createDatabase("migration-46-47-test", 46).apply {
            execSQL("INSERT INTO feed_cribs (id, name) VALUES ('stub-crib', 'Old stub')")
            execSQL(
                "INSERT INTO farmers (farmer_id, organisation_name, sync_status, record_guid) " +
                    "VALUES ('farmer-1', 'Test Farm', 'SYNCED', 'guid-farmer-1')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "migration-46-47-test", 47, true, BeefTechDatabase.MIGRATION_46_47
        )

        assertFalse(tableExists(db, "feed_crib_readings"))
        assertFalse(tableExists(db, "feed_crib_reading_values"))
        assertTrue(tableExists(db, "crib_reading_codes"))
        assertTrue(tableExists(db, "feed_crib_entries"))

        /* The old stub shape is gone: the new table is keyed by crib number and starts empty. */
        assertTrue("crib_number" in columns(db, "feed_cribs"))
        assertFalse("name" in columns(db, "feed_cribs"))
        db.query("SELECT COUNT(*) FROM feed_cribs").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }

        db.query("SELECT organisation_name FROM farmers WHERE farmer_id = 'farmer-1'").use {
            assertTrue(it.moveToFirst())
            assertEquals("Test Farm", it.getString(0))
        }
        db.close()
    }
}
