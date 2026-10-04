package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.ReferenceDataUiState
import com.beeftech.management.viewmodel.ReferenceDataViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
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
class ReferenceDataViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val snapshotBody = """{"success":true,"message":"ok","data":{"version":3,
        "diseases":[{"id":1,"name":"Anthrax","active":true},{"id":2,"name":"Rabies","active":false}],
        "treatmentTypes":[{"id":5,"name":"Vaccination","active":true}],
        "costTypes":[{"code":"TRANSPORT","displayName":"Transport","sortOrder":0,"active":true},
                     {"code":"TREATMENT","displayName":"Treatment","sortOrder":4,"active":true}]}}"""

    private fun change(kind: String, id: String, name: String, active: Boolean, version: Long, sortOrder: Int? = null) =
        """{"success":true,"message":"ok","data":{"version":$version,"item":{"kind":"$kind","id":"$id","name":"$name","active":$active
        ${sortOrder?.let { ",\"sortOrder\":$it" } ?: ""}}}}"""

    private fun viewModel(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): ReferenceDataViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return ReferenceDataViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    private suspend fun ReferenceDataViewModel.await(predicate: (ReferenceDataUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads every kind and the selected kind filters the list`() = runTest {
        val vm = viewModel { HttpStatusCode.OK to snapshotBody }

        vm.refresh()
        vm.await { it.entries.isNotEmpty() }

        assertEquals(3L, vm.uiState.value.version)
        assertEquals(listOf("Anthrax", "Rabies"), vm.uiState.value.visibleEntries.map { it.name })
        assertFalse(vm.uiState.value.visibleEntries.last().active)

        vm.selectKind("cost-types")
        assertEquals(listOf("TRANSPORT", "TREATMENT"), vm.uiState.value.visibleEntries.map { it.id })
        vm.selectKind("treatment-types")
        assertEquals(listOf("Vaccination"), vm.uiState.value.visibleEntries.map { it.name })
    }

    @Test
    fun `adding a disease sends a name and shows it in order with the new version`() = runTest {
        var body = ""
        val vm = viewModel {
            if (it.method == HttpMethod.Post) {
                body = String(it.body.toByteArray())
                HttpStatusCode.Created to change("diseases", "9", "Pinkeye", true, 4)
            } else {
                HttpStatusCode.OK to snapshotBody
            }
        }
        vm.refresh()
        vm.await { it.entries.isNotEmpty() }

        val added = CompletableDeferred<Unit>()
        vm.add("  Pinkeye ") { added.complete(Unit) }
        added.awaitFired()

        assertEquals("""{"name":"Pinkeye"}""", body)
        assertEquals(listOf("Anthrax", "Pinkeye", "Rabies"), vm.uiState.value.visibleEntries.map { it.name })
        assertEquals("Added Pinkeye", vm.uiState.value.notice)
    }

    @Test
    fun `adding a cost type sends a code and a display name`() = runTest {
        var body = ""
        var path = ""
        val vm = viewModel {
            if (it.method == HttpMethod.Post) {
                path = it.url.encodedPath
                body = String(it.body.toByteArray())
                HttpStatusCode.Created to change("cost-types", "AUCTION", "Auction fees", true, 4, sortOrder = 9)
            } else {
                HttpStatusCode.OK to snapshotBody
            }
        }
        vm.refresh()
        vm.await { it.entries.isNotEmpty() }
        vm.selectKind("cost-types")

        vm.add("Auction fees", code = "AUCTION")
        vm.await { it.version == 4L }

        assertEquals("/api/reference-data/cost-types", path)
        assertEquals("""{"code":"AUCTION","displayName":"Auction fees"}""", body)
        assertEquals(listOf("TRANSPORT", "TREATMENT", "AUCTION"), vm.uiState.value.visibleEntries.map { it.id })
    }

    @Test
    fun `a duplicate shows the server's message and adds nothing`() = runTest {
        val vm = viewModel {
            if (it.method == HttpMethod.Post) {
                HttpStatusCode.Conflict to """{"success":false,"message":"That disease already exists"}"""
            } else {
                HttpStatusCode.OK to snapshotBody
            }
        }
        vm.refresh()
        vm.await { it.entries.isNotEmpty() }

        var added = false
        vm.add("anthrax") { added = true }
        vm.await { it.error != null }

        assertFalse(added)
        assertEquals("That disease already exists", vm.uiState.value.error)
        assertEquals(2, vm.uiState.value.visibleEntries.size)
    }

    @Test
    fun `turning a value off and on updates the entry and the version`() = runTest {
        val paths = mutableListOf<String>()
        var next = false
        val vm = viewModel {
            if (it.method == HttpMethod.Patch) {
                paths += it.url.encodedPath + " " + String(it.body.toByteArray())
                HttpStatusCode.OK to change("diseases", "1", "Anthrax", next, if (next) 5 else 4)
            } else {
                HttpStatusCode.OK to snapshotBody
            }
        }
        vm.refresh()
        vm.await { it.entries.isNotEmpty() }

        vm.setActive(vm.uiState.value.visibleEntries.first { it.name == "Anthrax" }, false)
        vm.await { it.version == 4L }
        assertFalse(vm.uiState.value.visibleEntries.first { it.name == "Anthrax" }.active)
        assertEquals("Turned off Anthrax", vm.uiState.value.notice)

        next = true
        vm.setActive(vm.uiState.value.visibleEntries.first { it.name == "Anthrax" }, true)
        vm.await { it.version == 5L }
        assertTrue(vm.uiState.value.visibleEntries.first { it.name == "Anthrax" }.active)

        assertEquals(listOf("/api/reference-data/diseases/1 {\"active\":false}", "/api/reference-data/diseases/1 {\"active\":true}"), paths)
    }

    @Test
    fun `the protected Treatment cost type shows the server's refusal and stays on`() = runTest {
        val vm = viewModel {
            if (it.method == HttpMethod.Patch) {
                HttpStatusCode.Conflict to """{"success":false,"message":"The Treatment cost type can't be turned off"}"""
            } else {
                HttpStatusCode.OK to snapshotBody
            }
        }
        vm.refresh()
        vm.await { it.entries.isNotEmpty() }
        vm.selectKind("cost-types")

        vm.setActive(vm.uiState.value.visibleEntries.first { it.id == "TREATMENT" }, false)
        vm.await { it.error != null }

        assertTrue(vm.uiState.value.error!!.contains("can't be turned off"))
        assertTrue(vm.uiState.value.visibleEntries.first { it.id == "TREATMENT" }.active)
    }

    @Test
    fun `offline asks for a connection, 403 shows a message and 404 says the server lacks it`() = runTest {
        val offline = viewModel { throw IOException("offline") }
        offline.refresh()
        offline.await { it.needsConnection }
        assertNull(offline.uiState.value.error)

        val forbidden = viewModel { HttpStatusCode.Forbidden to """{"success":false,"message":"Only an admin can change reference data"}""" }
        forbidden.refresh()
        forbidden.await { it.error != null }
        assertEquals("Only an admin can change reference data", forbidden.uiState.value.error)

        val old = viewModel { HttpStatusCode.NotFound to "Not Found" }
        old.refresh()
        old.await { it.error != null }
        assertTrue(old.uiState.value.error!!.contains("doesn't support reference data"))
    }
}
