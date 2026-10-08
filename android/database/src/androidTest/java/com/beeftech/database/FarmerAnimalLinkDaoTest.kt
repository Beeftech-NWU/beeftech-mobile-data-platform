package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.FarmerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Device tests for the offline farmer-to-animal association. */
@RunWith(AndroidJUnit4::class)
class FarmerAnimalLinkDaoTest {
    private lateinit var context: Context
    private var database: BeefTechDatabase? = null

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("beeftech.db")
    }

    private suspend fun openSeededDatabase(): BeefTechDatabase {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = ByteArray(32) { index -> (index + 1).toByte() }
        )
        assertTrue("Database must open", result is DatabaseResult.Success)
        val db = (result as DatabaseResult.Success).database
        database = db
        for (farmerId in listOf("FARMER-A", "FARMER-B")) {
            db.farmerDao().insertFarmer(
                FarmerEntity(
                    farmer_id = farmerId,
                    client_code = null,
                    organisation_name = farmerId,
                    vat_number = null,
                    email_address = null,
                    gps_latitude = null,
                    gps_longitude = null
                )
            )
        }
        db.animalDao().insert(
            Animal(
                animalId = "ANIMAL-LINK-1", birthdate = 1700000000000L,
                breed = "Angus", gpsLat = -26.1, gpsLng = 27.9,
                captureAt = 1700000000000L, deviceId = "test-device",
                recordGuid = "link-animal-record-1"
            )
        )
        return db
    }

    @Test fun assignmentIsIdempotentAndVisibleForFarmer() = runBlocking {
        val db = openSeededDatabase()
        val dao = db.farmerAnimalLinkDao()
        assertTrue(dao.assign("FARMER-A", "ANIMAL-LINK-1"))
        assertFalse(dao.assign("FARMER-A", "ANIMAL-LINK-1"))
        assertEquals(1, dao.activeForFarmer("FARMER-A").size)
        assertEquals("FARMER-A", dao.activeForAnimal("ANIMAL-LINK-1")?.farmerId)
        assertEquals("PENDING", dao.activeForAnimal("ANIMAL-LINK-1")?.syncStatus)
    }

    @Test fun reassignmentEndsPreviousOwnership() = runBlocking {
        val db = openSeededDatabase()
        val dao = db.farmerAnimalLinkDao()
        assertTrue(dao.assign("FARMER-A", "ANIMAL-LINK-1"))
        assertTrue(dao.assign("FARMER-B", "ANIMAL-LINK-1"))
        assertTrue(dao.activeForFarmer("FARMER-A").isEmpty())
        assertEquals(1, dao.activeForFarmer("FARMER-B").size)
        assertEquals(1, dao.allActive().size)
    }

    @Test fun assignmentSurvivesDatabaseReopen() = runBlocking {
        val db = openSeededDatabase()
        assertTrue(db.farmerAnimalLinkDao().assign("FARMER-A", "ANIMAL-LINK-1"))
        db.close()
        database = null
        val reopened = DatabaseFactory.create(
            context = context,
            passphrase = ByteArray(32) { index -> (index + 1).toByte() }
        )
        assertTrue("Database must reopen", reopened is DatabaseResult.Success)
        database = (reopened as DatabaseResult.Success).database
        assertEquals("FARMER-A", database!!.farmerAnimalLinkDao().activeForAnimal("ANIMAL-LINK-1")?.farmerId)
    }

    @After fun tearDown() {
        database?.close()
        database = null
        context.deleteDatabase("beeftech.db")
    }
}
