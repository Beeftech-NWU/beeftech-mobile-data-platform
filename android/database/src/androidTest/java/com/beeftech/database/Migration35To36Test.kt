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
class Migration35To36Test {

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
     * Rows in the tables 36 must leave alone. Foreign keys are not enforced on a
     * MigrationTestHelper database, so a cost needs no animal parent row.
     */
    private fun insertExistingData(
        db: SupportSQLiteDatabase
    ) {

        db.execSQL("INSERT INTO cost_types (code, display_name, sort_order, is_active) VALUES ('TRANSPORT', 'Transport', 0, 1)")
        db.execSQL("INSERT INTO cost_types (code, display_name, sort_order, is_active) VALUES ('FEED', 'Feed / Ration', 5, 0)")
        db.execSQL("INSERT INTO diseases (diseaseId, name) VALUES ('Rabies', 'Rabies')")
        db.execSQL("INSERT INTO diseases (diseaseId, name) VALUES ('Anthrax', 'Anthrax')")
        db.execSQL(
            """
            INSERT INTO animal_costs (
                animalId, costType, amount, description, gpsLat, gpsLng, timestamp, record_guid
            )
            VALUES ('LEGACY-ANIMAL', 'TRANSPORT', 250.0, 'Legacy transport', -25.0, 28.0, 1700000000000, 'LEGACY-GUID')
            """.trimIndent()
        )
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM `$table`").use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test
    fun migration35To36_createsEmptyTablesAndLeavesEveryExistingRowAlone() {

        helper
            .createDatabase(
                TEST_DATABASE,
                35
            )
            .apply {

                insertExistingData(this)

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                36,
                true,
                BeefTechDatabase.MIGRATION_35_36
            )

        assertEquals(0, count(migrated, "reference_items"))
        assertEquals(0, count(migrated, "device_config"))

        assertEquals(2, count(migrated, "cost_types"))
        assertEquals(2, count(migrated, "diseases"))
        assertEquals(1, count(migrated, "animal_costs"))

        migrated
            .query("SELECT display_name, sort_order, is_active FROM cost_types WHERE code = 'FEED'")
            .use { cursor ->

                assertTrue(cursor.moveToFirst())
                assertEquals("Feed / Ration", cursor.getString(0))
                assertEquals(5, cursor.getInt(1))
                assertEquals("A switched-off cost type stays switched off.", 0, cursor.getInt(2))
            }

        /* The new tables are usable straight away. */
        migrated.execSQL(
            "INSERT INTO reference_items (kind, item_key, display_name, active, sort_order, server_id, updated_at) " +
                "VALUES ('diseases', 'Rabies', 'Rabies', 1, 0, 12, 5)"
        )
        migrated.execSQL("INSERT INTO device_config (config_key, value, updated_at) VALUES ('reference_data_version', '3', 5)")
        assertEquals(1, count(migrated, "reference_items"))
        assertEquals(1, count(migrated, "device_config"))

        migrated.close()
    }

    @Test
    fun migration35To36_isIdempotentWhenTheTablesAlreadyExist() {

        helper
            .createDatabase(
                TEST_DATABASE_IDEMPOTENT,
                35
            )
            .apply {

                insertExistingData(this)

                /* A half-applied earlier attempt: the tables exist and one holds a row. */
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `reference_items` (`kind` TEXT NOT NULL, `item_key` TEXT NOT NULL, `display_name` TEXT NOT NULL, `active` INTEGER NOT NULL DEFAULT 1, `sort_order` INTEGER NOT NULL DEFAULT 0, `server_id` INTEGER, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`kind`, `item_key`))"
                )
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `device_config` (`config_key` TEXT NOT NULL, `value` TEXT NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`config_key`))"
                )
                execSQL(
                    "INSERT INTO reference_items (kind, item_key, display_name, active, sort_order, server_id, updated_at) " +
                        "VALUES ('treatment_types', 'Vaccination', 'Vaccination', 1, 0, 9, 5)"
                )

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE_IDEMPOTENT,
                36,
                true,
                BeefTechDatabase.MIGRATION_35_36
            )

        assertEquals("The existing row must survive.", 1, count(migrated, "reference_items"))
        assertEquals(2, count(migrated, "diseases"))
        assertEquals(1, count(migrated, "animal_costs"))

        migrated.close()
    }

    companion object {

        private const val TEST_DATABASE =
            "migration-35-36-test"

        private const val TEST_DATABASE_IDEMPOTENT =
            "migration-35-36-idempotent-test"
    }
}
