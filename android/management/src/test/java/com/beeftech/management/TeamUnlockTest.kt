package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.TeamUiState
import com.beeftech.management.viewmodel.TeamViewModel
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
import org.junit.Before
import org.junit.Test

/* "Unlock sign-in" on a team member lifts a lockout without changing their PIN. */
@OptIn(ExperimentalCoroutinesApi::class)
class TeamUnlockTest {

    private val provider = object : TokenProvider {
        override suspend fun token(): String? = "tok"
    }

    private val member = """{"user_id":"u1","username":"jvdm","role":3,"site_id":"s1","active":true}"""

    private fun viewModel(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): TeamViewModel {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return TeamViewModel(
            ManagementApiClient(provider, "http://test-host/", HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            })
        )
    }

    private suspend fun TeamViewModel.await(predicate: (TeamUiState) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first(predicate) }
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `unlocking posts to the member's unlock path and says they can sign in again`() = runTest {
        val posts = mutableListOf<String>()
        val vm = viewModel {
            if (it.method == HttpMethod.Post) {
                posts += it.url.encodedPath
                HttpStatusCode.OK to """{"success":true,"message":"ok","data":$member}"""
            } else {
                HttpStatusCode.OK to """{"success":true,"message":"ok","data":[$member]}"""
            }
        }
        vm.refresh()
        vm.await { it.members.isNotEmpty() }

        vm.unlockLogin(vm.uiState.value.members.single())
        vm.await { it.notice != null }

        assertEquals(listOf("/api/users/u1/unlock-login"), posts)
        assertEquals("jvdm can sign in again", vm.uiState.value.notice)
    }

    @Test
    fun `a worker outside the manager's scope shows the not-found message`() = runTest {
        val vm = viewModel {
            if (it.method == HttpMethod.Post) HttpStatusCode.NotFound to "Not Found"
            else HttpStatusCode.OK to """{"success":true,"message":"ok","data":[$member]}"""
        }
        vm.refresh()
        vm.await { it.members.isNotEmpty() }

        vm.unlockLogin(vm.uiState.value.members.single())
        vm.await { it.error != null }

        assertEquals("That user no longer exists.", vm.uiState.value.error)
    }
}
