package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfCaptureContext
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.fakes.FakeCalfRegistrationDao
import com.beeftech.calfregistration.fakes.FakePendingSyncDao
import com.beeftech.calfregistration.fakes.ReceivedPhoto
import com.beeftech.calfregistration.fakes.failingApiClient
import com.beeftech.calfregistration.fakes.photoApiClient
import com.beeftech.calfregistration.fakes.recordingApiClient
import com.beeftech.calfregistration.fakes.rejectingApiClient
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
import java.io.File
import io.ktor.http.HttpStatusCode

class CalfRegistrationRepositoryTest {

    private val calfDao = FakeCalfRegistrationDao()
    private val pendingSyncDao = FakePendingSyncDao()

    // Sync queue reads are user-scoped, so the tests act as a signed-in user.
    private fun pendingSyncRepository() =
        PendingSyncRepository(pendingSyncDao, userIdProvider = { "test-user" })

    private fun repository(apiClient: CalfRegistrationApiClient) = CalfRegistrationRepository(
        calfRegistrationDao = calfDao,
        pendingSyncRepository = pendingSyncRepository(),
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
    fun `loadParentOptions lists registered females as dams and males as sires`() = runTest {
        val repository = repository(successfulApiClient())
        repository.saveCalf(form("Blu0000011").copy(gender = "Female", animalType = "BNM — Bonsmara"))
        repository.saveCalf(form("Red0000024").copy(gender = "Female", animalType = "BRN — Brangus"))
        repository.saveCalf(form("Blu0000902").copy(gender = "Male", animalType = "BNM — Bonsmara"))
        repository.saveCalf(form("Blu0000100").copy(gender = "Steer"))

        val options = repository.loadParentOptions()

        assertEquals(listOf("Blu0000011 (Bonsmara)", "Red0000024 (Brangus)"), options.dams)
        assertEquals(listOf("Blu0000902 (Bonsmara)"), options.sires)
    }

    @Test
    fun `loadParentOptions is empty when nothing is registered`() = runTest {
        val options = repository(successfulApiClient()).loadParentOptions()

        assertTrue(options.dams.isEmpty())
        assertTrue(options.sires.isEmpty())
    }

    @Test
    fun `a dam picked from the loaded options is linked on save`() = runTest {
        val repository = repository(successfulApiClient())
        repository.saveCalf(form("Blu0000011").copy(gender = "Female", animalType = "BNM — Bonsmara"))
        val damOption = repository.loadParentOptions().dams.single()

        val outcome = repository.saveCalf(form("Blu1234567", dam = damOption))

        assertTrue(outcome.warnings.isEmpty())
        assertEquals(
            calfDao.animals.first { it.gender == "Female" }.animalId,
            calfDao.registrations.last().damId
        )
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
    fun `a server rejection keeps the calf pending and remembers the message until the cap`() = runTest {
        val repository = repository(rejectingApiClient("Tag already registered"))

        val outcome = repository.saveCalf(form("Blu1234567"))

        assertFalse(outcome.needsAttention)
        assertEquals("Tag already registered", outcome.syncErrorMessage)
        val row = calfDao.registrations.single()
        assertEquals("PENDING", row.syncStatus)
        assertEquals(1, row.syncAttempts)
        assertEquals("Tag already registered", row.syncError)
        assertEquals(1, pendingSyncDao.snapshot().size)
    }

    @Test
    fun `a transport failure never counts toward the rejection cap`() = runTest {
        val repository = repository(failingApiClient())

        repeat(5) { repository.saveCalf(form("Blu123456$it")) }
        repository.syncPending()
        repository.syncPending()

        assertTrue(calfDao.registrations.all { it.syncStatus == "PENDING" && it.syncAttempts == 0 })
    }

    @Test
    fun `after the cap the calf becomes REJECTED, leaves the queue and is no longer retried`() = runTest {
        val repository = repository(rejectingApiClient("Tag already registered"))
        repository.saveCalf(form("Blu1234567")) // attempt 1

        val second = repository.syncPending() // attempt 2
        assertEquals(1, second.errorMessagesByTagNumber.size)
        assertTrue(second.rejectedByTagNumber.isEmpty())

        val third = repository.syncPending() // attempt 3 -> REJECTED
        assertTrue(third.errorMessagesByTagNumber.isEmpty())
        assertEquals(mapOf("Blu1234567" to "Tag already registered"), third.rejectedByTagNumber)
        assertEquals("REJECTED", calfDao.registrations.single().syncStatus)
        assertTrue(pendingSyncDao.snapshot().isEmpty())

        val fourth = repository.syncPending()
        assertEquals(0, fourth.syncedCount)
        assertTrue(fourth.errorMessagesByTagNumber.isEmpty())
        assertTrue(fourth.rejectedByTagNumber.isEmpty())
        assertEquals(3, calfDao.registrations.single().syncAttempts)
    }

    @Test
    fun `loadAll shows a rejected calf as needing attention with the server message`() = runTest {
        val repository = repository(rejectingApiClient("Tag already registered"))
        repository.saveCalf(form("Blu1234567"))
        repeat(2) { repository.syncPending() }

        val loaded = repository.loadAll().single()

        assertTrue(loaded.needsAttention)
        assertFalse(loaded.synced)
        assertEquals("Tag already registered", loaded.syncError)
    }

    @Test
    fun `manual retry gives a rejected calf fresh attempts and syncs it once the server accepts`() = runTest {
        val rejecting = repository(rejectingApiClient("Tag already registered"))
        rejecting.saveCalf(form("Blu1234567"))
        repeat(2) { rejecting.syncPending() }
        assertEquals("REJECTED", calfDao.registrations.single().syncStatus)

        // The same records, but the server now accepts them.
        val outcome = repository(successfulApiClient()).syncPending(retryRejected = true)

        assertEquals(1, outcome.syncedCount)
        val row = calfDao.registrations.single()
        assertEquals("SYNCED", row.syncStatus)
        assertEquals(0, row.syncAttempts)
        assertNull(row.syncError)
    }

    @Test
    fun `syncing without the manual retry leaves a rejected calf alone`() = runTest {
        val rejecting = repository(rejectingApiClient())
        rejecting.saveCalf(form("Blu1234567"))
        repeat(2) { rejecting.syncPending() }

        val outcome = repository(successfulApiClient()).syncPending()

        assertEquals(0, outcome.syncedCount)
        assertEquals("REJECTED", calfDao.registrations.single().syncStatus)
    }

    private fun photoFile(vararg bytes: Byte): String =
        File.createTempFile("calf-photo", ".jpg").also {
            it.deleteOnExit()
            it.writeBytes(bytes)
        }.absolutePath

    @Test
    fun `a calf saved with a photo uploads it right after the record syncs`() = runTest {
        val received = mutableListOf<ReceivedPhoto>()
        val path = photoFile(1, 2, 3)

        val outcome = repository(photoApiClient(received)).saveCalf(form("Blu1234567").copy(photoPath = path))

        assertTrue(outcome.data.synced)
        assertFalse(outcome.photoPending)
        assertEquals("Blu1234567", received.single().tagNumber)
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(received.single().bytes))
        assertEquals("UPLOADED", calfDao.media.single().uploadStatus)
    }

    @Test
    fun `a photo that cannot be sent yet stays pending and is flagged on the saved calf`() = runTest {
        val received = mutableListOf<ReceivedPhoto>()

        val outcome = repository(photoApiClient(received, HttpStatusCode.ServiceUnavailable))
            .saveCalf(form("Blu1234567").copy(photoPath = photoFile(1)))

        assertTrue(outcome.data.synced) // the record itself is on the server
        assertTrue(outcome.photoPending)
        assertEquals("PENDING", calfDao.media.single().uploadStatus)
        assertEquals("SYNCED", calfDao.registrations.single().syncStatus)
    }

    @Test
    fun `syncPending uploads the photo of a calf that synced earlier without it`() = runTest {
        val received = mutableListOf<ReceivedPhoto>()
        repository(photoApiClient(received, HttpStatusCode.ServiceUnavailable))
            .saveCalf(form("Blu1234567").copy(photoPath = photoFile(7, 7)))
        assertTrue(received.size == 1) // first attempt failed

        val outcome = repository(photoApiClient(received)).syncPending()

        assertEquals(1, outcome.photosUploaded)
        assertEquals(0, outcome.photosPending)
        assertEquals("UPLOADED", calfDao.media.single().uploadStatus)
    }

    @Test
    fun `a photo the server refuses is marked FAILED and not retried`() = runTest {
        val received = mutableListOf<ReceivedPhoto>()
        val repository = repository(photoApiClient(received, HttpStatusCode.PayloadTooLarge))
        repository.saveCalf(form("Blu1234567").copy(photoPath = photoFile(1)))

        assertEquals("FAILED", calfDao.media.single().uploadStatus)
        assertTrue(calfDao.media.single().uploadError!!.contains("413"))

        repository.syncPending()
        assertEquals(1, received.size)
        assertEquals(0, repository.pendingPhotoCount())
    }

    @Test
    fun `a photo path with no file behind it is skipped instead of retried forever`() = runTest {
        val received = mutableListOf<ReceivedPhoto>()
        val repository = repository(photoApiClient(received))

        val outcome = repository.saveCalf(form("Blu1234567").copy(photoPath = "captured_photo_uri"))

        assertTrue(outcome.data.synced)
        assertFalse(outcome.photoPending)
        assertTrue(received.isEmpty())
        assertEquals(0, repository.pendingPhotoCount())
    }

    @Test
    fun `no photo is uploaded for a calf the server does not have yet`() = runTest {
        val received = mutableListOf<ReceivedPhoto>()
        val repository = repository(failingApiClient())
        repository.saveCalf(form("Blu1234567").copy(photoPath = photoFile(1)))

        val summary = repository.uploadPendingPhotos()

        assertEquals(0, summary.uploaded)
        assertTrue(received.isEmpty())
        assertEquals("PENDING", calfDao.media.single().uploadStatus)
    }

    @Test
    fun `a batch from two devices is sent as one request per device, each labelled with its own device`() = runTest {
        val requests = mutableListOf<Pair<String, String>>() // deviceId -> body
        val recordingClient = recordingApiClient(requests)

        fun repositoryFor(device: String, api: CalfRegistrationApiClient) = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = pendingSyncRepository(),
            apiClient = api,
            captureContextProvider = { CalfCaptureContext(deviceId = device) }
        )

        repositoryFor("PHONE-A", failingApiClient()).saveCalf(form("Blu1111111"))
        repositoryFor("PHONE-B", failingApiClient()).saveCalf(form("Blu2222222"))

        val outcome = repositoryFor("PHONE-A", recordingClient).syncPending()

        assertEquals(2, outcome.syncedCount)
        assertEquals(setOf("PHONE-A", "PHONE-B"), requests.map { it.first }.toSet())
        requests.forEach { (device, body) ->
            val expectedTag = if (device == "PHONE-A") "Blu1111111" else "Blu2222222"
            assertTrue(body.contains(expectedTag))
            assertFalse(body.contains(if (device == "PHONE-A") "Blu2222222" else "Blu1111111"))
        }
        assertTrue(calfDao.registrations.all { it.syncStatus == "SYNCED" })
    }

