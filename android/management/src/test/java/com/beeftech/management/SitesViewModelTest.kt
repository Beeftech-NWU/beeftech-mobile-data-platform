package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.SitesUiState
import com.beeftech.management.viewmodel.SitesViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
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
class SitesViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun site(id: String, name: String, active: Boolean = true, users: Int = 0) =
        """{"siteId":"$id","name":"$name","active":$active,"createdAt":1,"activeUserCount":$users}"""

    private fun listBody(vararg sites: String) = """{"success":true,"message":"ok","data":[${sites.joinToString(",")}]}"""
    private fun oneBody(site: String) = """{"success":true,"message":"ok","data":$site}"""

    private fun viewModel(handler: (HttpMethod, String) -> Pair<HttpStatusCode, String>): SitesViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request.method, request.url.encodedPath)
            respond(body, status, jsonHeaders)
        }
        return SitesViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    /* The client does its I/O off the test dispatcher, so wait for the state instead of assuming it. */
    private suspend fun SitesViewModel.await(predicate: (SitesUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the sites`() = runTest {
        val vm = viewModel { _, _ -> HttpStatusCode.OK to listBody(site("s1", "North", users = 3)) }

        vm.refresh()
        vm.await { it.sites.isNotEmpty() }

        assertEquals(3L, vm.uiState.value.sites.single().activeUserCount)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `creating a site adds it in name order and says so`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Post) HttpStatusCode.Created to oneBody(site("s2", "Alpha"))
            else HttpStatusCode.OK to listBody(site("s1", "North"))
        }
        vm.refresh()
        vm.await { it.sites.isNotEmpty() }

        val created = CompletableDeferred<Unit>()
        vm.createSite("  Alpha ", " alph ") { created.complete(Unit) }
        created.awaitFired()

        assertEquals(listOf("Alpha", "North"), vm.uiState.value.sites.map { it.name })
        assertEquals("Created Alpha", vm.uiState.value.notice)
    }

    @Test
    fun `a duplicate name shows the server's message and adds nothing`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Post) {
                HttpStatusCode.Conflict to """{"success":false,"message":"A site with that name already exists"}"""
            } else {
                HttpStatusCode.OK to listBody(site("s1", "North"))
            }
        }
        vm.refresh()
        vm.await { it.sites.isNotEmpty() }

        var created = false
        vm.createSite("north", "NRTH") { created = true }
        vm.await { it.error != null }

        assertFalse(created)
        assertEquals("A site with that name already exists", vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.sites.size)
    }

    @Test
    fun `renaming replaces the site in place`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Patch) HttpStatusCode.OK to oneBody(site("s1", "North Farm"))
            else HttpStatusCode.OK to listBody(site("s1", "North"))
        }
        vm.refresh()
        vm.await { it.sites.isNotEmpty() }

        val renamed = CompletableDeferred<Unit>()
        vm.rename(vm.uiState.value.sites.single(), "North Farm") { renamed.complete(Unit) }
        renamed.awaitFired()

        assertEquals("Renamed to North Farm", vm.uiState.value.notice)
    }

    @Test
    fun `deactivating a site with active users shows why and keeps it active`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Patch) {
                HttpStatusCode.Conflict to
                    """{"success":false,"message":"This site still has 2 active user(s). Move or deactivate them first."}"""
            } else {
                HttpStatusCode.OK to listBody(site("s1", "North", users = 2))
            }
        }
        vm.refresh()
        vm.await { it.sites.isNotEmpty() }

        vm.setActive(vm.uiState.value.sites.single(), false)
        vm.await { it.error != null }

        assertTrue(vm.uiState.value.error!!.contains("2 active user(s)"))
        assertTrue(vm.uiState.value.sites.single().active)
    }

    @Test
    fun `reactivating updates the card`() = runTest {
        val vm = viewModel { method, _ ->
            if (method == HttpMethod.Patch) HttpStatusCode.OK to oneBody(site("s1", "North", active = true))
            else HttpStatusCode.OK to listBody(site("s1", "North", active = false))
        }
        vm.refresh()
        vm.await { it.sites.isNotEmpty() }

        vm.setActive(vm.uiState.value.sites.single(), true)
        vm.await { it.sites.single().active }

        assertEquals("Reactivated North", vm.uiState.value.notice)
    }

    @Test
    fun `losing the connection asks for a connection and a 403 shows a message`() = runTest {
        val offline = viewModel { _, _ -> throw IOException("offline") }
        offline.refresh()
        offline.await { it.needsConnection }
        assertNull(offline.uiState.value.error)

        val forbidden = viewModel { _, _ -> HttpStatusCode.Forbidden to """{"success":false,"message":"Only an admin can manage sites"}""" }
        forbidden.refresh()
        forbidden.await { it.error != null }
        assertEquals("Only an admin can manage sites", forbidden.uiState.value.error)
    }

    @Test
    fun `a 404 on the list says the server lacks sites`() = runTest {
        val vm = viewModel { _, _ -> HttpStatusCode.NotFound to "Not Found" }

        vm.refresh()
        vm.await { it.error != null }

        assertTrue(vm.uiState.value.error!!.contains("doesn't support sites"))
    }
}
