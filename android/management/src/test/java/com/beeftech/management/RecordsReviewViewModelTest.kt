package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.RecordsReviewUiState
import com.beeftech.management.viewmodel.RecordsReviewViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
class RecordsReviewViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun record(id: String, label: String, voidedAt: Long? = null, reason: String? = null) =
        """{"type":"mortalities","id":"$id","label":"$label","submittedByUsername":"jvdm","siteId":"s1"""" +
            (voidedAt?.let { ""","voidedAt":$it,"voidReason":"$reason"""" } ?: "") + "}"

    private fun list(vararg records: String) =
        HttpStatusCode.OK to """{"success":true,"message":"ok","data":[${records.joinToString(",")}]}"""

    private fun viewModel(handler: (HttpMethod, String, String) -> Pair<HttpStatusCode, String>): RecordsReviewViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request.method, request.url.encodedPath, request.url.encodedQuery)
            respond(body, status, jsonHeaders)
        }
        return RecordsReviewViewModel(
            ManagementApiClient(
                provider,
                "http://test-host/",
                HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
            )
        )
    }

    /* The client does its I/O off the test dispatcher, so wait for the state instead of assuming it. */
    private suspend fun RecordsReviewViewModel.await(predicate: (RecordsReviewUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the first type and asks for voided records too`() = runTest {
        var path = ""
        var query = ""
        val vm = viewModel { _, p, q ->
            path = p
            query = q
            list(record("g-1", "A-1 - Bloat"))
        }

        vm.refresh()
        vm.await { it.records.isNotEmpty() }

        assertEquals("/api/records/calf-registrations", path)
        assertEquals("includeVoided=true", query)
        assertEquals("g-1", vm.uiState.value.records.single().id)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `selecting another type loads that type`() = runTest {
        val paths = mutableListOf<String>()
        val vm = viewModel { _, p, _ ->
            paths += p
            list(record("g-1", "x"))
        }

        vm.selectType("treatments")
        vm.await { it.records.isNotEmpty() }

        assertEquals("treatments", vm.uiState.value.type)
        assertEquals("/api/records/treatments", paths.last())
    }

    @Test
    fun `show voided filters the list without a new request`() = runTest {
        var requests = 0
        val vm = viewModel { _, _, _ ->
            requests++
            list(record("g-1", "kept"), record("g-2", "gone", voidedAt = 5, reason = "dup"))
        }
        vm.refresh()
        vm.await { it.records.size == 2 }

        vm.setShowVoided(false)

        assertEquals(listOf("g-1"), vm.uiState.value.visibleRecords.map { it.id })
        vm.setShowVoided(true)
        assertEquals(2, vm.uiState.value.visibleRecords.size)
        assertEquals(1, requests)
    }

    @Test
    fun `voiding marks the record voided with the reason`() = runTest {
        var body: String? = null
        val vm = viewModel { method, path, _ ->
            if (method == HttpMethod.Get) {
                list(record("g-1", "A-1 - Bloat"))
            } else {
                body = path
                HttpStatusCode.OK to
                    """{"success":true,"message":"ok","data":{"entityType":"MORTALITY","entityId":"g-1","voidedAt":99}}"""
            }
        }
        vm.refresh()
        vm.await { it.records.isNotEmpty() }

        vm.voidRecord(vm.uiState.value.records.single(), "  Wrong animal ")
        vm.await { it.notice != null }

        val voided = vm.uiState.value.records.single()
        assertTrue(voided.isVoided)
        assertEquals(99L, voided.voidedAt)
        assertEquals("Wrong animal", voided.voidReason)
        assertEquals("/api/records/mortalities/g-1/void", body)
        assertEquals("Voided A-1 - Bloat", vm.uiState.value.notice)
    }

    @Test
    fun `a rejected void shows the server's message`() = runTest {
        val vm = viewModel { method, _, _ ->
            if (method == HttpMethod.Get) {
                list(record("g-1", "x"))
            } else {
                HttpStatusCode.Conflict to """{"success":false,"message":"Record is already voided"}"""
            }
        }
        vm.refresh()
        vm.await { it.records.isNotEmpty() }

        vm.voidRecord(vm.uiState.value.records.single(), "dup")
        vm.await { it.error != null }

        assertEquals("Record is already voided", vm.uiState.value.error)
    }

    @Test
    fun `without a connection it says so and keeps the old list`() = runTest {
        var online = true
        val engine = MockEngine {
            if (!online) throw IOException("offline")
            respond(list(record("g-1", "x")).second, HttpStatusCode.OK, jsonHeaders)
        }
        val vm = RecordsReviewViewModel(
            ManagementApiClient(
                provider,
                "http://test-host/",
                HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
            )
        )
        vm.refresh()
        vm.await { it.records.isNotEmpty() }
        online = false

        vm.refresh()
        vm.await { it.needsConnection }

        assertEquals(1, vm.uiState.value.records.size)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `a 404 on the list says the server lacks records review`() = runTest {
        val vm = viewModel { _, _, _ -> HttpStatusCode.NotFound to "Not Found" }

        vm.refresh()
        vm.await { it.error != null }

        assertTrue(vm.uiState.value.error!!.contains("doesn't support records review"))
    }
}
