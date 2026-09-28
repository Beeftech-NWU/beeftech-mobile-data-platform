package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.dao.DuplicateTagException
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.CalfRegistrationEntity
import com.beeftech.database.entity.IdentifierTypes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CalfRegistrationDaoTest {

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
    fun registerCalf_registersParentsAndCalf_andRejectsDuplicateTag() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.calfRegistrationDao()

        // 1. Register a dam and a sire
        val damUuid = UUID.randomUUID().toString()
        val sireUuid = UUID.randomUUID().toString()
        val damTag = "Blu0000011"
        val sireTag = "Blu0000902"

        val dam = Animal(
            animalId = damUuid,
            tagNumber = damTag,
            birthdate = System.currentTimeMillis(),
            breed = "Bonsmara",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordGuid = UUID.randomUUID().toString()
        )
        val damIdentifiers = listOf(
            AnimalIdentifierEntity(animalId = damUuid, identifierType = IdentifierTypes.TAG, identifierValue = damTag)
        )
        val damRegistration = CalfRegistrationEntity(registeredAnimalId = damUuid, registrationDate = "2024-01-01")
        dao.registerCalf(dam, damIdentifiers, emptyList(), damRegistration)

        val sire = Animal(
            animalId = sireUuid,
            tagNumber = sireTag,
            birthdate = System.currentTimeMillis(),
            breed = "Bonsmara",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordGuid = UUID.randomUUID().toString()
        )
        val sireIdentifiers = listOf(
            AnimalIdentifierEntity(animalId = sireUuid, identifierType = IdentifierTypes.TAG, identifierValue = sireTag)
        )
        val sireRegistration = CalfRegistrationEntity(registeredAnimalId = sireUuid, registrationDate = "2024-01-01")
        dao.registerCalf(sire, sireIdentifiers, emptyList(), sireRegistration)

        // 2. Register calf Blu1234567 with dam and sire
        val calfUuid = UUID.randomUUID().toString()
        val calfTag = "Blu1234567"
        val calf = Animal(
            animalId = calfUuid,
            tagNumber = calfTag,
            birthdate = System.currentTimeMillis(),
            breed = "Bonsmara",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordGuid = UUID.randomUUID().toString()
        )
        val calfIdentifiers = listOf(
            AnimalIdentifierEntity(animalId = calfUuid, identifierType = IdentifierTypes.TAG, identifierValue = calfTag)
        )
        val calfRegistration = CalfRegistrationEntity(
            registeredAnimalId = calfUuid,
            damId = damUuid,
            sireId = sireUuid,
            registrationDate = "2026-09-18"
        )
        dao.registerCalf(calf, calfIdentifiers, emptyList(), calfRegistration)

        // 3. Assert: 3 animals, 3 TAG identifiers, 3 registrations (dam, sire, calf)
        assertEquals(3, count("animals"))
        assertEquals(3, count("animal_identifiers", "identifier_type = 'TAG'"))
        assertEquals(1, count("calf_registrations", "registered_animal_id = '$calfUuid'"))
        assertEquals(
            "Registration must reference the calf, dam and sire by animal UUID.",
            1,
            count("calf_registrations", "registered_animal_id = '$calfUuid' AND dam_id = '$damUuid' AND sire_id = '$sireUuid'")
        )

        val view = dao.getRegistrationByTag("Blu1234567").first()
        assertNotNull(view)
        assertEquals("Blu0000011", view!!.damTagNumber)
        assertEquals("Blu0000902", view.sireTagNumber)
        assertNotEquals(calfTag, view.animalId)
        assertEquals(calfUuid, view.animalId)

        // 4. Registering Blu1234567 again throws DuplicateTagException, and transaction rolls back
        val dupUuid = UUID.randomUUID().toString()
        val dupCalf = Animal(
            animalId = dupUuid,
            tagNumber = calfTag,
            birthdate = System.currentTimeMillis(),
            breed = "Bonsmara",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordGuid = UUID.randomUUID().toString()
        )
        val dupIdentifiers = listOf(
            AnimalIdentifierEntity(animalId = dupUuid, identifierType = IdentifierTypes.TAG, identifierValue = calfTag)
        )
        val dupRegistration = CalfRegistrationEntity(registeredAnimalId = dupUuid, registrationDate = "2026-09-18")

        try {
            dao.registerCalf(dupCalf, dupIdentifiers, emptyList(), dupRegistration)
            fail("Expected DuplicateTagException was not thrown")
        } catch (e: DuplicateTagException) {
            assertEquals(calfTag, e.tagNumber)
        }

        // Nothing was written by the rejected registration
        assertEquals(3, count("animals"))
        assertEquals(3, count("animal_identifiers", "identifier_type = 'TAG'"))
        assertEquals(3, count("calf_registrations"))
        assertEquals(0, count("animals", "animalId = '$dupUuid'"))

        // Verify the original calf is untouched
        val viewAfterDup = dao.getRegistrationByTag("Blu1234567").first()
        assertEquals(calfUuid, viewAfterDup!!.animalId)
    }

    private fun count(table: String, where: String? = null): Int {
        val sql = "SELECT COUNT(*) FROM `$table`" + (where?.let { " WHERE $it" } ?: "")
        return database!!.openHelper.readableDatabase.query(sql).use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}
