package com.beeftech.farmtraceability

import com.beeftech.database.entity.Mortality
import com.beeftech.database.security.TokenProvider
import com.beeftech.database.security.UnauthorizedReason
import com.beeftech.database.security.reportUnauthorized
import com.beeftech.farmtraceability.data.MortalityApiClient
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
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * A 401 from the server is reported to the token provider, which decides what it means.
 * Sync clients only report: the result is still a failure, so the worker retries and the
 * records stay queued.
 */
class UnauthorizedReportingTest {

    private class RecordingProvider : TokenProvider {
        val reasons = mutableListOf<UnauthorizedReason>()
        override suspend fun token(): String? = "tok"
        override suspend fun onUnauthorized(reason: UnauthorizedReason) {
            reasons += reason
        }
    }

    private val mortality = Mortality(
        animalId = "A-1",
        causeOfDeath = "Bloat",
        responsibleWorker = "jvdm",
        timestamp = 1L,
        recordGuid = "g-1"
    )

    private fun client(provider: TokenProvider, status: HttpStatusCode, body: String) = MortalityApiClient(
        tokenProvider = provider,
        baseUrl = "http://test-host/",
        httpClient = HttpClient(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    )

    @Test
    fun `server messages tell a revocation from an ordinary rejected token`() {
        assertEquals(UnauthorizedReason.SESSION_REVOKED, UnauthorizedReason.fromServerMessage("Session revoked"))
        assertEquals(UnauthorizedReason.SESSION_REVOKED, UnauthorizedReason.fromServerMessage("Device revoked"))
        assertEquals(UnauthorizedReason.SESSION_REVOKED, UnauthorizedReason.fromServerMessage("DEVICE REVOKED"))
        assertEquals(UnauthorizedReason.TOKEN_REJECTED, UnauthorizedReason.fromServerMessage("Invalid token"))
        assertEquals(UnauthorizedReason.TOKEN_REJECTED, UnauthorizedReason.fromServerMessage("Missing token"))
        assertEquals(UnauthorizedReason.TOKEN_REJECTED, UnauthorizedReason.fromServerMessage(null))
    }

    @Test
    fun `a 401 with a revocation message is reported as revoked and the sync still fails`() = runTest {
        val provider = RecordingProvider()

        val result = client(provider, HttpStatusCode.Unauthorized, """{"success":false,"message":"Session revoked"}""")
            .syncMortalities(listOf(mortality), "phone")

        assertTrue(result.isFailure)
        assertEquals(listOf(UnauthorizedReason.SESSION_REVOKED), provider.reasons)
    }

    @Test
    fun `a revoked phone is reported as revoked too`() = runTest {
        val provider = RecordingProvider()

        client(provider, HttpStatusCode.Unauthorized, """{"success":false,"message":"Device revoked"}""")
            .syncMortalities(listOf(mortality), "phone")

        assertEquals(listOf(UnauthorizedReason.SESSION_REVOKED), provider.reasons)
    }

    @Test
    fun `any other 401 is just a rejected token`() = runTest {
        val provider = RecordingProvider()

        client(provider, HttpStatusCode.Unauthorized, """{"success":false,"message":"Invalid token"}""")
            .syncMortalities(listOf(mortality), "phone")
        client(provider, HttpStatusCode.Unauthorized, "Unauthorized")
            .syncMortalities(listOf(mortality), "phone")

        assertEquals(listOf(UnauthorizedReason.TOKEN_REJECTED, UnauthorizedReason.TOKEN_REJECTED), provider.reasons)
    }

    @Test
    fun `other failures and successes report nothing`() = runTest {
        val provider = RecordingProvider()

        client(provider, HttpStatusCode.InternalServerError, "boom").syncMortalities(listOf(mortality), "phone")
        client(provider, HttpStatusCode.Forbidden, """{"message":"Forbidden"}""").syncMortalities(listOf(mortality), "phone")
        client(
            provider, HttpStatusCode.OK,
            """{"success":true,"message":"ok","data":{"results":[{"recordguid":"g-1","animalId":"A-1","status":"SYNCED","serverSyncedAt":5}]}}"""
        ).syncMortalities(listOf(mortality), "phone")

        assertEquals(emptyList<UnauthorizedReason>(), provider.reasons)
    }

    @Test
    fun `reporting never throws even if the provider does`() = runTest {
        val broken = object : TokenProvider {
            override suspend fun token(): String? = "tok"
            override suspend fun onUnauthorized(reason: UnauthorizedReason) = error("prefs unavailable")
        }

        broken.reportUnauthorized("""{"message":"Session revoked"}""")

        /* Reaching here is the assertion. */
        assertTrue(true)
    }
}
