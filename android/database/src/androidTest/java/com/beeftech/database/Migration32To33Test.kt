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
class Migration32To33Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry
                .getInstrumentation(),

            BeefTechDatabase::class.java
                .canonicalName!!,

            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migration32To33_preservesUserAndAddsNullSiteId() {

        helper
            .createDatabase(
                TEST_DATABASE,
                32
            )
            .apply {

                execSQL(
                    "INSERT OR IGNORE INTO roles (role_id, role_name) VALUES (3, 'Worker')"
                )

                execSQL(
                    """
                    INSERT INTO users (
                        user_id,
                        username,
                        pin_hash,
                        failed_pin_attempts,
                        role,
                        failed_sync_attempts
                    )
                    VALUES (
                        'LEGACY-USER',
                        'jvdm',
                        'hash',
                        0,
                        3,
                        0
                    )
                    """.trimIndent()
                )

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                33,
                true,
                BeefTechDatabase.MIGRATION_32_33
            )

        migrated
            .query(
                """
                SELECT username, role, site_id
                FROM users
                WHERE user_id = 'LEGACY-USER'
                """.trimIndent()
            )
            .use { cursor ->

                assertTrue(
                    "Legacy user must survive migration.",
                    cursor.moveToFirst()
                )

                assertEquals(
                    "jvdm",
                    cursor.getString(0)
                )

                assertEquals(
                    3,
                    cursor.getInt(1)
                )

                assertTrue(
                    "site_id must be NULL for legacy users.",
                    cursor.isNull(2)
                )
            }

        migrated.close()
    }

    @Test
    fun migration32To33_isIdempotentWhenColumnAlreadyExists() {

        helper
            .createDatabase(
                TEST_DATABASE_IDEMPOTENT,
                32
            )
            .apply {

                /*
                 * Simulate a partially applied migration.
                 */
                execSQL(
                    "ALTER TABLE `users` ADD COLUMN `site_id` TEXT"
                )

                close()
            }

        val migrated: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(
                TEST_DATABASE_IDEMPOTENT,
                33,
                true,
                BeefTechDatabase.MIGRATION_32_33
            )

        migrated.close()
    }

    companion object {

        private const val TEST_DATABASE =
            "migration-32-33-test"

        private const val TEST_DATABASE_IDEMPOTENT =
            "migration-32-33-idempotent-test"
    }
}
