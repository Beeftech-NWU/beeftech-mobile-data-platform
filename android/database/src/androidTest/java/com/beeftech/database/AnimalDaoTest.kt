package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.IdentifierTypes
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalDaoTest {

    private lateinit var context: Context
    private var database: BeefTechDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun insertAnimal_allowsReadBack() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalDao()

        val animal = Animal(
            animalId = "ANIMAL-001",
            birthdate = 1700000000000L,
            breed = "Angus",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = 1700000000000L,
            deviceId = "test-device",
            recordGuid = "record-001"
        )

        dao.insert(animal)

        val resultAnimal = dao.getById("ANIMAL-001")

        assertEquals("ANIMAL-001", resultAnimal?.animalId)
        assertEquals("Angus", resultAnimal?.breed)
    }

    @Test
    fun getByTagNumber_returnsMatchingAnimal() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalDao()
        val identifierDao = database!!.animalIdentifierDao()

        dao.insert(
            Animal(
                animalId = "ANIMAL-002",
                birthdate = 1700000000000L,
                breed = "Angus",
                gpsLat = -26.1,
                gpsLng = 27.9,
                captureAt = 1700000000000L,
                deviceId = "test-device",
                recordGuid = "record-002"
            )
        )
        identifierDao.insertIdentifier(
            AnimalIdentifierEntity(
                animalId = "ANIMAL-002",
                identifierType = IdentifierTypes.TAG,
                identifierValue = "Blu0000064"
            )
        )

        val resultAnimal = dao.getByTagNumber("Blu0000064")

        assertEquals("ANIMAL-002", resultAnimal?.animalId)
        assertEquals("Angus", resultAnimal?.breed)
    }

    @Test
    fun getByReferenceNumber_returnsMatchingAnimal() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalDao()
        val identifierDao = database!!.animalIdentifierDao()

        dao.insert(
            Animal(
                animalId = "ANIMAL-003",
                birthdate = 1700000000000L,
                breed = "Angus",
                gpsLat = -26.1,
                gpsLng = 27.9,
                captureAt = 1700000000000L,
                deviceId = "test-device",
                recordGuid = "record-003"
            )
        )
        identifierDao.insertIdentifier(
            AnimalIdentifierEntity(
                animalId = "ANIMAL-003",
                identifierType = IdentifierTypes.REFERENCE,
                identifierValue = "B64"
            )
        )

        val resultAnimal = dao.getByReferenceNumber("B64")

        assertEquals("ANIMAL-003", resultAnimal?.animalId)
        assertEquals("Angus", resultAnimal?.breed)
    }

    @Test
    fun getOffspring_returnsAnimalsWithMatchingParentId() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalDao()
        val parent = Animal(
            animalId = "ANIMAL-001",
            birthdate = 1600000000000L,
            breed = "Angus",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = 1700000000000L,
            deviceId = "test-device",
            recordGuid = "record-parent"
        )

        dao.insert(parent)

        val parentId = parent.animalId

        dao.insert(
            Animal(
                animalId = "ANIMAL-004",
                birthdate = 1700000000000L,
                breed = "Angus",
                damId = parentId,
                gpsLat = -26.1,
                gpsLng = 27.9,
                captureAt = 1700000000000L,
                deviceId = "test-device",
                recordGuid = "record-004"
            )
        )

        dao.insert(
            Animal(
                animalId = "ANIMAL-005",
                birthdate = 1700000000000L,
                breed = "Bonsmara",
                gpsLat = -26.1,
                gpsLng = 27.9,
                captureAt = 1700000000000L,
                deviceId = "test-device",
                recordGuid = "record-005"
            )
        )

        val offspring = dao.getOffspring(parentId)

        assertEquals(1, offspring.size)
        assertEquals("ANIMAL-004", offspring[0].animalId)
        assertEquals(parentId, offspring[0].damId)
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    @After
    fun tearDown() {
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}
