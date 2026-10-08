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
class Migration43To44Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            BeefTechDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migration43To44_addsAnEmptyRunHistoryAndKeepsQueuedWork() {

        helper.createDatabase(TEST_DATABASE, 43).apply {
            execSQL(
                "INSERT INTO pending_sync (user_id, entityType, entityId, operation, payload, createdAt, retryCount) " +
                    "VALUES ('u-1', 'ANIMAL_COST', 'c-1', 'INSERT', '{}', 1700000000000, 2)"
            )
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                44,
                true,
                BeefTechDatabase.MIGRATION_43_44
            )

        migrated.query("SELECT entityId, retryCount FROM pending_sync").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("c-1", cursor.getString(0))
            assertEquals(2, cursor.getInt(1))
        }

        migrated.query("SELECT COUNT(*) FROM sync_runs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }

        migrated.execSQL(
            "INSERT INTO sync_runs (user_id, module, `trigger`, started_at, finished_at, synced_count, failed_count, result) " +
                "VALUES ('u-1', 'COST', 'MANUAL', 1, 2, 3, 0, 'SUCCESS')"
        )
    }

    companion object {
        private const val TEST_DATABASE = "migration-43-44-test"
    }
}
