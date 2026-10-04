package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.ui.auditChanges
import com.beeftech.management.viewmodel.AuditLogUiState
import com.beeftech.management.viewmodel.AuditLogViewModel
import com.beeftech.management.viewmodel.AuditRange
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuditLogViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val now = 10_000_000_000L

    private fun entry(id: Long, action: String = "VOID") =
        """{"id":$id,"action":"$action","entityType":"MORTALITY","entityId":"g-$id","reason":"dup",
        "actorUserId":"u1","actorUsername":"fmanager","actorRole":2,"siteId":"s1","createdAt":$id}"""

    private fun page(vararg ids: Long) =
        """{"success":true,"message":"ok","data":[${ids.joinToString(",") { entry(it) }}]}"""

    private fun viewModel(handler: (HttpRequestData) -> Pair<HttpStatusCode, String>): AuditLogViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, jsonHeaders)
        }
        return AuditLogViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }),
            now = { now }
        )
    }

    /* The client does its I/O off the test dispatcher, so wait for the state instead of assuming it. */
    private suspend fun AuditLogViewModel.await(predicate: (AuditLogUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the newest entries for the default week`() = runTest {
        var url = ""
        val vm = viewModel { url = it.url.toString(); HttpStatusCode.OK to page(3, 2, 1) }

        vm.refresh()
        vm.await { it.entries.isNotEmpty() }

        assertEquals(listOf(3L, 2L, 1L), vm.uiState.value.entries.map { it.id })
        assertFalse(vm.uiState.value.canLoadMore)
        assertEquals("fmanager", vm.uiState.value.entries.first().actorUsername)
        val expectedFrom = now - AuditRange.LAST_WEEK.millis!!
        assertTrue(url, "from=$expectedFrom" in url)
        assertTrue(url, "action=" !in url)
    }

    @Test
    fun `a full page offers more and load more asks for entries before the last id`() = runTest {
        val urls = mutableListOf<String>()
        val firstPage = page(*(100L downTo 51L).toList().toLongArray())
        val vm = viewModel {
            urls += it.url.toString()
            if ("before=" in urls.last()) HttpStatusCode.OK to page(50, 49) else HttpStatusCode.OK to firstPage
        }

        vm.refresh()
        vm.await { it.entries.size == 50 }
        assertTrue(vm.uiState.value.canLoadMore)

        vm.loadMore()
        vm.await { it.entries.size == 52 }

        assertTrue(urls.last(), "before=51" in urls.last())
        assertEquals(100L, vm.uiState.value.entries.first().id)
        assertEquals(49L, vm.uiState.value.entries.last().id)
        /* A short page means there is nothing older. */
        assertFalse(vm.uiState.value.canLoadMore)
    }

    @Test
    fun `changing the action or range reloads from the top with the new filter`() = runTest {
        val urls = mutableListOf<String>()
        val vm = viewModel { urls += it.url.toString(); HttpStatusCode.OK to page(2, 1) }

        vm.refresh()
        vm.await { it.entries.size == 2 }
        vm.selectAction("USER_CREATE")
        vm.await { urls.size == 2 && !it.loading && it.entries.size == 2 }
        vm.selectRange(AuditRange.ALL)
        vm.await { urls.size == 3 && !it.loading && it.entries.size == 2 }

        assertTrue(urls[1], "action=USER_CREATE" in urls[1])
        assertTrue(urls[2], "action=USER_CREATE" in urls[2])
        assertTrue(urls[2], "from=" !in urls[2])
        assertTrue(urls[2], "before=" !in urls[2])
    }

    @Test
    fun `losing the connection keeps the entries and asks for a connection`() = runTest {
        var online = true
        val engine = MockEngine {
            if (!online) throw IOException("offline")
            respond(page(2, 1), HttpStatusCode.OK, jsonHeaders)
        }
        val vm = AuditLogViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )

        vm.refresh()
        vm.await { it.entries.size == 2 }
        online = false
        vm.refresh()
        vm.await { it.needsConnection }

        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `a 403 and a 404 show a message instead of entries`() = runTest {
        val forbidden = viewModel { HttpStatusCode.Forbidden to """{"success":false,"message":"Forbidden"}""" }
        forbidden.refresh()
        forbidden.await { it.error != null }
        assertEquals("Forbidden", forbidden.uiState.value.error)

        val missing = viewModel { HttpStatusCode.NotFound to "Not Found" }
        missing.refresh()
        missing.await { it.error != null }
        assertTrue(missing.uiState.value.error!!.contains("doesn't support the audit log"))
    }

    @Test
    fun `details are shown as plain changes`() {
        assertEquals("role 3->2", auditChanges("""{"role":"3->2"}"""))
        assertEquals("role 3->2, siteId a->b", auditChanges("""{"role":"3->2","siteId":"a->b"}"""))
        assertNull(auditChanges(null))
        assertNull(auditChanges("{}"))
    }
}
