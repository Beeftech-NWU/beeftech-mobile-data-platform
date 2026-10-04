package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.TeamUiState
import com.beeftech.management.viewmodel.TeamViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/* The Add user dialog picks a site from the server's list instead of taking a typed id. */
@OptIn(ExperimentalCoroutinesApi::class)
class TeamSitePickerTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun viewModel(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): TeamViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, jsonHeaders)
        }
        return TeamViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    private suspend fun TeamViewModel.await(predicate: (TeamUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loadSites fills the picker`() = runTest {
        val vm = viewModel {
            HttpStatusCode.OK to
                """{"success":true,"message":"ok","data":[
                {"siteId":"s1","name":"North","active":true,"createdAt":1,"activeUserCount":1},
                {"siteId":"s2","name":"Old","active":false,"createdAt":1,"activeUserCount":0}]}"""
        }

        vm.loadSites()
        vm.await { it.sites.isNotEmpty() }

        assertEquals(listOf("North", "Old"), vm.uiState.value.sites.map { it.name })
    }

    @Test
    fun `a failed site load leaves the picker as it was and shows no error`() = runTest {
        val vm = viewModel { HttpStatusCode.NotFound to "Not Found" }

        vm.loadSites()
        withContext(Dispatchers.Default) { kotlinx.coroutines.delay(200) }

        assertEquals(emptyList<Any>(), vm.uiState.value.sites)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `creating a user sends the chosen site id`() = runTest {
        var body = ""
        val vm = viewModel {
            body = String(it.body.toByteArray())
            HttpStatusCode.Created to
                """{"success":true,"message":"ok","data":{"user_id":"u9","username":"nora","role":3,"site_id":"s1","active":true}}"""
        }

        vm.createWorker("nora", "12345", siteId = "s1")
        vm.await { it.members.isNotEmpty() }

        assertEquals(true, body.contains(""""site_id":"s1""""))
    }
}
