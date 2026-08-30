package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.CalfRegistration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
    fun insertCalfRegistration_allowsReadBack() = runBlocking {

        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database

        val calf = CalfRegistration(
            animalId = "CALF-001",
            birthdate = System.currentTimeMillis(),
            breed = "Angus",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordguid = "guid-001"
        )

        database!!.calfRegistrationDao().insert(calf)

        val calves = database!!.calfRegistrationDao().getAll()

        assertEquals(1, calves.size)
        assertEquals("CALF-001", calves[0].animalId)
        assertEquals("Angus", calves[0].breed)
    }

    @Test
    fun duplicateAnimalId_isRejectedByUniqueIndex() = runBlocking {

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

        val calf = CalfRegistration(
            animalId = "CALF-002",
            birthdate = System.currentTimeMillis(),
            breed = "Brahman",
            gpsLat = -26.1,
            gpsLng = 27.9,
            captureAt = System.currentTimeMillis(),
            deviceId = "device-1",
            recordguid = "guid-002"
        )

        dao.insert(calf)

        val duplicate = calf.copy(id = 0, recordguid = "guid-003")

        var threwConstraintViolation = false
        try {
            dao.insert(duplicate)
        } catch (_: Exception){
            threwConstraintViolation = true
        }



        assertTrue(
            "Inserting a second record with the same animalId should be rejected.",
            threwConstraintViolation
        )
    }

    @Test
    fun existsByAnimalId_reflectsInsertedRecords() = runBlocking {

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

        assertTrue(
            "A record that hasn't been inserted yet should not exist.",
            !dao.existsByAnimalId("CALF-003")
        )

        dao.insert(
            CalfRegistration(
                animalId = "CALF-003",
                birthdate = System.currentTimeMillis(),
                breed = "Nguni",
                gpsLat = -26.1,
                gpsLng = 27.9,
                captureAt = System.currentTimeMillis(),
                deviceId = "device-1",
                recordguid = "guid-004"
            )
        )

        assertTrue(
            "A record that has been inserted should be found by animalId.",
            dao.existsByAnimalId("CALF-003")
        )
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}