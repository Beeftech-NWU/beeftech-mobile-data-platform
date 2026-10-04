package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.DevicesUiState
import com.beeftech.management.viewmodel.DevicesViewModel
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
class DevicesViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun device(id: String, status: String = "ACTIVE", reason: String? = null) =
        """{"deviceId":"$id","model":"Pixel","appVersion":"1.0","status":"$status","firstSeenAt":1,"lastSeenAt":2,
        "lastUsername":"jvdm","siteId":"s1","revokeReason":${reason?.let { "\"$it\"" }},"boundUsernames":["jvdm"],"extra":1}"""

    private fun listBody(vararg devices: String) = """{"success":true,"message":"ok","data":[${devices.joinToString(",")}]}"""
    private fun oneBody(device: String) = """{"success":true,"message":"ok","data":$device}"""

    private fun viewModel(
        canManage: Boolean,
        handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>
    ): DevicesViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, jsonHeaders)
        }
        return DevicesViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }),
            canManage
        )
    }

    private suspend fun DevicesViewModel.await(predicate: (DevicesUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the phones and the status filter is sent to the server`() = runTest {
        val urls = mutableListOf<String>()
        val vm = viewModel(canManage = true) { urls += it.url.toString(); HttpStatusCode.OK to listBody(device("d1")) }

        vm.refresh()
        vm.await { it.devices.isNotEmpty() }
        vm.selectStatus("REVOKED")
        vm.await { urls.size == 2 && !it.loading }

        assertEquals("d1", vm.uiState.value.devices.single().deviceId)
        assertTrue(urls[0], "status=" !in urls[0])
        assertTrue(urls[1], "status=REVOKED" in urls[1])
    }

    @Test
    fun `an admin blocks a phone with a reason and the card shows it blocked`() = runTest {
        var body = ""
        val vm = viewModel(canManage = true) {
            if (it.method == HttpMethod.Post) {
                body = String(it.body.toByteArray())
                HttpStatusCode.OK to oneBody(device("d1", "REVOKED", "Stolen"))
            } else {
                HttpStatusCode.OK to listBody(device("d1"))
            }
        }
        vm.refresh()
        vm.await { it.devices.isNotEmpty() }

        var done = false
        vm.block(vm.uiState.value.devices.single(), "  Stolen  ") { done = true }
        vm.await { it.devices.single().isRevoked }

        assertTrue(done)
        assertEquals("""{"reason":"Stolen"}""", body)
        assertEquals("Stolen", vm.uiState.value.devices.single().revokeReason)
        assertEquals("Blocked Pixel", vm.uiState.value.notice)
    }

    @Test
    fun `unblocking while filtering on blocked phones drops the card from the list`() = runTest {
        val vm = viewModel(canManage = true) {
            if (it.method == HttpMethod.Post) HttpStatusCode.OK to oneBody(device("d1", "ACTIVE"))
            else HttpStatusCode.OK to listBody(device("d1", "REVOKED", "Lost"))
        }
        vm.selectStatus("REVOKED")
        vm.await { it.devices.isNotEmpty() }

        vm.unblock(vm.uiState.value.devices.single(), "Found it")
        vm.await { it.devices.isEmpty() }

        assertEquals("Unblocked Pixel", vm.uiState.value.notice)
    }

    @Test
    fun `a manager cannot block or unblock and no request is sent`() = runTest {
        var posts = 0
        val vm = viewModel(canManage = false) {
            if (it.method == HttpMethod.Post) posts++
            HttpStatusCode.OK to listBody(device("d1"))
        }
        vm.refresh()
        vm.await { it.devices.isNotEmpty() }

        vm.block(vm.uiState.value.devices.single(), "nope")
        vm.unblock(vm.uiState.value.devices.single(), "nope")

        assertEquals(0, posts)
        assertFalse(vm.canManage)
    }

    @Test
    fun `an already-blocked phone shows the server's message and stays as it was`() = runTest {
        val vm = viewModel(canManage = true) {
            if (it.method == HttpMethod.Post) {
                HttpStatusCode.Conflict to """{"success":false,"message":"That device is already revoked"}"""
            } else {
                HttpStatusCode.OK to listBody(device("d1"))
            }
        }
        vm.refresh()
        vm.await { it.devices.isNotEmpty() }

        var done = false
        vm.block(vm.uiState.value.devices.single(), "again") { done = true }
        vm.await { it.error != null }

        assertFalse(done)
        assertEquals("That device is already revoked", vm.uiState.value.error)
        assertFalse(vm.uiState.value.devices.single().isRevoked)
    }

    @Test
    fun `offline asks for a connection, 403 and 404 show messages`() = runTest {
        val offline = viewModel(canManage = true) { throw IOException("offline") }
        offline.refresh()
        offline.await { it.needsConnection }
        assertNull(offline.uiState.value.error)

        val forbidden = viewModel(canManage = false) { HttpStatusCode.Forbidden to """{"success":false,"message":"Forbidden"}""" }
        forbidden.refresh()
        forbidden.await { it.error != null }
        assertEquals("Forbidden", forbidden.uiState.value.error)

        val old = viewModel(canManage = false) { HttpStatusCode.NotFound to "Not Found" }
        old.refresh()
        old.await { it.error != null }
        assertTrue(old.uiState.value.error!!.contains("doesn't support phones"))
    }
}
