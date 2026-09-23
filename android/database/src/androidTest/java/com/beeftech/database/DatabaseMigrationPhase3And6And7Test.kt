package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.AnimalMediaEntity
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.entity.AnimalOwnershipEntity
import com.beeftech.database.entity.AnimalPurchaseEntity
import com.beeftech.database.entity.AnimalWeightEntity
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
class DatabaseMigrationPhase3And6And7Test {

    private lateinit var context: Context
    private val databaseName = "beeftech_phase_test.db"

    private fun testPassphrase(): ByteArray {
        return "test-phase-passphrase".toByteArray(Charsets.UTF_8)
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
    fun testPhase3And6And7DatabaseSchemaAndDAOs() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = testPassphrase()
        )

        assertTrue(result is DatabaseResult.Success)
        val db = (result as DatabaseResult.Success).database

        // Insert parent Animal record
        val animal = Animal(
            animalId = "ANIMAL-001",
            tagNumber = "TAG-001",
            birthdate = System.currentTimeMillis(),
            breed = "Bonsmara",
            gpsLat = -26.0,
            gpsLng = 28.0,
            captureAt = System.currentTimeMillis(),
            deviceId = "dev-1",
            recordguid = "guid-1"
        )
        db.animalDao().insert(animal)

        // Phase 3: Identifiers
        val identifier = AnimalIdentifierEntity(
            animalId = "ANIMAL-001",
            identifierType = "RFID",
            identifierValue = "982000123456789"
        )
        db.animalIdentifierDao().insertIdentifier(identifier)

        val foundAnimalId = db.animalIdentifierDao().findAnimalIdByIdentifier("RFID", "982000123456789")
        assertEquals("ANIMAL-001", foundAnimalId)

        // Phase 3: Media
        val media = AnimalMediaEntity(
            animalId = "ANIMAL-001",
            filePath = "/media/photo1.jpg",
            mediaType = "PHOTO",
            createdAt = "2026-09-18T10:00:00"
        )
        db.animalMediaDao().insertMedia(media)

        val mediaList = db.animalMediaDao().getMediaForAnimal("ANIMAL-001").first()
        assertEquals(1, mediaList.size)
        assertEquals("/media/photo1.jpg", mediaList[0].filePath)

        // Phase 3: Weights
        val weight = AnimalWeightEntity(
            animalId = "ANIMAL-001",
            weightKg = 450.5,
            weighDate = "2026-09-18"
        )
        db.animalWeightDao().insertWeight(weight)

        val latestWeight = db.animalWeightDao().getLatestWeightForAnimal("ANIMAL-001").first()
        assertNotNull(latestWeight)
        assertEquals(450.5, latestWeight!!.weightKg, 0.01)

        // Phase 6: Ownerships
        val ownership = AnimalOwnershipEntity(
            animalId = "ANIMAL-001",
            ownerName = "Farmer John",
            ownershipPercentage = 100.0,
            startDate = "2026-01-01"
        )
        db.animalOwnershipDao().insertOwnership(ownership)

        val headCount = db.animalOwnershipDao().getCurrentHeadCountByOwner("Farmer John").first()
        assertNotNull(headCount)
        assertEquals(1, headCount!!.headCount)

        // Phase 6: Purchases
        val purchase = AnimalPurchaseEntity(
            animalId = "ANIMAL-001",
            purchasePrice = 12500.0,
            purchaseDate = "2026-01-01",
            sellerName = "Oak Valley Stud"
        )
        db.animalPurchaseDao().insertPurchase(purchase)

        val purchases = db.animalPurchaseDao().getPurchasesForAnimal("ANIMAL-001").first()
        assertEquals(1, purchases.size)
        assertEquals("Oak Valley Stud", purchases[0].sellerName)

        // Phase 6: Movements
        val movement = AnimalMovementEntity(
            animalId = "ANIMAL-001",
            destinationFarmId = "FARM-A",
            destinationPenId = "PEN-10",
            movementDate = "2026-09-18"
        )
        db.animalMovementDao().insert(movement)

        val movements = db.animalMovementDao().getByAnimalId("ANIMAL-001")
        assertEquals(1, movements.size)

        // Phase 7: Calf Registration
        val calfReg = CalfRegistrationEntity(
            registeredAnimalId = "ANIMAL-001",
            registrationDate = "2026-09-18"
        )
        db.calfRegistrationDao().insertCalfRegistration(calfReg)

        val calfDetails = db.calfRegistrationDao().getCalfRegistrationDetails("ANIMAL-001").first()
        assertNotNull(calfDetails)
        assertEquals("ANIMAL-001", calfDetails!!.registeredAnimalId)

        db.close()
    }
}
