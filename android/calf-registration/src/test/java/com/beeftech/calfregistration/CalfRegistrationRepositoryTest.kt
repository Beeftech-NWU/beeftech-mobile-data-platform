package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.data.SYNC_STATUS_SYNCED
import com.beeftech.calfregistration.fakes.FakeCalfRegistrationDao
import com.beeftech.calfregistration.fakes.FakePendingSyncDao
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.database.repository.PendingSyncRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalfRegistrationRepositoryTest {

    private fun successfulApiClient(): CalfRegistrationApiClient {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/api/auth/login") -> respond(
                    content = """{"success":true,"message":"ok","data":{"token":"tok"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )

                request.url.encodedPath.endsWith("/api/calf-registrations/sync") -> respond(
                    content = """
                        {"success":true,"message":"ok","data":{"results":[
                            {"recordguid":"any","animalId":"RMB12345","status":"SYNCED","serverSyncedAt":555,"message":null}
                        ]}}
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )

                else -> error("Unhandled request: ${request.url}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        return CalfRegistrationApiClient(baseUrl = "http://test-host/", httpClient = httpClient)
    }

    private fun failingApiClient(): CalfRegistrationApiClient {
        val mockEngine = MockEngine { _ ->
            respondError(HttpStatusCode.InternalServerError)
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        return CalfRegistrationApiClient(baseUrl = "http://test-host/", httpClient = httpClient)
    }

    @Test
    fun `saveCalf persists locally and syncs successfully`() = runTest {
        val calfDao = FakeCalfRegistrationDao()
        val pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao())
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = pendingSyncRepository,
            apiClient = successfulApiClient()
        )

        val formData = CalfRegistrationData(tagNumber = "RMB12345", animalType = "Bonsmara")

        val saved = repository.saveCalf(formData)

        assertTrue(saved.data.synced)
        assertTrue(calfDao.existsByAnimalId("RMB12345"))

        val persisted = calfDao.findByAnimalId("RMB12345")
        assertEquals(SYNC_STATUS_SYNCED, persisted?.syncStatus)
        assertEquals(555L, persisted?.syncedat)
    }

    @Test
    fun `saveCalf leaves record pending when sync fails`() = runTest {
        val calfDao = FakeCalfRegistrationDao()
        val pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao())
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = pendingSyncRepository,
            apiClient = failingApiClient()
        )

        val formData = CalfRegistrationData(tagNumber = "RMB99999", animalType = "Angus")

        val saved = repository.saveCalf(formData)

        assertTrue(calfDao.existsByAnimalId("RMB99999"))
        assertEquals(false, saved.data.synced)
        assertTrue(saved.syncErrorMessage?.isNotBlank() == true)
    }

    @Test
    fun `saveCalf preserves recordguid when re-saving the same animalId`() = runTest {
        val calfDao = FakeCalfRegistrationDao()
        val pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao())
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = pendingSyncRepository,
            apiClient = failingApiClient()
        )

        repository.saveCalf(CalfRegistrationData(tagNumber = "RMB11111", animalType = "Angus"))
        val firstGuid = calfDao.findByAnimalId("RMB11111")?.recordguid

        repository.saveCalf(CalfRegistrationData(tagNumber = "RMB11111", animalType = "Updated Breed"))
        val secondGuid = calfDao.findByAnimalId("RMB11111")?.recordguid

        assertEquals(firstGuid, secondGuid)
        assertEquals("Updated Breed", calfDao.findByAnimalId("RMB11111")?.breed)
    }

    @Test
    fun `syncPending updates sync status for successfully synced records`() = runTest {
        val calfDao = FakeCalfRegistrationDao()
        val pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao())
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = pendingSyncRepository,
            apiClient = failingApiClient()
        )

        // Save while offline (sync fails), record stays PENDING.
        repository.saveCalf(CalfRegistrationData(tagNumber = "RMB12345", animalType = "Bonsmara"))
        assertEquals("PENDING", calfDao.findByAnimalId("RMB12345")?.syncStatus)

        // Now retry with a working API client.
        val onlineRepository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = pendingSyncRepository,
            apiClient = successfulApiClient()
        )

        val syncOutcome = onlineRepository.syncPending()

        assertEquals(1, syncOutcome.syncedCount)
        assertEquals(SYNC_STATUS_SYNCED, calfDao.findByAnimalId("RMB12345")?.syncStatus)
    }

    @Test
    fun `loadAll returns all locally persisted records mapped to form data`() = runTest {
        val calfDao = FakeCalfRegistrationDao()
        val pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao())
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = pendingSyncRepository,
            apiClient = failingApiClient()
        )

        repository.saveCalf(CalfRegistrationData(tagNumber = "RMB1"))
        repository.saveCalf(CalfRegistrationData(tagNumber = "RMB2"))

        val all = repository.loadAll()

        assertEquals(2, all.size)
        assertTrue(all.any { it.tagNumber == "RMB1" })
        assertTrue(all.any { it.tagNumber == "RMB2" })
    }
}
