package com.beeftech.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AnimalCostMigration13To14Test {

    private lateinit var context: Context
    private val databaseName = "animal_cost_migration_test.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun testMigration13To14_rebuildsAnimalCosts_andSeedsCostTypes_andBackfillsTreatments() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `animal_costs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, `costType` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL DEFAULT '', `gpsLat` REAL NOT NULL DEFAULT 0.0, `gpsLng` REAL NOT NULL DEFAULT 0.0, `timestamp` INTEGER NOT NULL, `record_guid` TEXT NOT NULL DEFAULT '')")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_costs_record_guid` ON `animal_costs` (`record_guid`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `treatments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, `disease` TEXT NOT NULL, `treatmentName` TEXT NOT NULL, `batchNumber` TEXT NOT NULL, `volumeUsed` TEXT NOT NULL, `cost` REAL NOT NULL, `gpsLat` REAL NOT NULL DEFAULT 0.0, `gpsLng` REAL NOT NULL DEFAULT 0.0, `timestamp` INTEGER NOT NULL, `deviceId` TEXT NOT NULL DEFAULT '', `recordguid` TEXT NOT NULL DEFAULT '', `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', `syncedAt` INTEGER)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_treatments_recordguid` ON `treatments` (`recordguid`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helperFactory.create(config)
        val db = openHelper.writableDatabase

        val guidCost1 = UUID.randomUUID().toString()
        val guidCost2 = UUID.randomUUID().toString()
        val guidTreatmentWithCost = UUID.randomUUID().toString()
        val guidTreatmentZeroCost = UUID.randomUUID().toString()

        db.execSQL(
            "INSERT INTO `animal_costs` (`animalId`, `costType`, `amount`, `description`, `gpsLat`, `gpsLng`, `timestamp`, `record_guid`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("A-001", "TRANSPORT", 200.0, "Transport cost", -25.0, 28.0, 1000L, guidCost1)
        )
        db.execSQL(
            "INSERT INTO `animal_costs` (`animalId`, `costType`, `amount`, `description`, `gpsLat`, `gpsLng`, `timestamp`, `record_guid`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("A-001", "Misc", 50.0, "Misc cost", -25.0, 28.0, 2000L, guidCost2)
        )

        db.execSQL(
            "INSERT INTO `treatments` (`animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`, `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `recordguid`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("A-001", "Disease A", "Treatment 1", "B01", "10ml", 100.0, -25.0, 28.0, 3000L, "dev1", guidTreatmentWithCost)
        )
        db.execSQL(
            "INSERT INTO `treatments` (`animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`, `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `recordguid`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("A-001", "Disease B", "Treatment 2", "B02", "5ml", 0.0, -25.0, 28.0, 4000L, "dev1", guidTreatmentZeroCost)
        )

        db.close()

        val configForMigration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 13 && newVersion == 14) {
                        BeefTechDatabase.MIGRATION_13_14.migrate(db)
                    }
                }
            })
            .build()

        val migratedDb = helperFactory.create(configForMigration).writableDatabase

        // Assert original cost rows remain with record_guids unchanged
        var foundCost1 = false
        var foundCost2 = false
        var foundDerivedTreatment = false
        var foundZeroCostDerived = false

        migratedDb.query("SELECT `costType`, `amount`, `source_entity`, `source_record_id`, `record_guid` FROM `animal_costs`").use { c ->
            while (c.moveToNext()) {
                val costType = c.getString(0)
                val amount = c.getDouble(1)
                val sourceEntity = c.getString(2)
                val sourceRecordId = c.getString(3)
                val recordGuid = c.getString(4)

                if (recordGuid == guidCost1) {
                    assertEquals("TRANSPORT", costType)
                    assertEquals(200.0, amount, 0.001)
                    foundCost1 = true
                }
                if (recordGuid == guidCost2) {
                    assertEquals("Misc", costType)
                    assertEquals(50.0, amount, 0.001)
                    foundCost2 = true
                }
                if (sourceEntity == "TREATMENT" && sourceRecordId == guidTreatmentWithCost) {
                    assertEquals("TREATMENT", costType)
                    assertEquals(100.0, amount, 0.001)
                    foundDerivedTreatment = true
                }
                if (sourceEntity == "TREATMENT" && sourceRecordId == guidTreatmentZeroCost) {
                    foundZeroCostDerived = true
                }
            }
        }

        assertTrue("Original cost 1 should remain", foundCost1)
        assertTrue("Original cost 2 (Misc) should remain", foundCost2)
        assertTrue("Derived treatment cost should exist for treatment with cost > 0", foundDerivedTreatment)
        assertFalse("Derived treatment cost should NOT exist for treatment with cost == 0", foundZeroCostDerived)

        // Assert cost_types contains seed codes and "Misc"
        val costTypeCodes = mutableListOf<String>()
        migratedDb.query("SELECT `code` FROM `cost_types`").use { c ->
            while (c.moveToNext()) {
                costTypeCodes.add(c.getString(0))
            }
        }

        CostTypeSeed.TYPES.forEach { (code, _) ->
            assertTrue("cost_types must contain seed code $code", costTypeCodes.contains(code))
        }
        assertTrue("cost_types must contain preserved unexpected code 'Misc'", costTypeCodes.contains("Misc"))

        // Assert foreign key check returns no rows
        migratedDb.query("PRAGMA foreign_key_check").use { c ->
            assertEquals("PRAGMA foreign_key_check should find 0 violations", 0, c.count)
        }

        migratedDb.close()
    }
}
