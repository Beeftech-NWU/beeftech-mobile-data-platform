package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ReportFormat
import com.beeftech.management.data.ReportKind
import com.beeftech.management.viewmodel.ReportsUiState
import com.beeftech.management.viewmodel.ReportsViewModel
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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    private val sitesBody =
        """{"success":true,"message":"ok","data":[
        {"siteId":"s1","name":"North","active":true,"createdAt":1,"activeUserCount":1}]}"""

    private fun reportBody(title: String = "Mortality report", siteName: String? = null) =
        """{"success":true,"message":"ok","data":{"report":"mortality","title":"$title",
        "siteName":${siteName?.let { "\"$it\"" }},"from":1,"to":2,"generatedAt":2,
        "summary":[{"label":"Mortalities","value":"3"}],"columns":["Cause","Mortalities"],
        "rows":[["Bloat","3"]],"footer":"note","somethingNew":1}}"""

    private fun viewModel(
        canSwitchSite: Boolean = false,
        requests: MutableList<HttpRequestData> = mutableListOf(),
        respondWith: (HttpRequestData) -> Pair<HttpStatusCode, ByteArray> = { _ ->
            HttpStatusCode.OK to reportBody().toByteArray()
        }
    ): ReportsViewModel {
        val engine = MockEngine { request ->
            requests += request
            if (request.url.encodedPath.endsWith("/api/sites")) {
                respond(sitesBody, HttpStatusCode.OK, jsonHeaders)
            } else {
                val (status, body) = respondWith(request)
                respond(body, status, jsonHeaders)
            }
        }
        return ReportsViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }),
            canSwitchSite = canSwitchSite,
            now = { now }
        )
    }

    private suspend fun ReportsViewModel.await(predicate: (ReportsUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads the report for the default last 30 days`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val vm = viewModel(requests = requests)

        vm.refresh()
        vm.await { it.report != null }

        val request = requests.single()
        assertEquals("http://test-host/api/reports/mortality", request.url.toString().substringBefore("?"))
        assertEquals("Bearer tok", request.headers[HttpHeaders.Authorization])
        assertEquals(now.toString(), request.url.parameters["to"])
        assertEquals((now - 30 * day).toString(), request.url.parameters["from"])
        assertNull(request.url.parameters["siteId"])
        assertNull(request.url.parameters["bucket"])
        assertEquals("3", vm.uiState.value.report!!.summary.single().value)
        assertEquals(listOf("Bloat", "3"), vm.uiState.value.report!!.rows.single())
    }

    @Test
    fun `changing the range and the report kind reloads, and the bucket is only sent for calf registrations`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val vm = viewModel(requests = requests)

        vm.refresh()
        vm.await { it.report != null }
        vm.selectRange(7)
        vm.await { it.report != null && !it.loading && it.rangeDays == 7 }
        assertEquals((now - 7 * day).toString(), requests.last().url.parameters["from"])

        vm.selectKind(ReportKind.CALF_REGISTRATIONS)
        vm.await { it.report != null && !it.loading && it.kind == ReportKind.CALF_REGISTRATIONS }
        assertTrue(requests.last().url.encodedPath.endsWith("/api/reports/calf-registrations"))
        assertEquals("week", requests.last().url.parameters["bucket"])

        val before = requests.size
        vm.selectBucket("month")
        vm.await { it.bucket == "month" && !it.loading }
        assertEquals("month", requests.last().url.parameters["bucket"])
        assertTrue(requests.size > before)
    }

    @Test
    fun `an admin loads sites and narrows to one, a manager never sends a site`() = runTest {
        val adminRequests = mutableListOf<HttpRequestData>()
        val admin = viewModel(canSwitchSite = true, requests = adminRequests)
        admin.refresh()
        admin.await { it.report != null && it.sites.size == 1 }
        admin.selectSite("s1")
        admin.await { it.selectedSiteId == "s1" && !it.loading && it.report != null }
        assertEquals("s1", adminRequests.last().url.parameters["siteId"])

        val managerRequests = mutableListOf<HttpRequestData>()
        val manager = viewModel(canSwitchSite = false, requests = managerRequests)
        manager.refresh()
        manager.await { it.report != null }
        manager.selectSite("s1")
        withContext(Dispatchers.Default) { kotlinx.coroutines.delay(200) }
        assertNull(manager.uiState.value.selectedSiteId)
        assertTrue(managerRequests.none { "api/sites" in it.url.toString() || "siteId" in it.url.toString() })
    }

    @Test
    fun `a dropped connection keeps the last report and asks for a connection`() = runTest {
        var fail = false
        val engine = MockEngine { _ ->
            if (fail) throw IOException("offline")
            respond(reportBody(), HttpStatusCode.OK, jsonHeaders)
        }
        val vm = ReportsViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }),
            now = { now }
        )

        vm.refresh()
        vm.await { it.report != null }
        fail = true
        vm.refresh()
        vm.await { it.needsConnection }

        assertNotNull(vm.uiState.value.report)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `rejections and forbidden answers show the server message`() = runTest {
        val vm = viewModel { _ ->
            HttpStatusCode.Forbidden to """{"success":false,"message":"Managers can only view their own site"}""".toByteArray()
        }

        vm.refresh()
        vm.await { it.error != null }

        assertEquals("Managers can only view their own site", vm.uiState.value.error)
        assertNull(vm.uiState.value.report)
    }

    @Test
    fun `export downloads the file once and exportHandled clears it`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val pdf = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x00, 0x7f, -1, -2)
        val vm = viewModel(requests = requests) { request ->
            if (request.url.parameters["format"] == "pdf") HttpStatusCode.OK to pdf
            else HttpStatusCode.OK to reportBody().toByteArray()
        }

        vm.refresh()
        vm.await { it.report != null }
        vm.export(ReportFormat.PDF)
        val state = vm.await { it.exported != null }

        val file = state.exported!!
        assertEquals("application/pdf", file.mimeType)
        assertEquals("beeftech-mortality-2027-01-15.pdf", file.name)
        /* Binary bytes must survive untouched. */
        assertArrayEquals(pdf, file.bytes)
        assertEquals("pdf", requests.last().url.parameters["format"])
        assertTrue(!vm.uiState.value.exporting)

        vm.exportHandled()
        assertNull(vm.uiState.value.exported)
    }

    @Test
    fun `a failed export reports the error and shares nothing`() = runTest {
        val vm = viewModel { request ->
            if (request.url.parameters["format"] == null) HttpStatusCode.OK to reportBody().toByteArray()
            else HttpStatusCode.BadRequest to """{"success":false,"message":"Range is limited to 366 days"}""".toByteArray()
        }

        vm.refresh()
        vm.await { it.report != null }
        vm.export(ReportFormat.CSV)
        vm.await { it.error != null }

        assertEquals("Range is limited to 366 days", vm.uiState.value.error)
        assertNull(vm.uiState.value.exported)
        assertTrue(!vm.uiState.value.exporting)
    }
}
