package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.TeamUiState
import com.beeftech.management.viewmodel.TeamViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TeamViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun member(id: String, name: String, active: Boolean = true) =
        """{"user_id":"$id","username":"$name","role":3,"site_id":"s1","active":$active}"""

    private fun viewModel(handler: (HttpMethod, String) -> Pair<HttpStatusCode, String>): TeamViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request.method, request.url.encodedPath)
            respond(body, status, jsonHeaders)
        }
        return TeamViewModel(
            ManagementApiClient(provider, "http://test-host/", httpClient(engine))
        )
    }

    private fun httpClient(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /* The client does its I/O off the test dispatcher, so wait for the state instead of assuming it. */
    private suspend fun TeamViewModel.await(predicate: (TeamUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the members`() = runTest {
        val vm = viewModel { _, _ ->
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":[${member("1", "a")}]}"""
        }

        vm.refresh()
        vm.await { it.members.isNotEmpty() }

        assertEquals(listOf("a"), vm.uiState.value.members.map { it.username })
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `refresh without a connection sets needsConnection and keeps the old list`() = runTest {
        var online = true
        val engine = MockEngine {
            if (!online) throw IOException("offline")
            respond("""{"success":true,"message":"ok","data":[${member("1", "a")}]}""", HttpStatusCode.OK, jsonHeaders)
        }
        val vm = TeamViewModel(
            ManagementApiClient(provider, "http://test-host/", httpClient(engine))
        )

        vm.refresh()
        vm.await { it.members.isNotEmpty() }
        online = false
        vm.refresh()
        vm.await { it.needsConnection }

        assertTrue(vm.uiState.value.needsConnection)
        assertEquals(1, vm.uiState.value.members.size)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `deactivating a member replaces it in the list`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Get) {
                HttpStatusCode.OK to """{"success":true,"message":"ok","data":[${member("1", "a")}]}"""
            } else {
                HttpStatusCode.OK to """{"success":true,"message":"ok","data":${member("1", "a", active = false)}}"""
            }
        }
        vm.refresh()
        vm.await { it.members.isNotEmpty() }

        vm.setActive(vm.uiState.value.members.single(), false)
        vm.await { it.notice != null }

        assertFalse(vm.uiState.value.members.single().active)
        assertEquals("Deactivated a", vm.uiState.value.notice)
    }

    @Test
    fun `reset pin exposes the pin once`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Get) {
                HttpStatusCode.OK to """{"success":true,"message":"ok","data":[${member("1", "a")}]}"""
            } else {
                HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"pin":"48213"}}"""
            }
        }
        vm.refresh()
        vm.await { it.members.isNotEmpty() }

        vm.resetPin(vm.uiState.value.members.single())
        vm.await { it.issuedPin != null }
        assertEquals("48213", vm.uiState.value.issuedPin?.pin)

        vm.dismissIssuedPin()
        assertNull(vm.uiState.value.issuedPin)
    }

    @Test
    fun `create adds the member in name order and reports a rejected username`() = runTest {
        var reject = false
        val vm = viewModel { method, _ ->
            when {
                method == HttpMethod.Get ->
                    HttpStatusCode.OK to """{"success":true,"message":"ok","data":[${member("2", "zed")}]}"""
                reject -> HttpStatusCode.Conflict to """{"success":false,"message":"Username already taken"}"""
                else -> HttpStatusCode.Created to """{"success":true,"message":"ok","data":${member("1", "amy")}}"""
            }
        }
        vm.refresh()
        vm.await { it.members.isNotEmpty() }

        val created = CompletableDeferred<Unit>()
        vm.createWorker("amy", "12345") { created.complete(Unit) }
        created.awaitFired()
        assertEquals(listOf("amy", "zed"), vm.uiState.value.members.map { it.username })

        reject = true
        vm.createWorker("amy", "12345")
        vm.await { it.error != null }
        assertEquals("Username already taken", vm.uiState.value.error)
    }

    @Test
    fun `a 404 on the list says the server lacks team management, not that a user is gone`() = runTest {
        val vm = viewModel { _, _ -> HttpStatusCode.NotFound to "Not Found" }

        vm.refresh()
        vm.await { it.error != null }

        assertTrue(vm.uiState.value.error!!.contains("doesn't support team management"))
    }

    @Test
    fun `a 404 on a member action says the user no longer exists`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Get) {
                HttpStatusCode.OK to """{"success":true,"message":"ok","data":[${member("1", "a")}]}"""
            } else {
                HttpStatusCode.NotFound to """{"success":false,"message":"User not found"}"""
            }
        }
        vm.refresh()
        vm.await { it.members.isNotEmpty() }

        vm.setActive(vm.uiState.value.members.single(), false)
        vm.await { it.error != null }

        assertEquals("That user no longer exists.", vm.uiState.value.error)
    }
}
