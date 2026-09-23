package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.CalfRegistrationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FindAnimalDatabaseTest {

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
    fun findAnimal_returnsCorrectCalfDetails() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database

        val animal = Animal(
            animalId = "FIND-001",
            birthdate = System.currentTimeMillis(),
            breed = "Simmentaler",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordguid = "guid-find-001"
        )
        database!!.animalDao().insert(animal)

        val calfReg = CalfRegistrationEntity(
            registeredAnimalId = "FIND-001",
            registrationDate = "2026-09-18"
        )
        database!!.calfRegistrationDao().insertCalfRegistration(calfReg)

        val found = database!!.calfRegistrationDao().getCalfRegistrationDetails("FIND-001").first()
        assertNotNull(found)
        assertEquals("FIND-001", found!!.registeredAnimalId)
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}
