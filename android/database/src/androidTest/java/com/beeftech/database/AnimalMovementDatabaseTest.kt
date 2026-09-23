package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalMovementEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalMovementDatabaseTest {

    private lateinit var context: Context
    private var database: BeefTechDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun insertAnimalMovement_persistsRecord() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database

        val animal = Animal(
            animalId = "MOVE-001",
            birthdate = System.currentTimeMillis(),
            breed = "Bonsmara",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordguid = "guid-move-001"
        )
        database!!.animalDao().insert(animal)

        val movement = AnimalMovementEntity(
            animalId = "MOVE-001",
            destinationFarmId = "Farm A",
            destinationPenId = "Pen 1",
            movementDate = "2026-09-18",
            notes = "Worker 1"
        )

        database!!.animalMovementDao().insert(movement)

        val records = database!!.animalMovementDao().getByAnimalId("MOVE-001")
        assertEquals(1, records.size)
        assertEquals("Farm A", records[0].destinationFarmId)
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}
