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
class Migration30To31Test {

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
    fun migration30To31_preservesLegacyPendingRowWithNullUserId() {

        /*
         * Create a genuine version-30 database.
         */
        helper
            .createDatabase(
                TEST_DATABASE,
                30
            )
            .apply {

                /*
                 * Version 30 has no user_id column.
                 */
                execSQL(
                    """
                    INSERT INTO pending_sync (
                        entityType,
                        entityId,
                        operation,
                        payload,
                        createdAt,
                        retryCount
                    )
                    VALUES (
                        'FARMER_REGISTRATION',
                        'LEGACY-FARMER',
                        'CREATE',
                        'LEGACY-FARMER',
                        1700000000000,
                        0
                    )
                    """.trimIndent()
                )

                close()
            }

        /*
         * Perform only the real production 30 -> 31 migration.
         */
        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                31,
                true,
                BeefTechDatabase.MIGRATION_30_31
            )

        /*
         * Existing record must survive.
         */
        migrated
            .query(
                """
                SELECT
                    entityId,
                    user_id
                FROM pending_sync
                WHERE entityId = 'LEGACY-FARMER'
                """.trimIndent()
            )
            .use { cursor ->

                assertTrue(
                    "Legacy pending row must survive migration.",
                    cursor.moveToFirst()
                )

                assertEquals(
                    "LEGACY-FARMER",
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            "entityId"
                        )
                    )
                )

                /*
                 * CRITICAL:
                 * Never guess ownership of old data.
                 */
                val userIdIndex =
                    cursor.getColumnIndexOrThrow(
                        "user_id"
                    )

                assertTrue(
                    "Legacy queue ownership must remain NULL.",
                    cursor.isNull(
                        userIdIndex
                    )
                )
            }

        /*
         * Confirm the new column actually exists.
         */
        migrated
            .query(
                "PRAGMA table_info(`pending_sync`)"
            )
            .use { cursor ->

                val nameIndex =
                    cursor.getColumnIndexOrThrow(
                        "name"
                    )

                var found =
                    false

                while (
                    cursor.moveToNext()
                ) {

                    if (
                        cursor.getString(
                            nameIndex
                        ) == "user_id"
                    ) {

                        found =
                            true

                        break
                    }
                }

                assertTrue(
                    "user_id column must exist after migration.",
                    found
                )
            }

        migrated.close()
    }

    companion object {

        private const val TEST_DATABASE =
            "migration-30-31-test"
    }
}
