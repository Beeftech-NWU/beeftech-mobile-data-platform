package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigration8To9Test {

    private lateinit var context: Context
    private val databaseName = "beeftech.db"

    private fun newPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        DatabaseProvider.close()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        DatabaseProvider.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrateVersion8To9_enforcesCascadeDelete() = runBlocking {
        // 1. Create v9 database via factory
        val result = DatabaseFactory.create(context = context, passphrase = newPassphrase())
        assertTrue(result is DatabaseResult.Success)
        val db = (result as DatabaseResult.Success).database

        val animal = Animal(
            animalId = "ANIMAL-001",
            birthdate = 1000L,
            breed = "Brahman",
            gpsLat = -26.0,
            gpsLng = 28.0,
            captureAt = 1000L,
            deviceId = "dev-1",
            recordguid = "guid-1"
        )
        db.animalDao().insert(animal)

        db.treatmentDao().insert(
            Treatment(
                animalId = "ANIMAL-001",
                disease = "Fever",
                treatmentName = "Oxytetracycline",
                batchNumber = "BATCH-1",
                volumeUsed = "10ml",
                cost = 100.0,
                timestamp = System.currentTimeMillis()
            )
        )

        assertEquals(1, db.treatmentDao().getByAnimalId("ANIMAL-001").size)

        // Delete parent animal -> CASCADE should remove treatment
        db.animalDao().delete(animal)

        assertEquals(0, db.treatmentDao().getByAnimalId("ANIMAL-001").size)
    }
}
