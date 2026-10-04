package com.beeftech.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration36To37Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry
                .getInstrumentation(),

            BeefTechDatabase::class.java
                .canonicalName!!,

            FrameworkSQLiteOpenHelperFactory()
        )

    private fun insertEvent(
        db: SupportSQLiteDatabase,
        key: String,
        type: String
    ) {

        db.execSQL(
            "INSERT INTO sync_security_events (event_key, user_id, event_type, event_time, warning_day, pending_count, oldest_pending_created_at, details) " +
                "VALUES ('$key', 'user-1', '$type', 1700000000000, 7, 4, 1690000000000, 'recorded before v37')"
        )
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM `$table`").use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test
    fun migration36To37_keepsEveryEventAndLeavesItNotUploaded() {

        helper
            .createDatabase(
                TEST_DATABASE,
                36
            )
            .apply {

                insertEvent(this, "user-1:DAY7_WIPE:1", "DAY_7_WIPE")
                insertEvent(this, "user-1:DAY7_LOCK:1", "SYNC_POLICY_ACCOUNT_LOCKED")

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                37,
                true,
                BeefTechDatabase.MIGRATION_36_37
            )

        assertEquals(2, count(migrated, "sync_security_events"))

        migrated
            .query("SELECT event_type, pending_count, details, uploaded_at FROM sync_security_events WHERE event_key = 'user-1:DAY7_WIPE:1'")
            .use { cursor ->

                assertTrue(cursor.moveToFirst())
                assertEquals("DAY_7_WIPE", cursor.getString(0))
                assertEquals(4, cursor.getInt(1))
                assertEquals("recorded before v37", cursor.getString(2))
                assertTrue("Old events must upload once.", cursor.isNull(3))
            }

        migrated.close()
    }

    @Test
    fun migration36To37_isIdempotentWhenTheColumnAlreadyExists() {

        helper
            .createDatabase(
                TEST_DATABASE_IDEMPOTENT,
                36
            )
            .apply {

                insertEvent(this, "user-1:WARNING:2:1", "SYNC_WARNING")

                /* A half-applied earlier attempt: the column exists and holds a value. */
                execSQL("ALTER TABLE `sync_security_events` ADD COLUMN `uploaded_at` INTEGER")
                execSQL("UPDATE sync_security_events SET uploaded_at = 5")

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE_IDEMPOTENT,
                37,
                true,
                BeefTechDatabase.MIGRATION_36_37
            )

        migrated
            .query("SELECT uploaded_at FROM sync_security_events")
            .use { cursor ->

                assertTrue(cursor.moveToFirst())
                assertEquals("The existing value must survive.", 5, cursor.getInt(0))
            }

        migrated.close()
    }

    companion object {

        private const val TEST_DATABASE =
            "migration-36-37-test"

        private const val TEST_DATABASE_IDEMPOTENT =
            "migration-36-37-idempotent-test"
    }
}
