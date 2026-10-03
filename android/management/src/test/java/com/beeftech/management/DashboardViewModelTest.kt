package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.DashboardUiState
import com.beeftech.management.viewmodel.DashboardViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val summaryBody =
        """{"success":true,"message":"ok","data":{"calves":{"total":4,"last7Days":2}}}"""

    private fun viewModel(handler: () -> Pair<HttpStatusCode, String>): DashboardViewModel {
        val engine = MockEngine {
            val (status, body) = handler()
            respond(body, status, jsonHeaders)
        }
        return DashboardViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    /* The client does its I/O off the test dispatcher, so wait for the state instead of assuming it. */
    private suspend fun DashboardViewModel.await(predicate: (DashboardUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the summary`() = runTest {
        val vm = viewModel { HttpStatusCode.OK to summaryBody }

        vm.refresh()
        vm.await { it.summary != null }

        assertEquals(4L, vm.uiState.value.summary!!.calves.total)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `losing the connection keeps the last summary and asks for a connection`() = runTest {
        var online = true
        val engine = MockEngine {
            if (!online) throw IOException("offline")
            respond(summaryBody, HttpStatusCode.OK, jsonHeaders)
        }
        val vm = DashboardViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )

        vm.refresh()
        vm.await { it.summary != null }
        online = false
        vm.refresh()
        vm.await { it.needsConnection }

        assertTrue(vm.uiState.value.summary != null)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `a 404 says the server lacks the dashboard`() = runTest {
        val vm = viewModel { HttpStatusCode.NotFound to "Not Found" }

        vm.refresh()
        vm.await { it.error != null }

        assertTrue(vm.uiState.value.error!!.contains("doesn't support the dashboard"))
    }
}
