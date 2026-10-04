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
class Migration34To35Test {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry
                .getInstrumentation(),

            BeefTechDatabase::class.java
                .canonicalName!!,

            FrameworkSQLiteOpenHelperFactory()
        )

    /*
     * Foreign keys are not enforced on a MigrationTestHelper database, so
     * the legacy cost needs no animal or cost_types parent row.
     */
    private fun insertLegacyCost(
        db: SupportSQLiteDatabase
    ) {

        db.execSQL(
            """
            INSERT INTO animal_costs (
                animalId,
                costType,
                amount,
                description,
                gpsLat,
                gpsLng,
                timestamp,
                record_guid
            )
            VALUES (
                'LEGACY-ANIMAL',
                'TRANSPORT',
                250.0,
                'Legacy transport',
                -25.0,
                28.0,
                1700000000000,
                'LEGACY-GUID'
            )
            """.trimIndent()
        )
    }

    @Test
    fun migration34To35_keepsCostsAndMarksThemPending() {

        helper
            .createDatabase(
                TEST_DATABASE,
                34
            )
            .apply {

                insertLegacyCost(this)

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                35,
                true,
                BeefTechDatabase.MIGRATION_34_35
            )

        migrated
            .query(
                """
                SELECT animalId, costType, record_guid, sync_status, synced_at
                FROM animal_costs
                WHERE record_guid = 'LEGACY-GUID'
                """.trimIndent()
            )
            .use { cursor ->

                assertTrue(
                    "Legacy cost must survive migration.",
                    cursor.moveToFirst()
                )

                assertEquals("LEGACY-ANIMAL", cursor.getString(0))
                assertEquals("TRANSPORT", cursor.getString(1))

                assertEquals(
                    "A cost that never synced must start as PENDING.",
                    "PENDING",
                    cursor.getString(3)
                )

                assertTrue(
                    "synced_at must be NULL for legacy animal_costs.",
                    cursor.isNull(4)
                )
            }

        migrated.close()
    }

    @Test
    fun migration34To35_isIdempotentWhenColumnsAlreadyExist() {

        helper
            .createDatabase(
                TEST_DATABASE_IDEMPOTENT,
                34
            )
            .apply {

                insertLegacyCost(this)

                /*
                 * Simulate a partially applied migration.
                 */
                execSQL(
                    "ALTER TABLE `animal_costs` ADD COLUMN `sync_status` TEXT NOT NULL DEFAULT 'PENDING'"
                )

                close()
            }

        val migrated: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(
                TEST_DATABASE_IDEMPOTENT,
                35,
                true,
                BeefTechDatabase.MIGRATION_34_35
            )

        migrated
            .query("SELECT COUNT(*) FROM animal_costs")
            .use { cursor ->

                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "The legacy cost must still be there.",
                    1,
                    cursor.getInt(0)
                )
            }

        migrated.close()
    }

    companion object {

        private const val TEST_DATABASE =
            "migration-34-35-test"

        private const val TEST_DATABASE_IDEMPOTENT =
            "migration-34-35-idempotent-test"
    }
}
