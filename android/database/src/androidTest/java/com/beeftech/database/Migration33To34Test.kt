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
class Migration33To34Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry
                .getInstrumentation(),

            BeefTechDatabase::class.java
                .canonicalName!!,

            FrameworkSQLiteOpenHelperFactory()
        )

    private fun insertLegacyMortality(
        db: SupportSQLiteDatabase
    ) {

        db.execSQL(
            """
            INSERT INTO mortalities (
                animalId,
                causeOfDeath,
                responsibleWorker,
                timestamp,
                record_guid
            )
            VALUES (
                'LEGACY-ANIMAL',
                'Bloat',
                'jvdm',
                1700000000000,
                'LEGACY-GUID'
            )
            """.trimIndent()
        )
    }

    @Test
    fun migration33To34_keepsMortalitiesAndMarksThemPending() {

        helper
            .createDatabase(
                TEST_DATABASE,
                33
            )
            .apply {

                insertLegacyMortality(this)

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                34,
                true,
                BeefTechDatabase.MIGRATION_33_34
            )

        migrated
            .query(
                """
                SELECT animalId, causeOfDeath, record_guid, sync_status, synced_at
                FROM mortalities
                WHERE record_guid = 'LEGACY-GUID'
                """.trimIndent()
            )
            .use { cursor ->

                assertTrue(
                    "Legacy mortality must survive migration.",
                    cursor.moveToFirst()
                )

                assertEquals("LEGACY-ANIMAL", cursor.getString(0))
                assertEquals("Bloat", cursor.getString(1))

                assertEquals(
                    "A mortality that never synced must start as PENDING.",
                    "PENDING",
                    cursor.getString(3)
                )

                assertTrue(
                    "synced_at must be NULL for legacy mortalities.",
                    cursor.isNull(4)
                )
            }

        migrated.close()
    }

    @Test
    fun migration33To34_isIdempotentWhenColumnsAlreadyExist() {

        helper
            .createDatabase(
                TEST_DATABASE_IDEMPOTENT,
                33
            )
            .apply {

                insertLegacyMortality(this)

                /*
                 * Simulate a partially applied migration.
                 */
                execSQL(
                    "ALTER TABLE `mortalities` ADD COLUMN `sync_status` TEXT NOT NULL DEFAULT 'PENDING'"
                )

                close()
            }

        val migrated: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(
                TEST_DATABASE_IDEMPOTENT,
                34,
                true,
                BeefTechDatabase.MIGRATION_33_34
            )

        migrated
            .query("SELECT COUNT(*) FROM mortalities")
            .use { cursor ->

                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "The legacy mortality must still be there.",
                    1,
                    cursor.getInt(0)
                )
            }

        migrated.close()
    }

    companion object {

        private const val TEST_DATABASE =
            "migration-33-34-test"

        private const val TEST_DATABASE_IDEMPOTENT =
            "migration-33-34-idempotent-test"
    }
}
