package com.beeftech.calfregistration

import android.content.Context
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.fakes.FakeCalfRegistrationDao
import com.beeftech.calfregistration.fakes.FakePendingSyncDao
import com.beeftech.calfregistration.fakes.FakeTokenProvider
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
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class CalfRegistrationViewModelTest {

    private val mockContext = Mockito.mock(Context::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        Mockito.`when`(mockContext.applicationContext).thenReturn(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successfulApiClient(): CalfRegistrationApiClient {
        val mockEngine = MockEngine { request ->
            when {
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

        return CalfRegistrationApiClient(
            tokenProvider = FakeTokenProvider("tok"),
            baseUrl = "http://test-host/",
            httpClient = httpClient
        )
    }

    private fun buildViewModel(apiClient: CalfRegistrationApiClient): CalfRegistrationViewModel {
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = FakeCalfRegistrationDao(),
            pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao()),
            apiClient = apiClient
        )
        return CalfRegistrationViewModel(repository, mockContext)
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

        val viewModel = CalfRegistrationViewModel(repository, mockContext)

        assertEquals(1, viewModel.registeredCalves.value.size)
        assertEquals("RMB1", viewModel.registeredCalves.value.first().tagNumber)
    }

    @Test
    fun `isTagRegistered returns true for existing tag and false for new tag`() = runBlocking {
        val viewModel = buildViewModel(successfulApiClient())

        assertEquals(false, viewModel.isTagRegistered("Blu0000064"))

        val resultDeferred = CompletableDeferred<Pair<Boolean, String>>()
        viewModel.saveCalf(
            CalfRegistrationData(tagNumber = "Blu0000064", animalType = "Brangus")
        ) { success, message ->
            resultDeferred.complete(success to message)
        }
        withTimeout(5_000) { resultDeferred.await() }

        assertEquals(true, viewModel.isTagRegistered("Blu0000064"))
        assertEquals(false, viewModel.isTagRegistered("Red0000123"))
    }
}
