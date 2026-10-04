package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.SyncSecurityUiState
import com.beeftech.management.viewmodel.SyncSecurityViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSecurityViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun event(id: Long, type: String = "SYNC_WARNING") =
        """{"id":$id,"deviceId":"d1","userId":"u1","username":"jvdm","siteId":"s1","eventType":"$type","eventTime":$id,"warningDay":2,"pendingCount":3}"""

    private fun eventsBody(vararg ids: Long) =
        """{"success":true,"message":"ok","data":[${ids.joinToString(",") { event(it) }}]}"""

    private val lockedBody =
        """{"success":true,"message":"ok","data":[
        {"userId":"u1","username":"jvdm","siteId":"s1","deviceId":"d1","lockedAt":1000,"reason":"Day 7"},
        {"userId":"u2","username":"other","deviceId":"d2","lockedAt":2000}]}"""

    private fun viewModel(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): SyncSecurityViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, jsonHeaders)
        }
        return SyncSecurityViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    private suspend fun SyncSecurityViewModel.await(predicate: (SyncSecurityUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the locked accounts and the newest events`() = runTest {
        val vm = viewModel {
            if (it.url.encodedPath.endsWith("/locked")) HttpStatusCode.OK to lockedBody
            else HttpStatusCode.OK to eventsBody(3, 2, 1)
        }

        vm.refresh()
        val state = vm.await { !it.loading && it.events.isNotEmpty() }

        assertEquals(listOf("jvdm", "other"), state.locked.map { it.username })
        assertEquals(listOf(3L, 2L, 1L), state.events.map { it.id })
        assertFalse(state.needsConnection)
    }

    @Test
    fun `choosing an event type asks the server for that type`() = runTest {
        val urls = mutableListOf<String>()
        val vm = viewModel {
            urls += it.url.toString()
            if (it.url.encodedPath.endsWith("/locked")) HttpStatusCode.OK to lockedBody
            else HttpStatusCode.OK to eventsBody(1)
        }

        vm.selectEventType("DAY_7_WIPE")
        vm.await { !it.loading && it.events.isNotEmpty() }

        assertTrue(urls.any { "eventType=DAY_7_WIPE" in it })
    }

    @Test
    fun `clearing a lock calls the server and removes the account from the list`() = runTest {
        var cleared: String? = null
        val vm = viewModel {
            when {
                it.method == HttpMethod.Post && it.url.encodedPath.endsWith("/clear-sync-lock") -> {
                    cleared = it.url.encodedPath
                    HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"userId":"u1","clearedAt":9000}}"""
                }
                it.url.encodedPath.endsWith("/locked") -> HttpStatusCode.OK to lockedBody
                else -> HttpStatusCode.OK to eventsBody()
            }
        }
        vm.refresh()
        val loaded = vm.await { !it.loading && it.locked.isNotEmpty() }

        vm.clearLock(loaded.locked.first())
        val state = vm.await { it.notice != null }

        assertEquals("/api/users/u1/clear-sync-lock", cleared)
        assertEquals(listOf("other"), state.locked.map { it.username })
    }

    @Test
    fun `a refused clear keeps the account listed and shows the message`() = runTest {
        val vm = viewModel {
            when {
                it.method == HttpMethod.Post -> HttpStatusCode.Forbidden to """{"success":false,"message":"Only an admin can do this"}"""
                it.url.encodedPath.endsWith("/locked") -> HttpStatusCode.OK to lockedBody
                else -> HttpStatusCode.OK to eventsBody()
            }
        }
        vm.refresh()
        val loaded = vm.await { !it.loading && it.locked.isNotEmpty() }

        vm.clearLock(loaded.locked.first())
        val state = vm.await { it.error != null }

        assertEquals("Only an admin can do this", state.error)
        assertEquals(2, state.locked.size)
    }

    @Test
    fun `load more pages with before and appends`() = runTest {
        val urls = mutableListOf<String>()
        val vm = viewModel {
            urls += it.url.toString()
            when {
                it.url.encodedPath.endsWith("/locked") -> HttpStatusCode.OK to lockedBody
                "before=" in it.url.toString() -> HttpStatusCode.OK to eventsBody(1)
                else -> HttpStatusCode.OK to eventsBody(*LongArray(ManagementApiClient.AUDIT_PAGE_SIZE) { (100 - it).toLong() })
            }
        }
        vm.refresh()
        vm.await { !it.loading && it.canLoadMore }

        vm.loadMore()
        val state = vm.await { it.events.size == ManagementApiClient.AUDIT_PAGE_SIZE + 1 }

        assertTrue(urls.any { "before=${100 - ManagementApiClient.AUDIT_PAGE_SIZE + 1}" in it })
        assertFalse(state.canLoadMore)
    }

    @Test
    fun `no connection shows the needs-connection notice, not an error`() = runTest {
        val vm = viewModel { throw IOException("offline") }

        vm.refresh()
        val state = vm.await { it.needsConnection }

        assertEquals(null, state.error)
    }

    @Test
    fun `an old server without the endpoint says so`() = runTest {
        val vm = viewModel { HttpStatusCode.NotFound to "Not Found" }

        vm.refresh()
        val state = vm.await { it.error != null }

        assertTrue("Update the server" in state.error!!)
    }
}
