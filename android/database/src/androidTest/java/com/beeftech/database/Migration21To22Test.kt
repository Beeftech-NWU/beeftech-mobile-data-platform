package com.beeftech.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * R3, bullet 5: nine TEXT date columns across seven tables become epoch
 * millisecond INTEGER. Seeds a mix of the three formats these columns
 * actually hold in the wild -- a numeric millisecond string
 * (`animal_movements.movement_date`), an ISO date `yyyy-MM-dd`
 * (`CalfRegistrationMappers.isoDate`'s old output), and an ISO datetime
 * `yyyy-MM-dd'T'HH:mm:ss` (an existing test fixture shape) -- to prove all
 * three parse to the same correct millisecond value, and that a blank
 * nullable date becomes SQL NULL rather than a fabricated one.
 *
 * The expected values are computed with the same unqualified
 * `SimpleDateFormat` the migration uses (device-default time zone, matching
 * how `CalfRegistrationMappers.isoDate` originally wrote these strings), so
 * this test passes regardless of which time zone it runs in.
 */
@RunWith(AndroidJUnit4::class)
class Migration21To22Test {

    private val TEST_DB = "migration-test-21-22"

    private val expectedDateOnlyMillis =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-09-18")!!.time
    private val expectedDateTimeMillis =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse("2026-09-18T10:00:00")!!.time

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeefTechDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration21To22_convertsDateColumns_handlingNumericAndIsoStrings() {
        val animalId = UUID.randomUUID().toString()

        helper.createDatabase(TEST_DB, 21).use { db ->
            db.execSQL(
                "INSERT INTO `animals` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(animalId, 1000L, "Bonsmara", 0.0, 0.0, 1000L, "device1", UUID.randomUUID().toString(), "PENDING")
            )

            // movement_date: a numeric millisecond string (System.currentTimeMillis().toString()).
            db.execSQL(
                "INSERT INTO `animal_movements` (`movement_id`, `animal_id`, `destination_farm_id`, `destination_pen_id`, `movement_date`, `record_guid`) VALUES (?, ?, 'FARM-A', 'PEN-1', ?, ?)",
                arrayOf<Any>(UUID.randomUUID().toString(), animalId, expectedDateOnlyMillis.toString(), UUID.randomUUID().toString())
            )

            // valid_from: an ISO date. valid_to: blank -- must become NULL, not a fabricated date.
            db.execSQL(
                "INSERT INTO `animal_identifiers` (`identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `valid_to`, `record_guid`) VALUES (?, ?, 'TAG', 'TAG-001', ?, NULL, ?)",
                arrayOf<Any>(UUID.randomUUID().toString(), animalId, "2026-09-18", UUID.randomUUID().toString())
            )

            // created_at: an ISO datetime.
            db.execSQL(
                "INSERT INTO `animal_media` (`media_id`, `animal_id`, `file_path`, `media_type`, `created_at`, `record_guid`) VALUES (?, ?, '/tmp/a.jpg', 'PHOTO', ?, ?)",
                arrayOf<Any>(UUID.randomUUID().toString(), animalId, "2026-09-18T10:00:00", UUID.randomUUID().toString())
            )

            // weigh_date: an ISO date.
            db.execSQL(
                "INSERT INTO `animal_weights` (`weight_id`, `animal_id`, `weight_kg`, `weigh_date`, `record_guid`) VALUES (?, ?, 250.0, ?, ?)",
                arrayOf<Any>(UUID.randomUUID().toString(), animalId, "2026-09-18", UUID.randomUUID().toString())
            )

            // start_date: an ISO date. end_date: blank -- must become NULL.
            db.execSQL(
                "INSERT INTO `animal_ownerships` (`ownership_id`, `animal_id`, `owner_name`, `ownership_percentage`, `start_date`, `end_date`, `record_guid`) VALUES (?, ?, 'Farmer A', 100.0, ?, NULL, ?)",
                arrayOf<Any>(UUID.randomUUID().toString(), animalId, "2026-09-18", UUID.randomUUID().toString())
            )

            // purchase_date: a numeric millisecond string.
            db.execSQL(
                "INSERT INTO `animal_purchases` (`purchase_id`, `animal_id`, `purchase_price`, `purchase_date`, `seller_name`, `record_guid`) VALUES (?, ?, 1000.0, ?, 'Seller A', ?)",
                arrayOf<Any>(UUID.randomUUID().toString(), animalId, expectedDateOnlyMillis.toString(), UUID.randomUUID().toString())
            )

            // registration_date: an ISO date.
            db.execSQL(
                "INSERT INTO `calf_registrations` (`registration_id`, `registered_animal_id`, `registration_date`, `record_guid`) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(UUID.randomUUID().toString(), animalId, "2026-09-18", UUID.randomUUID().toString())
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 22, true, BeefTechDatabase.MIGRATION_21_22)

        db.query("SELECT `movement_date` FROM `animal_movements`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedDateOnlyMillis, c.getLong(0))
        }

        db.query("SELECT `valid_from`, `valid_to` FROM `animal_identifiers`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedDateOnlyMillis, c.getLong(0))
            assertNull("a blank valid_to must become NULL, not a fabricated date", c.getString(1))
        }

        db.query("SELECT `created_at` FROM `animal_media`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedDateTimeMillis, c.getLong(0))
        }

        db.query("SELECT `weigh_date` FROM `animal_weights`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedDateOnlyMillis, c.getLong(0))
        }

        db.query("SELECT `start_date`, `end_date` FROM `animal_ownerships`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedDateOnlyMillis, c.getLong(0))
            assertNull("a blank end_date must become NULL, not a fabricated date", c.getString(1))
        }

        db.query("SELECT `purchase_date` FROM `animal_purchases`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedDateOnlyMillis, c.getLong(0))
        }

        db.query("SELECT `registration_date` FROM `calf_registrations`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedDateOnlyMillis, c.getLong(0))
        }
    }
}
