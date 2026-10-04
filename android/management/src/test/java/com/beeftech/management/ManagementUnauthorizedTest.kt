package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.database.security.UnauthorizedReason
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagementUnauthorizedTest {

    private class RecordingProvider : TokenProvider {
        val reasons = mutableListOf<UnauthorizedReason>()
        override suspend fun token(): String? = "tok"
        override suspend fun onUnauthorized(reason: UnauthorizedReason) {
            reasons += reason
        }
    }

    private fun client(provider: TokenProvider, status: HttpStatusCode, body: String) = ManagementApiClient(
        tokenProvider = provider,
        baseUrl = "http://test-host/",
        httpClient = HttpClient(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    )

    @Test
    fun `a revoked session is reported and the call still returns Unauthorized`() = runTest {
        val provider = RecordingProvider()

        val result = client(provider, HttpStatusCode.Unauthorized, """{"success":false,"message":"Session revoked"}""").listUsers()

        assertEquals(ManagementResult.Unauthorized, result)
        assertEquals(listOf(UnauthorizedReason.SESSION_REVOKED), provider.reasons)
    }

    @Test
    fun `an ordinary 401 is a rejected token, and a 403 reports nothing`() = runTest {
        val provider = RecordingProvider()

        client(provider, HttpStatusCode.Unauthorized, """{"success":false,"message":"Invalid token"}""").listUsers()
        client(provider, HttpStatusCode.Forbidden, """{"success":false,"message":"Forbidden"}""").listUsers()

        assertEquals(listOf(UnauthorizedReason.TOKEN_REJECTED), provider.reasons)
    }
}
