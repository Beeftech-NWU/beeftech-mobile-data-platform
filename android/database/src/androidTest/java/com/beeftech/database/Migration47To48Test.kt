package com.beeftech.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class Migration47To48Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun migrationPreservesCribsAndReadingsAndAssignsMatchingUuid() {
        helper.createDatabase("migration-47-48-test", 47).apply {
            execSQL("INSERT INTO feed_cribs (crib_number, site_id, current_adi) VALUES ('A009', 'site-1', 1.5)")
            execSQL("INSERT INTO feed_crib_entries (record_guid, crib_number, site_id, reading_date, slot, adi, captured_at) VALUES ('reading-1', 'A009', 'site-1', '2026-10-11', 'MORNING', 1.6, 1000)")
            close()
        }
        val db = helper.runMigrationsAndValidate("migration-47-48-test", 48, true, FeedCribCreationMigration.MIGRATION_47_48)
        val uuid = UUID.nameUUIDFromBytes("site-1|A009".toByteArray()).toString()
        db.query("SELECT crib_uuid, current_adi, sync_status FROM feed_cribs").use {
            assertTrue(it.moveToFirst())
            assertEquals(uuid, it.getString(0))
            assertEquals(1.5, it.getDouble(1), 0.0)
            assertEquals("SYNCED", it.getString(2))
        }
        db.query("SELECT crib_uuid, sync_status FROM feed_crib_entries").use {
            assertTrue(it.moveToFirst())
            assertEquals(uuid, it.getString(0))
            assertEquals("PENDING", it.getString(1))
        }
        db.execSQL("INSERT INTO feed_cribs (crib_uuid, crib_number, site_id, initial_adi, current_adi, sync_status) VALUES ('new-uuid', 'A009', 'site-2', 2.0, 2.0, 'PENDING')")
        db.execSQL("INSERT INTO feed_crib_setup (userId, json) VALUES ('admin', '{}')")
        db.close()
    }
}

