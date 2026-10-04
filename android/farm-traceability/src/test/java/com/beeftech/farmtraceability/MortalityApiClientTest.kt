package com.beeftech.farmtraceability

import com.beeftech.database.entity.Mortality
import com.beeftech.database.security.TokenProvider
import com.beeftech.farmtraceability.data.MortalityApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MortalityApiClientTest {

    private class FakeTokenProvider(private val value: String?) : TokenProvider {
        override suspend fun token(): String? = value
    }

    private val mortality = Mortality(
        animalId = "A-1",
        causeOfDeath = "Bloat",
        responsibleWorker = "jvdm",
        timestamp = 1_700_000_000_000L,
        recordGuid = "guid-1"
    )

    private fun client(
        token: String? = "tok",
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"success":true,"message":"ok","data":{"results":[
            {"recordguid":"guid-1","animalId":"A-1","status":"SYNCED","serverSyncedAt":55}]}}""",
        onRequest: suspend (HttpRequestData) -> Unit = {}
    ) = MortalityApiClient(
        tokenProvider = FakeTokenProvider(token),
        baseUrl = "http://test-host/",
        httpClient = HttpClient(
            MockEngine {
                onRequest(it)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    )

    @Test
    fun `fails without a token and does not call the server`() = runTest {
        var called = false
        val api = client(token = null) { called = true }

        val result = api.syncMortalities(listOf(mortality), "phone")

        assertTrue(result.isFailure)
        assertFalse(called)
    }

    @Test
    fun `an empty batch succeeds without a request`() = runTest {
        var called = false
        val api = client { called = true }

        val result = api.syncMortalities(emptyList(), "phone")

        assertTrue(result.isSuccess)
        assertFalse(called)
    }

    @Test
    fun `posts the record with its guid and the bearer token`() = runTest {
        var url = ""
        var auth: String? = null
        var body = ""
        val api = client {
            url = it.url.toString()
            auth = it.headers[HttpHeaders.Authorization]
            body = String(it.body.toByteArray())
        }

        val result = api.syncMortalities(listOf(mortality), "phone")

        assertEquals("http://test-host/api/mortalities/sync", url)
        assertEquals("Bearer tok", auth)
        val record = Json.parseToJsonElement(body).jsonObject["records"]!!.jsonArray.single().jsonObject
        assertEquals("guid-1", record["recordguid"]!!.jsonPrimitive.content)
        assertEquals("Bloat", record["causeOfDeath"]!!.jsonPrimitive.content)
        assertEquals("phone", record["deviceId"]!!.jsonPrimitive.content)
        assertEquals("SYNCED", result.getOrThrow().results.single().status)
    }

    @Test
    fun `a server error becomes a failure`() = runTest {
        val api = client(status = HttpStatusCode.InternalServerError, body = "oops")

        assertTrue(api.syncMortalities(listOf(mortality), "phone").isFailure)
    }
}
