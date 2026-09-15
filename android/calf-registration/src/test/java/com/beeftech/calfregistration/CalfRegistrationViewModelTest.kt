package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.fakes.FakeCalfRegistrationDao
import com.beeftech.calfregistration.fakes.FakePendingSyncDao
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel
import com.beeftech.database.repository.PendingSyncRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * These tests avoid virtual-time [kotlinx.coroutines.test.TestDispatcher]s
 * on purpose: [CalfRegistrationApiClient] exercises a real Ktor
 * `HttpClient` (backed by [MockEngine]), which internally dispatches work
 * on real dispatchers rather than a controllable [kotlinx.coroutines.test.TestCoroutineScheduler].
 * Mixing that with virtual time leads to flaky "did the coroutine finish
 * yet?" races. Instead, Main is swapped for the real
 * [Dispatchers.Unconfined], and each test synchronizes on the ViewModel's
 * `onResult` callback (or, where there is none, on the fact that the
 * fakes/mapper never truly suspend) using real coroutine primitives.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalfRegistrationViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    private fun buildViewModel(apiClient: CalfRegistrationApiClient): CalfRegistrationViewModel {
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = FakeCalfRegistrationDao(),
            pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao()),
            apiClient = apiClient
        )
        return CalfRegistrationViewModel(repository)
    }

    @Test
    fun `saveCalf updates registeredCalves and reports success`() = runBlocking {
        val viewModel = buildViewModel(successfulApiClient())
        val resultDeferred = CompletableDeferred<Pair<Boolean, String>>()

        viewModel.saveCalf(
            CalfRegistrationData(tagNumber = "RMB12345", animalType = "Bonsmara")
        ) { success, message ->
            resultDeferred.complete(success to message)
        }

        val (success, message) = withTimeout(5_000) { resultDeferred.await() }

        assertEquals(true, success)
        assertTrue(message.isNotBlank())
        assertEquals(1, viewModel.registeredCalves.value.size)
        assertEquals("RMB12345", viewModel.registeredCalves.value.first().tagNumber)
        assertTrue(viewModel.registeredCalves.value.first().synced)
    }

    @Test
    fun `loadCalves populates registeredCalves from the repository on init`() = runBlocking {
        val calfDao = FakeCalfRegistrationDao()
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao()),
            apiClient = successfulApiClient()
        )

        repository.saveCalf(CalfRegistrationData(tagNumber = "RMB1"))

        val viewModel = CalfRegistrationViewModel(repository)

        assertEquals(1, viewModel.registeredCalves.value.size)
        assertEquals("RMB1", viewModel.registeredCalves.value.first().tagNumber)
    }
}
