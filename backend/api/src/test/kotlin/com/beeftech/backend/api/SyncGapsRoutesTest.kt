package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.UserRepository
import com.beeftech.backend.api.feedcrib.FeedCribRepository
import com.beeftech.backend.api.feedcrib.FeedCribRequest
import com.beeftech.backend.api.feedcrib.FeedCribService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncGapsRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun newDbUrl(): String {
        val file = Files.createTempFile("beeftech-syncgaps-test", ".db")
        file.toFile().deleteOnExit()
        return "jdbc:sqlite:$file"
    }

    private fun ApplicationTestBuilder.startApp() {
        System.setProperty("beeftech.db.url", newDbUrl())
        application { module() }
    }

    private suspend fun HttpClient.login(username: String, pin: String): String {
        val text = post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","pin":"$pin","device_id":"dev-$username"}""")
        }.bodyAsText()
        return Json.parseToJsonElement(text).jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content
    }

    /* The module loads lazily, so touch it before using repositories. */
    private suspend fun HttpClient.insertOtherSiteWorker() {
        get("/health")
        UserRepository().insertUser("other-worker", "other", PinHasher.hash("40004"), 3, siteId = "other-site")
    }

    private suspend fun HttpClient.syncMovement(token: String, animalId: String, guid: String, timestamp: Long) {
        post("/api/animal-movements/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """{"deviceId":"d","records":[{"animalId":"$animalId","movementType":"PEN_TO_PEN",
                "responsibleWorker":"w","timestamp":$timestamp,"recordguid":"$guid"}]}"""
            )
        }
    }

    private suspend fun HttpClient.guids(path: String, token: String): List<String> =
        Json.parseToJsonElement(get(path) { header("Authorization", "Bearer $token") }.bodyAsText())
            .jsonObject["data"]!!.jsonArray.map { it.jsonObject["recordguid"]!!.jsonPrimitive.content }

    @Test
    fun `movement lists are scoped to worker then site then admin and newest first`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")

        client.syncMovement(worker, "A-1", "m-old", 100)
        client.syncMovement(worker, "A-1", "m-new", 200)
        client.syncMovement(other, "A-1", "m-other", 150)

        assertEquals(listOf("m-new", "m-old"), client.guids("/api/animal-movements", worker))
        assertEquals(listOf("m-new", "m-old"), client.guids("/api/animal-movements", manager))
        assertEquals(listOf("m-new", "m-other", "m-old"), client.guids("/api/animal-movements", admin))
        assertEquals(listOf("m-other"), client.guids("/api/animal-movements", other))
    }

    @Test
    fun `movements by animal are scoped and an unknown animal is an empty list`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")

        client.syncMovement(worker, "A-1", "m-mine", 100)
        client.syncMovement(other, "A-1", "m-other", 200)
        client.syncMovement(worker, "A-2", "m-mine-2", 300)

        assertEquals(listOf("m-mine"), client.guids("/api/animal-movements/A-1", manager))
        assertEquals(emptyList(), client.guids("/api/animal-movements/NOPE", manager))
    }

    @Test
    fun `movement reads need a token`() = testApplication {
        startApp()
        val client = createClient { }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/animal-movements").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/animal-movements/A-1").status)
    }

    @Test
    fun `feed crib readings are stored in the database and survive a restart`() {
        val url = newDbUrl()
        System.setProperty("beeftech.db.url", url)

        DatabaseFactory.init(url)
        runBlocking {
            FeedCribService(FeedCribRepository()).saveReading(
                FeedCribRequest("Pen A", 1.5, "ok", "ok", "ok", 100),
                submittedByUserId = "u1",
                siteId = "s1"
            )
        }

        /* A fresh connection and service, as after a backend restart. */
        DatabaseFactory.init(url)
        val readings = runBlocking { FeedCribService(FeedCribRepository()).getAll() }

        assertEquals(listOf("Pen A"), readings.map { it.penName })
        assertEquals(1.5, readings.single().adiValue, 0.0)
    }

    @Test
    fun `feed crib filters by pen name ignoring case and keeps scope`() {
        val url = newDbUrl()
        DatabaseFactory.init(url)
        val service = FeedCribService(FeedCribRepository())

        runBlocking {
            service.saveReading(FeedCribRequest("Pen A", 1.0, "a", "a", "a", 1), "u1", "s1")
            service.saveReading(FeedCribRequest("Pen B", 2.0, "b", "b", "b", 2), "u1", "s1")
            service.saveReading(FeedCribRequest("pen a", 3.0, "c", "c", "c", 3), "u2", "s2")

            assertEquals(listOf(1.0, 3.0), service.getByPenName("PEN A").map { it.adiValue })
            assertEquals(listOf(1.0), service.getByPenName("pen a", RecordScope.Site("s1")).map { it.adiValue })
            assertEquals(listOf(3.0), service.getByPenName("pen a", RecordScope.User("u2")).map { it.adiValue })
            assertTrue(service.getAll(RecordScope.Site(null)).isEmpty())
            assertFalse(service.getAll(RecordScope.All).isEmpty())
        }
    }
}
