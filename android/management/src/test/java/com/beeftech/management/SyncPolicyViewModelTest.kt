package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.SyncPolicyUiState
import com.beeftech.management.viewmodel.SyncPolicyViewModel
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
class SyncPolicyViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private fun policy(version: Long, days: String, hours: Int) =
        """{"success":true,"message":"ok","data":{"version":$version,"warningDays":$days,"wipeDay":7,"staleSyncAlertHours":$hours}}"""

    private fun viewModel(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): SyncPolicyViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return SyncPolicyViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    private suspend fun SyncPolicyViewModel.await(predicate: (SyncPolicyUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh fills the form from the server and nothing needs saving`() = runTest {
        val vm = viewModel { HttpStatusCode.OK to policy(1, "[2,4,6]", 48) }

        vm.refresh()
        vm.await { it.saved != null }

        val state = vm.uiState.value
        assertEquals(listOf("2", "4", "6"), listOf(state.day1, state.day2, state.day3))
        assertEquals("48", state.staleHours)
        assertFalse(state.changed)
        assertFalse(state.canSave)
        assertNull(state.validationError)
    }

    @Test
    fun `editing makes the form changed and valid edits can be saved`() = runTest {
        val vm = viewModel { HttpStatusCode.OK to policy(1, "[2,4,6]", 48) }
        vm.refresh()
        vm.await { it.saved != null }

        vm.setDay(0, "1")
        vm.setDay(1, "3")
        vm.setDay(2, "5")
        vm.setStaleHours("24")

        assertTrue(vm.uiState.value.changed)
        assertNull(vm.uiState.value.validationError)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `invalid edits show why and cannot be saved`() = runTest {
        val vm = viewModel { HttpStatusCode.OK to policy(1, "[2,4,6]", 48) }
        vm.refresh()
        vm.await { it.saved != null }

        vm.setDay(2, "7")
        assertTrue(vm.uiState.value.validationError!!.contains("wiped on day 7"))
        assertFalse(vm.uiState.value.canSave)

        vm.setDay(2, "4")
        assertTrue(vm.uiState.value.validationError!!.contains("increase"))

        vm.setDay(2, "")
        assertTrue(vm.uiState.value.validationError!!.contains("all three"))

        vm.setDay(2, "6")
        vm.setStaleHours("5")
        assertTrue(vm.uiState.value.validationError!!.contains("12"))
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `typing keeps only digits and at most two for a day and three for the hours`() = runTest {
        val vm = viewModel { HttpStatusCode.OK to policy(1, "[2,4,6]", 48) }
        vm.refresh()
        vm.await { it.saved != null }

        vm.setDay(0, "a1b2c3")
        vm.setStaleHours("1x2y3z4")

        assertEquals("12", vm.uiState.value.day1)
        assertEquals("123", vm.uiState.value.staleHours)
    }

    @Test
    fun `saving sends the days and hours as numbers and then shows the saved version`() = runTest {
        var body = ""
        val vm = viewModel {
            if (it.method == HttpMethod.Put) {
                body = String(it.body.toByteArray())
                HttpStatusCode.OK to policy(2, "[1,3,5]", 24)
            } else {
                HttpStatusCode.OK to policy(1, "[2,4,6]", 48)
            }
        }
        vm.refresh()
        vm.await { it.saved != null }
        vm.setDay(0, "1"); vm.setDay(1, "3"); vm.setDay(2, "5"); vm.setStaleHours("24")

        vm.save()
        vm.await { it.saved?.version == 2L }

        assertEquals("""{"warningDays":[1,3,5],"staleSyncAlertHours":24}""", body)
        assertFalse(vm.uiState.value.changed)
        assertFalse(vm.uiState.value.saving)
        assertTrue(vm.uiState.value.notice!!.contains("Saved"))
    }

    @Test
    fun `saving with nothing changed or an invalid form sends nothing`() = runTest {
        var puts = 0
        val vm = viewModel {
            if (it.method == HttpMethod.Put) puts++
            HttpStatusCode.OK to policy(1, "[2,4,6]", 48)
        }
        vm.refresh()
        vm.await { it.saved != null }

        vm.save()
        vm.setDay(2, "7")
        vm.save()

        assertEquals(0, puts)
    }

    @Test
    fun `a refusal from the server shows its message and keeps the form`() = runTest {
        val vm = viewModel {
            if (it.method == HttpMethod.Put) {
                HttpStatusCode.BadRequest to """{"success":false,"message":"Warning days must increase, for example 2, 4, 6"}"""
            } else {
                HttpStatusCode.OK to policy(1, "[2,4,6]", 48)
            }
        }
        vm.refresh()
        vm.await { it.saved != null }
        vm.setDay(0, "3")

        vm.save()
        vm.await { it.error != null }

        assertTrue(vm.uiState.value.error!!.contains("must increase"))
        assertEquals("3", vm.uiState.value.day1)
        assertEquals(1L, vm.uiState.value.saved!!.version)
    }

    @Test
    fun `undo puts the form back to what the server has`() = runTest {
        val vm = viewModel { HttpStatusCode.OK to policy(1, "[2,4,6]", 48) }
        vm.refresh()
        vm.await { it.saved != null }
        vm.setDay(0, "1")
        vm.setStaleHours("24")

        vm.reset()

        assertFalse(vm.uiState.value.changed)
        assertEquals("2", vm.uiState.value.day1)
        assertEquals("48", vm.uiState.value.staleHours)
    }

    @Test
    fun `offline asks for a connection, a 403 shows a message and a 404 says the server lacks it`() = runTest {
        val offline = viewModel { throw IOException("offline") }
        offline.refresh()
        offline.await { it.needsConnection }
        assertNull(offline.uiState.value.saved)

        val forbidden = viewModel { HttpStatusCode.Forbidden to """{"success":false,"message":"Only an admin can change the sync policy"}""" }
        forbidden.refresh()
        forbidden.await { it.error != null }
        assertEquals("Only an admin can change the sync policy", forbidden.uiState.value.error)

        val old = viewModel { HttpStatusCode.NotFound to "Not Found" }
        old.refresh()
        old.await { it.error != null }
        assertTrue(old.uiState.value.error!!.contains("doesn't support the sync policy"))
    }
}
