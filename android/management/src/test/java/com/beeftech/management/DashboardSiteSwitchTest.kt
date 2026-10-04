package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.DashboardUiState
import com.beeftech.management.viewmodel.DashboardViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/* Admins can narrow the dashboard to one site; managers always get their own. */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardSiteSwitchTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val sitesBody =
        """{"success":true,"message":"ok","data":[
        {"siteId":"s1","name":"North","active":true,"createdAt":1,"activeUserCount":1},
        {"siteId":"s2","name":"South","active":true,"createdAt":1,"activeUserCount":1}]}"""

    private fun summaryBody(siteId: String?, siteName: String?) =
        """{"success":true,"message":"ok","data":{"siteId":${siteId?.let { "\"$it\"" }},"siteName":${siteName?.let { "\"$it\"" }},
        "calves":{"total":4,"last7Days":2}}}"""

    private fun viewModel(canSwitchSite: Boolean, urls: MutableList<String>): DashboardViewModel {
        val engine = MockEngine { request: HttpRequestData ->
            urls += request.url.toString()
            val path = request.url.encodedPath
            val body = when {
                path.endsWith("/api/sites") -> sitesBody
                request.url.parameters["siteId"] == "s2" -> summaryBody("s2", "South")
                else -> summaryBody(null, null)
            }
            respond(body, HttpStatusCode.OK, jsonHeaders)
        }
        return DashboardViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }),
            canSwitchSite = canSwitchSite
        )
    }

    private suspend fun DashboardViewModel.await(predicate: (DashboardUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an admin loads the sites and switches the summary to one site and back`() = runTest {
        val urls = mutableListOf<String>()
        val vm = viewModel(canSwitchSite = true, urls = urls)

        vm.refresh()
        vm.await { it.summary != null && it.sites.size == 2 }
        assertNull(vm.uiState.value.selectedSiteId)
        assertNull(vm.uiState.value.summary!!.siteName)

        vm.selectSite("s2")
        vm.await { it.summary?.siteName == "South" }
        assertEquals("s2", vm.uiState.value.selectedSiteId)
        assertTrue(urls.any { it.endsWith("api/dashboard/summary?siteId=s2") })

        vm.selectSite(null)
        vm.await { it.summary?.siteName == null && it.selectedSiteId == null && !it.loading }
        assertTrue(urls.last { "dashboard" in it }.endsWith("api/dashboard/summary"))
    }

    @Test
    fun `a manager never loads sites or sends a site id`() = runTest {
        val urls = mutableListOf<String>()
        val vm = viewModel(canSwitchSite = false, urls = urls)

        vm.refresh()
        vm.await { it.summary != null }
        vm.selectSite("s2")
        withContext(Dispatchers.Default) { kotlinx.coroutines.delay(200) }

        assertEquals(emptyList<Any>(), vm.uiState.value.sites)
        assertNull(vm.uiState.value.selectedSiteId)
        assertTrue(urls.none { "api/sites" in it || "siteId" in it })
    }
}
