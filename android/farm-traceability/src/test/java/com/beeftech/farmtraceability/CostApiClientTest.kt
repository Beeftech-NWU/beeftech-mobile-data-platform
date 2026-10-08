package com.beeftech.farmtraceability

import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.security.SyncIdentityRegistry
import com.beeftech.database.security.TokenProvider
import com.beeftech.database.util.FileNamingUtils
import com.beeftech.farmtraceability.data.CostApiClient
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test

class CostApiClientTest {

    private class FakeTokenProvider(private val value: String?) : TokenProvider {
        override suspend fun token(): String? = value
    }

    private val cost = AnimalCost(
        animalId = "A-1",
        costType = "TRANSPORT",
        amount = 250.5,
        description = "Truck",
        gpsLat = -25.0,
        gpsLng = 28.0,
        timestamp = 1_700_000_000_000L,
        sourceEntity = "TREATMENT",
        sourceRecordId = "t-1",
        recordGuid = "guid-1"
    )

    private fun client(
        token: String? = "tok",
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"success":true,"message":"ok","data":{"results":[
            {"recordguid":"guid-1","animalId":"A-1","status":"SYNCED","serverSyncedAt":55}]}}""",
        onRequest: suspend (HttpRequestData) -> Unit = {}
    ) = CostApiClient(
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

    @After
    fun clearSyncIdentity() = SyncIdentityRegistry.clear()

    private suspend fun sentBatchName(): String? {
        var body = ""
        client { body = String(it.body.toByteArray()) }.syncCosts(listOf(cost), "phone")
        return Json.parseToJsonElement(body).jsonObject["batchName"]?.jsonPrimitive?.content
    }

    @Test
    fun `the upload is named from the signed-in farm code and device`() = runTest {
        SyncIdentityRegistry.set("BF01", "MOB_DEV_a1b2c3d4")

        val name = sentBatchName()!!

        val parts = FileNamingUtils.parse(name)!!
        assertEquals("BF01", parts.farmCode)
        assertEquals("COST", parts.project)
        assertEquals("MOB_DEV_a1b2c3d4", parts.deviceId)
    }

    @Test
    fun `the upload goes without a name until the farm code is known`() = runTest {
        SyncIdentityRegistry.clear()

        assertNull(sentBatchName())
    }

    @Test
    fun `fails without a token and does not call the server`() = runTest {
        var called = false
        val api = client(token = null) { called = true }

        val result = api.syncCosts(listOf(cost), "phone")

        assertTrue(result.isFailure)
        assertFalse(called)
    }

    @Test
    fun `an empty batch succeeds without a request`() = runTest {
        var called = false
        val api = client { called = true }

        val result = api.syncCosts(emptyList(), "phone")

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

        val result = api.syncCosts(listOf(cost), "phone")

        assertEquals("http://test-host/api/costs/sync", url)
        assertEquals("Bearer tok", auth)
        val record = Json.parseToJsonElement(body).jsonObject["records"]!!.jsonArray.single().jsonObject
        assertEquals("guid-1", record["recordguid"]!!.jsonPrimitive.content)
        assertEquals("TRANSPORT", record["costType"]!!.jsonPrimitive.content)
        assertEquals(250.5, record["amount"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals("TREATMENT", record["sourceEntity"]!!.jsonPrimitive.content)
        assertEquals("t-1", record["sourceRecordId"]!!.jsonPrimitive.content)
        assertEquals("phone", record["deviceId"]!!.jsonPrimitive.content)
        assertEquals("SYNCED", result.getOrThrow().results.single().status)
    }

    @Test
    fun `a server error becomes a failure`() = runTest {
        val api = client(status = HttpStatusCode.InternalServerError, body = "oops")

        assertTrue(api.syncCosts(listOf(cost), "phone").isFailure)
    }
}
