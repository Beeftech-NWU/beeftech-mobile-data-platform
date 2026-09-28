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
import java.util.UUID

/**
 * R3, bullet 2: nine tables that had no `record_guid` gain one, backfilled
 * with a unique, non-empty UUID on every existing row.
 */
@RunWith(AndroidJUnit4::class)
class Migration19To20Test {

    private val TEST_DB = "migration-test-19-20"

    private val tablesAndSeedSql = listOf(
        "animal_movements" to """
            INSERT INTO `animal_movements`
                (`movement_id`, `animal_id`, `destination_farm_id`, `destination_pen_id`, `movement_date`)
            VALUES (?, ?, 'FARM-A', 'PEN-1', '1000')
        """.trimIndent(),
        "animal_weights" to """
            INSERT INTO `animal_weights` (`weight_id`, `animal_id`, `weight_kg`, `weigh_date`)
            VALUES (?, ?, 250.0, '2026-01-01')
        """.trimIndent(),
        "animal_identifiers" to """
            INSERT INTO `animal_identifiers`
                (`identifier_id`, `animal_id`, `identifier_type`, `identifier_value`)
            VALUES (?, ?, 'TAG', 'TAG-001')
        """.trimIndent(),
        "animal_media" to """
            INSERT INTO `animal_media` (`media_id`, `animal_id`, `file_path`, `media_type`, `created_at`)
            VALUES (?, ?, '/tmp/a.jpg', 'PHOTO', '2026-01-01')
        """.trimIndent(),
        "animal_ownerships" to """
            INSERT INTO `animal_ownerships`
                (`ownership_id`, `animal_id`, `owner_name`, `ownership_percentage`, `start_date`)
            VALUES (?, ?, 'Farmer A', 100.0, '2026-01-01')
        """.trimIndent(),
        "animal_purchases" to """
            INSERT INTO `animal_purchases`
                (`purchase_id`, `animal_id`, `purchase_price`, `purchase_date`, `seller_name`)
            VALUES (?, ?, 1000.0, '2026-01-01', 'Seller A')
        """.trimIndent(),
        "farmer_addresses" to """
            INSERT INTO `farmer_addresses` (`address_id`, `farmer_id`)
            VALUES (?, ?)
        """.trimIndent(),
        "feed_crib_readings" to """
            INSERT INTO `feed_crib_readings` (`id`, `cribId`)
            VALUES (?, ?)
        """.trimIndent(),
        "feed_crib_reading_values" to """
            INSERT INTO `feed_crib_reading_values` (`id`, `readingId`, `value`)
            VALUES (?, ?, 5.0)
        """.trimIndent()
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration19To20_addsRecordGuid_uniqueAndNonEmpty_onEveryTable() {
        val animalId = UUID.randomUUID().toString()

        helper.createDatabase(TEST_DB, 19).use { db ->
            db.execSQL(
                "INSERT INTO `animals` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animalId, 1000L, "Bonsmara", 0.0, 0.0, 1000L, "device1", UUID.randomUUID().toString(), "PENDING")
            )

            // Two rows per table (id1, id2), so uniqueness is actually exercised.
            tablesAndSeedSql.forEach { (_, sql) ->
                db.execSQL(sql, arrayOf<Any>(UUID.randomUUID().toString(), animalId))
                db.execSQL(sql, arrayOf<Any>(UUID.randomUUID().toString(), animalId))
            }
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 20, true, BeefTechDatabase.MIGRATION_19_20)

        tablesAndSeedSql.forEach { (table, _) ->
            var rowCount = 0
            val guids = mutableSetOf<String>()
            db.query("SELECT `record_guid` FROM `$table`").use { c ->
                while (c.moveToNext()) {
                    rowCount++
                    val guid = c.getString(0)
                    assertTrue("$table.record_guid must not be blank", guid.isNotBlank())
                    guids.add(guid)
                }
            }
            assertEquals("$table must have 2 rows", 2, rowCount)
            assertEquals("every $table.record_guid must be unique", 2, guids.size)

            db.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_${table}_record_guid'"
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("$table must have its unique record_guid index", 1, c.getInt(0))
            }
        }
    }
}
