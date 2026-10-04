package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.UserRepository
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CostRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-cost-test", ".db")
        file.toFile().deleteOnExit()
        System.setProperty("beeftech.db.url", "jdbc:sqlite:$file")
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

    private suspend fun HttpClient.sync(token: String, body: String) =
        post("/api/costs/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private fun record(animal: String, guid: String, amount: Double = 10.0, timestamp: Long = 100) =
        """{"animalId":"$animal","costType":"FEED","amount":$amount,"timestamp":$timestamp,"recordguid":"$guid"}"""

    private fun batch(vararg records: String) = """{"deviceId":"phone","records":[${records.joinToString(",")}]}"""

    private suspend fun HttpClient.rows(path: String, token: String): List<JsonObject> =
        Json.parseToJsonElement(get(path) { header("Authorization", "Bearer $token") }.bodyAsText())
            .jsonObject["data"]!!.jsonArray.map { it.jsonObject }

    @Test
    fun `sync stores the record stamped from the token and the device id from the batch`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        val response = client.sync(worker, batch(record("A-1", "g-1")))

        assertEquals(HttpStatusCode.OK, response.status)
        val result = Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["data"]!!.jsonObject["results"]!!.jsonArray.single().jsonObject
        assertEquals("SYNCED", result["status"]!!.jsonPrimitive.content)
        assertEquals("g-1", result["recordguid"]!!.jsonPrimitive.content)

        val row = transaction(DatabaseFactory.getDatabase()) {
            CostTable.selectAll().single()
        }
        assertEquals("dev-site-1", row[CostTable.siteId])
        assertEquals("phone", row[CostTable.deviceId])
        assertEquals("SYNCED", row[CostTable.syncStatus])
    }

    @Test
    fun `a retried sync updates the same row instead of duplicating it`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        client.sync(worker, batch(record("A-1", "g-1", amount = 10.0)))
        client.sync(worker, batch(record("A-1", "g-1", amount = 25.5)))

        val rows = client.rows("/api/costs", worker)
        assertEquals(1, rows.size)
        assertEquals(25.5, rows.single()["amount"]!!.jsonPrimitive.content.toDouble())
    }

    @Test
    fun `lists are scoped to worker then site then admin and newest first`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")

        client.sync(worker, batch(record("A-1", "g-old", timestamp = 100), record("A-2", "g-new", timestamp = 200)))
        client.sync(other, batch(record("A-3", "g-other", timestamp = 150)))

        fun List<JsonObject>.guids() = map { it["recordguid"]!!.jsonPrimitive.content }

        assertEquals(listOf("g-new", "g-old"), client.rows("/api/costs", worker).guids())
        assertEquals(listOf("g-new", "g-old"), client.rows("/api/costs", manager).guids())
        assertEquals(listOf("g-new", "g-other", "g-old"), client.rows("/api/costs", admin).guids())
        assertEquals(listOf("g-other"), client.rows("/api/costs", other).guids())
    }

    @Test
    fun `by animal returns only that animal in scope and unknown animals are empty`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")

        client.sync(worker, batch(record("A-1", "g-mine")))
        client.sync(other, batch(record("A-1", "g-other")))

        assertEquals(
            listOf("g-mine"),
            client.rows("/api/costs/A-1", manager).map { it["recordguid"]!!.jsonPrimitive.content }
        )
        assertEquals(emptyList(), client.rows("/api/costs/NOPE", manager))
    }

    @Test
    fun `cost routes need a token`() = testApplication {
        startApp()
        val client = createClient { }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/costs").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/costs/A-1").status)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.post("/api/costs/sync") {
                contentType(ContentType.Application.Json)
                setBody(batch(record("A-1", "g-1")))
            }.status
        )
    }
}
