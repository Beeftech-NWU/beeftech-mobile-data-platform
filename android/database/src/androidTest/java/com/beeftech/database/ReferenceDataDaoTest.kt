package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.dao.CostTypeValue
import com.beeftech.database.dao.ReferenceSnapshot
import com.beeftech.database.dao.ReferenceValue
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.DeviceConfigEntry
import com.beeftech.database.entity.ReferenceItem
import com.beeftech.database.entity.Treatment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/*
 * The cache apply: values from the server are stored, a value the server turns off is only
 * hidden, and nothing is ever deleted, so records that already use a value still resolve.
 */
@RunWith(AndroidJUnit4::class)
class ReferenceDataDaoTest {

    private lateinit var context: Context

    @Before
    fun setUp() {

        context =
            ApplicationProvider.getApplicationContext()

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)

        val result =
            DatabaseProvider.initialize(
                context = context,
                passphrase = ByteArray(32) { (it + 1).toByte() }
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

    private val db get() = DatabaseProvider.getDatabase()!!

    private fun snapshot(
        version: Long,
        diseases: List<ReferenceValue> = emptyList(),
        treatmentTypes: List<ReferenceValue> = emptyList(),
        costTypes: List<CostTypeValue> = emptyList()
    ) = ReferenceSnapshot(version, diseases, treatmentTypes, costTypes)

    private suspend fun insertAnimal(id: String) {
        db.animalDao().insert(
            Animal(
                animalId = id,
                birthdate = 1725148800000L,
                breed = "Bonsmara",
                gpsLat = -26.0,
                gpsLng = 28.0,
                captureAt = 1725148800000L,
                deviceId = "device-1",
                recordGuid = "guid-animal-$id"
            )
        )
    }

    @Test
    fun apply_storesValuesInTheCacheAndTheDiseaseTableAndRemembersTheVersion() =
        runBlocking {

            val dao = db.referenceDataDao()

            dao.apply(
                snapshot(
                    version = 7,
                    diseases = listOf(ReferenceValue(1, "Pinkeye", true), ReferenceValue(2, "Rabies", true)),
                    treatmentTypes = listOf(ReferenceValue(3, "Hoof trimming", true), ReferenceValue(4, "Vaccination", true))
                ),
                now = 100L
            )

            assertEquals(listOf("Pinkeye", "Rabies"), dao.getActive(ReferenceItem.KIND_DISEASES).map { it.displayName })
            assertEquals(listOf("Hoof trimming", "Vaccination"), dao.getActive(ReferenceItem.KIND_TREATMENT_TYPES).map { it.displayName })
            assertEquals(1, dao.getAll(ReferenceItem.KIND_DISEASES).first { it.itemKey == "Pinkeye" }.serverId)
            assertEquals("7", dao.getConfig(DeviceConfigEntry.REFERENCE_DATA_VERSION))

            /* A treatment points at the diseases table, so a new server disease has to be there. */
            assertTrue(db.diseaseDao().getAll().any { it.name == "Pinkeye" && it.diseaseId == "Pinkeye" })
        }

    @Test
    fun apply_whenTheServerTurnsAValueOff_hidesItAndDeletesNothing() =
        runBlocking {

            val dao = db.referenceDataDao()
            insertAnimal("A-1")

            dao.apply(snapshot(1, diseases = listOf(ReferenceValue(2, "Rabies", true))), 100L)
            db.treatmentDao().insert(
                Treatment(
                    animalId = "A-1",
                    disease = "Rabies",
                    treatmentName = "Vaccination",
                    batchNumber = "B-1",
                    volumeUsed = "5 ml",
                    cost = 10.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1725148800000L
                )
            )

            dao.apply(snapshot(2, diseases = listOf(ReferenceValue(2, "Rabies", false))), 200L)

            assertFalse(dao.getActive(ReferenceItem.KIND_DISEASES).any { it.itemKey == "Rabies" })
            val kept = dao.getAll(ReferenceItem.KIND_DISEASES).single { it.itemKey == "Rabies" }
            assertFalse(kept.active)
            assertEquals(200L, kept.updatedAt)
            /* The disease the treatment points at is still there, and so is the treatment. */
            assertTrue(db.diseaseDao().getAll().any { it.diseaseId == "Rabies" })
            assertEquals("Rabies", db.treatmentDao().getByAnimalId("A-1").single().disease)
            assertEquals("2", dao.getConfig(DeviceConfigEntry.REFERENCE_DATA_VERSION))

            /* And switching it back on works. */
            dao.apply(snapshot(3, diseases = listOf(ReferenceValue(2, "Rabies", true))), 300L)
            assertTrue(dao.getActive(ReferenceItem.KIND_DISEASES).any { it.itemKey == "Rabies" })
        }

    @Test
    fun apply_leavesAValueTheServerDoesNotMentionAlone() =
        runBlocking {

            val dao = db.referenceDataDao()
            dao.upsertItems(
                listOf(ReferenceItem(ReferenceItem.KIND_TREATMENT_TYPES, "Local only", "Local only", true, 0, null, 1L))
            )

            dao.apply(snapshot(1, treatmentTypes = listOf(ReferenceValue(4, "Vaccination", true))), 100L)

            assertEquals(
                listOf("Local only", "Vaccination"),
                dao.getActive(ReferenceItem.KIND_TREATMENT_TYPES).map { it.displayName }
            )
        }

    @Test
    fun apply_updatesCostTypesInPlaceAndAddsNewOnesWithoutTouchingCostsThatUseThem() =
        runBlocking {

            val dao = db.referenceDataDao()
            insertAnimal("A-1")
            db.animalCostDao().insert(
                AnimalCost(
                    animalId = "A-1",
                    costType = "FEED",
                    amount = 99.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1725148800000L
                )
            )
            val before = db.costTypeDao().getActive().map { it.code }
            assertTrue("FEED" in before)

            dao.apply(
                snapshot(
                    version = 2,
                    costTypes = listOf(
                        CostTypeValue("FEED", "Feed / Ration", 5, false),
                        CostTypeValue("AUCTION", "Auction fees", 20, true)
                    )
                ),
                now = 100L
            )

            val after = db.costTypeDao().getActive().map { it.code }
            assertFalse("FEED" in after)
            assertTrue("AUCTION" in after)
            assertEquals(before.size, after.size)
            /* The cost that already used FEED is untouched, so it still resolves. */
            assertEquals(99.0, db.animalCostDao().getByAnimalId("A-1").single().amount, 0.001)
            assertEquals("FEED", db.animalCostDao().getByAnimalId("A-1").single().costType)
        }

    @Test
    fun apply_neverSwitchesOffTheTreatmentCostType() =
        runBlocking {

            val dao = db.referenceDataDao()

            dao.apply(snapshot(2, costTypes = listOf(CostTypeValue("TREATMENT", "Treatment", 4, false))), 100L)

            assertTrue(db.costTypeDao().getActive().any { it.code == "TREATMENT" })
        }

    @Test
    fun apply_isRepeatableAndAnUnknownVersionIsNull() =
        runBlocking {

            val dao = db.referenceDataDao()
            assertNull(dao.getConfig(DeviceConfigEntry.REFERENCE_DATA_VERSION))

            val data = snapshot(
                1,
                diseases = listOf(ReferenceValue(1, "Pinkeye", true)),
                treatmentTypes = listOf(ReferenceValue(3, "Hoof trimming", true)),
                costTypes = listOf(CostTypeValue("AUCTION", "Auction fees", 20, true))
            )
            dao.apply(data, 100L)
            val diseasesAfterFirst = db.diseaseDao().getAll().size
            dao.apply(data, 200L)

            assertEquals(1, dao.getAll(ReferenceItem.KIND_DISEASES).size)
            assertEquals(1, dao.getAll(ReferenceItem.KIND_TREATMENT_TYPES).size)
            assertEquals(diseasesAfterFirst, db.diseaseDao().getAll().size)
            assertEquals(1, db.costTypeDao().getActive().count { it.code == "AUCTION" })
        }

    companion object {

        private const val DATABASE_NAME = "beeftech.db"
    }
}