    @Test
    fun `old tag, reference and proofs are saved, listed and sent to the server`() = runTest {
        val requests = mutableListOf<Pair<String, String>>()
        val repository = repository(com.beeftech.calfregistration.fakes.recordingApiClient(requests))

        repository.saveCalf(
            form("Blu1234567").copy(
                oldTagNumber = "OLD-1", referenceNumber = "REF-9", processProof = "P-77", implantProof = "I-12"
            )
        )

        val loaded = repository.loadAll().single()
        assertEquals("OLD-1", loaded.oldTagNumber)
        assertEquals("REF-9", loaded.referenceNumber)
        assertEquals("P-77", loaded.processProof)
        assertEquals("I-12", loaded.implantProof)

        val body = requests.single().second
        assertTrue(body.contains("\"oldTagNumber\":\"OLD-1\""))
        assertTrue(body.contains("\"referenceNumber\":\"REF-9\""))
        assertTrue(body.contains("\"processProof\":\"P-77\""))
        assertTrue(body.contains("\"implantProof\":\"I-12\""))
    }

    @Test
    fun `saveCalf reports a saveError, not a pending sync, when it fails before the local write`() = runTest {
        calfDao.lookupFailure = IllegalStateException("database is locked")

        val outcome = repository(successfulApiClient()).saveCalf(form("Blu1234567", dam = "Blu0000011 (Bonsmara)"))

        assertEquals("database is locked", outcome.saveError)
        assertNull(outcome.syncErrorMessage)
        assertFalse(outcome.data.synced)
        assertTrue(calfDao.animals.isEmpty())
        assertTrue(pendingSyncDao.snapshot().isEmpty())
    }

    @Test
    fun `syncPending marks records SYNCED and clears the queue`() = runTest {
        repository(failingApiClient()).saveCalf(form("Blu1234567"))

        val outcome = repository(successfulApiClient()).syncPending()

        assertEquals(1, outcome.syncedCount)
        assertEquals("SYNCED", calfDao.registrations.single().syncStatus)
        assertNotNull(calfDao.registrations.single().syncedAt)
        assertTrue(pendingSyncRepository().getPendingOperations().isEmpty())
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
