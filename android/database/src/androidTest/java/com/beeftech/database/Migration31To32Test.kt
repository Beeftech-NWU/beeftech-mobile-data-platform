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
class Migration31To32Test {

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
    fun migration31To32_preservesFarmerAndAddressAndAddsNullColumns() {

        helper
            .createDatabase(
                TEST_DATABASE,
                31
            )
            .apply {

                execSQL(
                    """
                    INSERT INTO farmers (
                        farmer_id,
                        client_code,
                        organisation_name,
                        sync_status,
                        record_guid
                    )
                    VALUES (
                        'LEGACY-FARMER',
                        'KAR001',
                        'Karoo Vryburg',
                        'PENDING',
                        'guid-farmer'
                    )
                    """.trimIndent()
                )

                execSQL(
                    """
                    INSERT INTO farmer_addresses (
                        address_id,
                        farmer_id,
                        address_type,
                        address_line_1,
                        record_guid
                    )
                    VALUES (
                        'LEGACY-ADDRESS',
                        'LEGACY-FARMER',
                        'PRIMARY',
                        'Mark Str 93',
                        'guid-address'
                    )
                    """.trimIndent()
                )

                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                32,
                true,
                BeefTechDatabase.MIGRATION_31_32
            )

        migrated
            .query(
                """
                SELECT
                    client_code,
                    co_reg_id_no,
                    land_ownership,
                    fa_code_rmis,
                    gln_number
                FROM farmers
                WHERE farmer_id = 'LEGACY-FARMER'
                """.trimIndent()
            )
            .use { cursor ->

                assertTrue(
                    "Legacy farmer must survive migration.",
                    cursor.moveToFirst()
                )

                assertEquals(
                    "KAR001",
                    cursor.getString(0)
                )

                (1..4).forEach {
                    assertTrue(
                        "New farmer columns must be NULL for legacy rows.",
                        cursor.isNull(it)
                    )
                }
            }

        migrated
            .query(
                """
                SELECT
                    address_line_1,
                    street_code,
                    postal_address,
                    country
                FROM farmer_addresses
                WHERE address_id = 'LEGACY-ADDRESS'
                """.trimIndent()
            )
            .use { cursor ->

                assertTrue(
                    "Legacy address must survive migration.",
                    cursor.moveToFirst()
                )

                assertEquals(
                    "Mark Str 93",
                    cursor.getString(0)
                )

                (1..3).forEach {
                    assertTrue(
                        "New address columns must be NULL for legacy rows.",
                        cursor.isNull(it)
                    )
                }
            }

        migrated.close()
    }

    @Test
    fun migration31To32_isIdempotentWhenColumnsAlreadyExist() {

        helper
            .createDatabase(
                TEST_DATABASE_IDEMPOTENT,
                31
            )
            .apply {

                /*
                 * Simulate a partially applied migration.
                 */
                execSQL(
                    "ALTER TABLE `farmers` ADD COLUMN `co_reg_id_no` TEXT"
                )

                close()
            }

        val migrated: SupportSQLiteDatabase =
            helper.runMigrationsAndValidate(
                TEST_DATABASE_IDEMPOTENT,
                32,
                true,
                BeefTechDatabase.MIGRATION_31_32
            )

        migrated.close()
    }

    companion object {

        private const val TEST_DATABASE =
            "migration-31-32-test"

        private const val TEST_DATABASE_IDEMPOTENT =
            "migration-31-32-idempotent-test"
    }
}
