package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.Lockout
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.LoginSecurityUiState
import com.beeftech.management.viewmodel.LoginSecurityViewModel
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
class LoginSecurityViewModelTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun event(id: Long, outcome: String = "BAD_CREDENTIALS") =
        """{"id":$id,"createdAt":$id,"usernameAttempted":"jvdm","userId":"u1","deviceId":"d1","outcome":"$outcome","siteId":"s1"}"""

    private val lockoutsBody =
        """{"success":true,"message":"ok","data":[
        {"username":"jvdm","userId":"u1","failedAttempts":5,"lockedUntil":9999999999999},
        {"username":"ghost","failedAttempts":5,"lockedUntil":9999999999999}]}"""

    private fun eventsBody(vararg ids: Long) = """{"success":true,"message":"ok","data":[${ids.joinToString(",") { event(it) }}]}"""

    private fun viewModel(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): LoginSecurityViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, jsonHeaders)
        }
        return LoginSecurityViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    private suspend fun LoginSecurityViewModel.await(predicate: (LoginSecurityUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh loads who is locked out and the newest attempts`() = runTest {
        val vm = viewModel {
            if (it.url.encodedPath.endsWith("lockouts")) HttpStatusCode.OK to lockoutsBody
            else HttpStatusCode.OK to eventsBody(3, 2, 1)
        }

        vm.refresh()
        vm.await { it.events.isNotEmpty() }

        assertEquals(listOf("jvdm", "ghost"), vm.uiState.value.lockouts.map { it.username })
        assertEquals(listOf(3L, 2L, 1L), vm.uiState.value.events.map { it.id })
        assertFalse(vm.uiState.value.canLoadMore)
    }

    @Test
    fun `a full page offers more, load more pages before the last id, and the outcome filter is sent`() = runTest {
        val urls = mutableListOf<String>()
        val first = (100L downTo 51L).toList().toLongArray()
        val vm = viewModel {
            urls += it.url.toString()
            when {
                it.url.encodedPath.endsWith("lockouts") -> HttpStatusCode.OK to lockoutsBody
                "before=" in it.url.toString() -> HttpStatusCode.OK to eventsBody(50, 49)
                else -> HttpStatusCode.OK to eventsBody(*first)
            }
        }

        vm.selectOutcome("BAD_CREDENTIALS")
        vm.await { it.events.size == 50 }
        assertTrue(vm.uiState.value.canLoadMore)
        vm.loadMore()
        vm.await { it.events.size == 52 }

        assertTrue(urls.any { "outcome=BAD_CREDENTIALS" in it && "before=" !in it })
        assertTrue(urls.last(), "before=51" in urls.last() && "outcome=BAD_CREDENTIALS" in urls.last())
        assertFalse(vm.uiState.value.canLoadMore)
    }

    @Test
    fun `unlocking a real user removes the lock and says so, and a guessed name cannot be unlocked`() = runTest {
        var unlocks = 0
        val vm = viewModel {
            when {
                it.method == HttpMethod.Post -> {
                    unlocks++
                    HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"user_id":"u1","username":"jvdm","role":3,"active":true}}"""
                }
                it.url.encodedPath.endsWith("lockouts") -> HttpStatusCode.OK to lockoutsBody
                else -> HttpStatusCode.OK to eventsBody(1)
            }
        }
        vm.refresh()
        vm.await { it.lockouts.size == 2 }

        vm.unlock(Lockout("ghost", null, 5, 1))
        vm.unlock(vm.uiState.value.lockouts.first { it.username == "jvdm" })
        vm.await { it.lockouts.size == 1 }

        assertEquals(1, unlocks)
        assertEquals(listOf("ghost"), vm.uiState.value.lockouts.map { it.username })
        assertEquals("Unlocked jvdm", vm.uiState.value.notice)
    }

    @Test
    fun `offline asks for a connection and a 403 shows the server's message`() = runTest {
        val offline = viewModel { throw IOException("offline") }
        offline.refresh()
        offline.await { it.needsConnection }

        val forbidden = viewModel { HttpStatusCode.Forbidden to """{"success":false,"message":"Only an admin can read lockouts"}""" }
        forbidden.refresh()
        forbidden.await { it.error != null }
        assertEquals("Only an admin can read lockouts", forbidden.uiState.value.error)
    }
}
