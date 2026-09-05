package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.CalfRegistration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FindAnimalDatabaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {

        context =
            ApplicationProvider.getApplicationContext()

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)

        val passphrase =
            ByteArray(32) { index ->
                (index + 1).toByte()
            }

        val result =
            DatabaseProvider.initialize(
                context = context,
                passphrase = passphrase
            )

        check(result is DatabaseResult.Success) {
            "Database could not be initialized."
        }
    }

    @After
    fun tearDown() {

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun findAnimal_existingAnimal_returnsAnimal() =
        runBlocking {

            val database =
                DatabaseProvider.getDatabase()

            assertNotNull(database)

            val dao =
                database!!.calfRegistrationDao()

            val testAnimal =
                CalfRegistration(
                    animalId = "TEST-001",
                    birthdate = 1725148800000L,
                    breed = "Bonsmara",
                    damId = "DAM-001",
                    sireId = "SIRE-001",
                    photoPath = null,
                    videoPath = null,
                    gpsLat = -26.2041,
                    gpsLng = 28.0473,
                    captureAt = 1725148800000L,
                    deviceId = "TEST-DEVICE",
                    recordguid = "TEST-GUID-001",
                    syncStatus = "PENDING",
                    syncedat = null
                )

            dao.insert(testAnimal)

            val result =
                dao.findByAnimalId(
                    "TEST-001"
                )

            assertNotNull(result)

            assertEquals(
                "TEST-001",
                result?.animalId
            )

            assertEquals(
                "Bonsmara",
                result?.breed
            )
        }

    @Test
    fun findAnimal_unknownAnimal_returnsNull() =
        runBlocking {

            val database =
                DatabaseProvider.getDatabase()

            assertNotNull(database)

            val dao =
                database!!.calfRegistrationDao()

            val result =
                dao.findByAnimalId(
                    "DOES-NOT-EXIST"
                )

            assertNull(result)
        }

    companion object {

        private const val DATABASE_NAME =
            "beeftech.db"
    }
}