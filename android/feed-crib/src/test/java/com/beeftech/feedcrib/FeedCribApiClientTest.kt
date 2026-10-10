package com.beeftech.feedcrib

import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.feedcrib.data.FeedCribApiClient
import com.beeftech.feedcrib.fakes.FakeFeedCribServer
import com.beeftech.feedcrib.fakes.FakeTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedCribApiClientTest {

    private fun entry(guid: String = "guid-1", code: Int? = 3) = FeedCribEntryEntity(
        recordGuid = guid, cribNumber = "A06", readingDate = "2026-10-10", slot = "MIDDAY",
        code = code, adi = 11.1, capturedAt = 1_791_634_800_000L, deviceId = "dev-1", userId = "user-1",
        gpsLat = 0.0, gpsLng = 0.0
    )

    @Test
    fun `syncEntries posts to the sync endpoint with the bearer token and the records`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        var auth: String? = null
        var body: String? = null

        val engine = MockEngine { request ->
            path = request.url.encodedPath
            method = request.method
            auth = request.headers[HttpHeaders.Authorization]
            body = (request.body as TextContent).text
            respond(
                content = """{"success":true,"message":"ok","data":{"results":[
                    {"recordguid":"guid-1","cribNumber":"A06","status":"SYNCED","serverSyncedAt":999}]}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = FeedCribApiClient(
            tokenProvider = FakeTokenProvider("tok-123"),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine) { install(ContentNegotiation) { json() } }
        )

        val result = client.syncEntries(listOf(entry()), deviceId = "dev-1")

        assertEquals("/api/feed-crib-entries/sync", path)
        assertEquals(HttpMethod.Post, method)
        assertEquals("Bearer tok-123", auth)
        assertTrue(body!!.contains("\"recordguid\":\"guid-1\""))
        assertTrue(body!!.contains("\"cribNumber\":\"A06\""))
        assertTrue(body!!.contains("\"readingDate\":\"2026-10-10\""))
        assertTrue(body!!.contains("\"slot\":\"MIDDAY\""))
        assertTrue(body!!.contains("\"code\":3"))
        assertTrue(body!!.contains("\"deviceId\":\"dev-1\""))
        assertTrue(result.isSuccess)
        assertEquals("SYNCED", result.getOrThrow().results.single().status)
        assertEquals(999L, result.getOrThrow().results.single().serverSyncedAt)
    }

    @Test
    fun `an ADI only entry is sent without a code`() = runTest {
        val server = FakeFeedCribServer()

        server.client().syncEntries(listOf(entry(code = null)), deviceId = "dev-1")

        assertNull(server.syncRequests.single().records.single().code)
    }

    @Test
    fun `syncEntries with nothing to send makes no request`() = runTest {
        val server = FakeFeedCribServer()

        val result = server.client().syncEntries(emptyList(), deviceId = "dev-1")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().results.isEmpty())
        assertTrue(server.syncRequests.isEmpty())
    }

    @Test
    fun `an error status from the server is a failure`() = runTest {
        val server = FakeFeedCribServer().apply { syncHttpStatus = HttpStatusCode.InternalServerError }

        val result = server.client().syncEntries(listOf(entry()), deviceId = "dev-1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("500"))
    }

    @Test
    fun `no signal is a failure not a crash`() = runTest {
        val server = FakeFeedCribServer().apply { offline = true }

        assertTrue(server.client().syncEntries(listOf(entry()), deviceId = "dev-1").isFailure)
        assertTrue(server.client().fetchCribs().isFailure)
    }

    @Test
    fun `no token is a failure and nothing is sent`() = runTest {
        var called = false
        val engine = MockEngine { called = true; respond("", HttpStatusCode.OK) }
        val client = FeedCribApiClient(
            tokenProvider = FakeTokenProvider(null),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine) { install(ContentNegotiation) { json() } }
        )

        assertTrue(client.syncEntries(listOf(entry()), "dev-1").isFailure)
        assertTrue(client.fetchCribs().isFailure)
        assertFalse(called)
    }

    @Test
    fun `fetchCribs asks for the given number of days and reads cribs, codes and entries`() = runTest {
        val server = FakeFeedCribServer().apply { downloadJson = FakeFeedCribServer.sampleDownload() }

        val result = server.client().fetchCribs(days = 3)

        assertTrue(server.downloadUrls.single().endsWith("/api/feed-cribs?days=3"))

        val download = result.getOrThrow()
        assertEquals("site-1", download.siteId)
        assertEquals(listOf("A01", "A02"), download.cribs.map { it.cribNumber })
        assertEquals(10.5, download.cribs.first().currentAdi!!, 0.0)
        assertNull(download.cribs.last().currentAdi)
        assertEquals((0..5).toList(), download.codes.map { it.code })
        assertEquals("server-1", download.entries.single().recordguid)
        assertEquals("user-2", download.entries.single().submittedByUserId)
    }

    @Test
    fun `fetchCribs reports an unsuccessful answer`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"success":false,"message":"Your account has no site"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = FeedCribApiClient(
            tokenProvider = FakeTokenProvider("tok"),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine) { install(ContentNegotiation) { json() } }
        )

        val result = client.fetchCribs()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("no site"))
    }
}
