package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfCaptureContext
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.fakes.FakeCalfRegistrationDao
import com.beeftech.calfregistration.fakes.FakePendingSyncDao
import com.beeftech.calfregistration.fakes.failingApiClient
import com.beeftech.calfregistration.fakes.successfulApiClient
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.repository.PendingSyncRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalfRegistrationRepositoryTest {

    private val calfDao = FakeCalfRegistrationDao()
    private val pendingSyncDao = FakePendingSyncDao()

    private fun repository(apiClient: CalfRegistrationApiClient) = CalfRegistrationRepository(
        calfRegistrationDao = calfDao,
        pendingSyncRepository = PendingSyncRepository(pendingSyncDao),
        apiClient = apiClient,
        captureContextProvider = { CalfCaptureContext(deviceId = "TEST-DEVICE") }
    )

    private fun form(tag: String, dam: String = "Select dame", sire: String = "Select sire") =
        CalfRegistrationData(tagNumber = tag, animalType = "BNM — Bonsmara", dameTagNumber = dam, sireTagNumber = sire)

    @Test
    fun `saveCalf creates one animal, one TAG and one registration, then syncs`() = runTest {
        val outcome = repository(successfulApiClient()).saveCalf(form("Blu1234567"))

        assertTrue(outcome.data.synced)
        assertNull(outcome.validationError)
        assertEquals(1, calfDao.animals.size)
        assertEquals(1, calfDao.tagIdentifierCount())
        assertEquals(1, calfDao.registrations.size)
        assertEquals(calfDao.animals.single().animalId, calfDao.registrations.single().registeredAnimalId)
        assertEquals("SYNCED", calfDao.registrations.single().syncStatus)
    }

    @Test
    fun `saveCalf rejects a duplicate tag as a validation error and writes nothing`() = runTest {
        val repository = repository(successfulApiClient())
        repository.saveCalf(form("Blu1234567"))

        val outcome = repository.saveCalf(form("Blu1234567"))

        assertNotNull(outcome.validationError)
        assertNull(outcome.syncErrorMessage)
        assertFalse(outcome.data.synced)
        assertEquals(1, calfDao.animals.size)
        assertEquals(1, calfDao.tagIdentifierCount())
        assertEquals(1, calfDao.registrations.size)
    }

    @Test
    fun `saveCalf rejects a duplicate entered in shorthand`() = runTest {
        val repository = repository(successfulApiClient())
        repository.saveCalf(form("Blu1234567"))

        val outcome = repository.saveCalf(form("b1234567"))

        assertNotNull(outcome.validationError)
        assertEquals(1, calfDao.animals.size)
    }

    @Test
    fun `saveCalf rejects a malformed tag as a validation error`() = runTest {
        val outcome = repository(successfulApiClient()).saveCalf(form("RMB12345"))

        assertNotNull(outcome.validationError)
        assertTrue(calfDao.animals.isEmpty())
    }

    @Test
    fun `saveCalf saves with a null dam and a warning when the dam is not registered`() = runTest {
        val outcome = repository(successfulApiClient())
            .saveCalf(form("Blu1234567", dam = "Blu0000011 (Bonsmara)"))

        assertNull(outcome.validationError)
        assertNull(calfDao.registrations.single().damId)
        assertEquals(1, outcome.warnings.size)
        assertTrue(outcome.warnings.single().contains("Blu0000011"))
    }

    @Test
    fun `saveCalf links a registered dam and sire by animal id`() = runTest {
        val repository = repository(successfulApiClient())
        repository.saveCalf(form("Blu0000011"))
        repository.saveCalf(form("Blu0000902"))
        val damId = calfDao.findAnimalIdByTag("Blu0000011")
        val sireId = calfDao.findAnimalIdByTag("Blu0000902")

        val outcome = repository.saveCalf(
            form("Blu1234567", dam = "Blu0000011 (Bonsmara)", sire = "Blu0000902 (Bonsmara Stud)")
        )

        val registration = calfDao.registrations.last()
        assertEquals(damId, registration.damId)
        assertEquals(sireId, registration.sireId)
        assertTrue(outcome.warnings.isEmpty())
        assertEquals("Blu0000011", outcome.data.dameTagNumber)
    }

    @Test
    fun `saveCalf queues a pending operation keyed by record guid when sync fails`() = runTest {
        val outcome = repository(failingApiClient()).saveCalf(form("Blu1234567"))

        assertFalse(outcome.data.synced)
        assertTrue(outcome.syncErrorMessage?.isNotBlank() == true)
        assertEquals("PENDING", calfDao.registrations.single().syncStatus)

        val queued = pendingSyncDao.snapshot().single()
        assertEquals("CALF_REGISTRATION", queued.entityType)
        assertEquals(calfDao.registrations.single().recordGuid, queued.entityId)
    }

    @Test
    fun `syncPending marks records SYNCED and clears the queue`() = runTest {
        repository(failingApiClient()).saveCalf(form("Blu1234567"))

        val outcome = repository(successfulApiClient()).syncPending()

        assertEquals(1, outcome.syncedCount)
        assertEquals("SYNCED", calfDao.registrations.single().syncStatus)
        assertNotNull(calfDao.registrations.single().syncedAt)
        assertTrue(PendingSyncRepository(pendingSyncDao).getPendingOperations().isEmpty())
    }

    @Test
    fun `syncPending reports errors by tag number when the server is down`() = runTest {
        repository(failingApiClient()).saveCalf(form("Blu1234567"))

        val outcome = repository(failingApiClient()).syncPending()

        assertEquals(0, outcome.syncedCount)
        assertTrue("Blu1234567" in outcome.errorMessagesByTagNumber)
        assertEquals("PENDING", calfDao.registrations.single().syncStatus)
    }

    @Test
    fun `isTagRegistered matches full and shorthand tags`() = runTest {
        val repository = repository(successfulApiClient())
        repository.saveCalf(form("Blu1234567"))

        assertTrue(repository.isTagRegistered("Blu1234567"))
        assertTrue(repository.isTagRegistered("B1234567"))
        assertFalse(repository.isTagRegistered("Red0000123"))
        assertFalse(repository.isTagRegistered(""))
    }

    @Test
    fun `loadAll returns saved calves with their real sync state`() = runTest {
        repository(failingApiClient()).saveCalf(form("Blu0000001"))
        repository(successfulApiClient()).saveCalf(form("Blu0000002"))

        val all = repository(failingApiClient()).loadAll()

        assertEquals(2, all.size)
        assertFalse(all.single { it.tagNumber == "Blu0000001" }.synced)
        assertTrue(all.single { it.tagNumber == "Blu0000002" }.synced)
    }
}
