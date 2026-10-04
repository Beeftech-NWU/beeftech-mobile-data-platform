package com.beeftech.management

import com.beeftech.database.entity.SyncSecurityEvent
import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.EventUploadOutcome
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.SecurityEventSync
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.engine.mock.toByteArray
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class SecurityEventSyncTest {

    private val dao = FakeSyncSecurityDao()

    private suspend fun record(key: String, user: String? = "u1", time: Long = 1_000) {
        dao.insertEvent(
            SyncSecurityEvent(
                eventKey = key, userId = user, eventType = "SYNC_WARNING", eventTime = time, pendingCount = 2
            )
        )
    }

    private fun sync(
        userId: String = "u1",
        handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>
    ): SecurityEventSync {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return SecurityEventSync(
            ManagementApiClient(
                tokenProvider = object : TokenProvider {
                    override suspend fun token(): String? = "tok"
                },
                baseUrl = "http://test-host/",
                httpClient = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
            ),
            dao,
            userId,
            now = { 777L }
        )
    }

    private fun answer(accepted: Int, duplicates: Int = 0, rejected: Int = 0) =
        HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"accepted":$accepted,"duplicates":$duplicates,"rejected":$rejected}}"""

    @Test
    fun `nothing waiting means no request`() = runTest {
        var calls = 0

        val outcome = sync { calls++; answer(0) }.upload()

        assertEquals(EventUploadOutcome.NothingToSend, outcome)
        assertEquals(0, calls)
    }

    @Test
    fun `events are sent oldest first with the user, and flagged uploaded once the server answers`() = runTest {
        record("late", time = 2_000)
        record("early", time = 1_000)
        var body = ""

        val outcome = sync {
            body = it.body.toByteArray().decodeToString()
            answer(accepted = 2)
        }.upload()

        assertEquals(EventUploadOutcome.Uploaded(2), outcome)
        val sent = Json.parseToJsonElement(body).jsonObject["events"]!!.jsonArray
        assertEquals(listOf("early", "late"), sent.map { it.jsonObject["eventKey"]!!.jsonPrimitive.content })
        assertEquals("u1", sent.first().jsonObject["userId"]!!.jsonPrimitive.content)
        assertEquals(listOf(777L, 777L), dao.events.map { it.uploadedAt })
    }

    @Test
    fun `duplicates and rejected events count as taken, so they are not sent again`() = runTest {
        record("a")
        record("b")
        record("c")

        sync { answer(accepted = 1, duplicates = 1, rejected = 1) }.upload()

        assertEquals(listOf(777L, 777L, 777L), dao.events.map { it.uploadedAt })
    }

    @Test
    fun `a short answer leaves the events waiting`() = runTest {
        record("a")
        record("b")

        val outcome = sync { answer(accepted = 1) }.upload()

        assertEquals(EventUploadOutcome.Failed("The server did not take every event"), outcome)
        assertEquals(listOf(null, null), dao.events.map { it.uploadedAt })
    }

    @Test
    fun `no connection, a sign-out or a server error leave every event waiting and delete nothing`() = runTest {
        record("a")

        assertEquals(EventUploadOutcome.Failed("No connection"), sync { throw IOException("offline") }.upload())
        assertEquals(EventUploadOutcome.Failed("Signed out"), sync { HttpStatusCode.Unauthorized to """{"success":false,"message":"Session revoked"}""" }.upload())
        assertNotNull(sync { HttpStatusCode.InternalServerError to "boom" }.upload() as? EventUploadOutcome.Failed)

        assertEquals(1, dao.events.size)
        assertNull(dao.events.single().uploadedAt)
    }

    @Test
    fun `only the signed-in user's events are sent, other users' wait for their own sign-in`() = runTest {
        record("mine", user = "u1")
        record("theirs", user = "u2")
        var body = ""

        sync { body = it.body.toByteArray().decodeToString(); answer(accepted = 1) }.upload()

        assertEquals(listOf(777L, null), dao.events.map { it.uploadedAt })
        assertEquals(1, Json.parseToJsonElement(body).jsonObject["events"]!!.jsonArray.size)
    }

    @Test
    fun `more than one batch is sent in turn`() = runTest {
        repeat(SecurityEventSync.BATCH_SIZE + 5) { record("k$it", time = it.toLong()) }
        val sizes = mutableListOf<Int>()

        val outcome = sync {
            val count = Json.parseToJsonElement(it.body.toByteArray().decodeToString()).jsonObject["events"]!!.jsonArray.size
            sizes += count
            answer(accepted = count)
        }.upload()

        assertEquals(listOf(SecurityEventSync.BATCH_SIZE, 5), sizes)
        assertEquals(EventUploadOutcome.Uploaded(SecurityEventSync.BATCH_SIZE + 5), outcome)
    }
}
