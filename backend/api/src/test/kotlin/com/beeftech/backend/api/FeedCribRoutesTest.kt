package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.UserRepository
import com.beeftech.backend.api.feedcrib.FeedCribSeeder
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedCribRoutesTest {

    private fun uniqueTestDbUrl(): String {
        val tempFile = Files.createTempFile("beeftech-backend-test", ".db")
        tempFile.toFile().deleteOnExit()
        return "jdbc:sqlite:${tempFile}"
    }

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }
    }

    private suspend fun HttpClient.login(username: String, pin: String): String {
        val response = post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","pin":"$pin","device_id":"dev-$username"}""")
        }
        val text = response.bodyAsText()
        return Json.parseToJsonElement(text)
            .also { require(it.jsonObject["data"] !is JsonNull) { "login $username failed: $text" } }
            .jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content
    }

    /* A worker on a second site that has its own A01..A08, so scoping has something to exclude. */
    private suspend fun HttpClient.insertOtherSiteWorker() {
        /* The module loads lazily, so make a request first or this would hit the previous test's DB. */
        get("/health")
        UserRepository().insertUser(
            userId = "other-worker",
            username = "other",
            pinHash = PinHasher.hash("40004"),
            role = 3,
            siteId = "other-site"
        )
        FeedCribSeeder.seedDevCribs("other-site")
    }

    private val today: LocalDate = LocalDate.now(ZoneOffset.UTC)

    private fun record(
        guid: String,
        crib: String = "A01",
        date: LocalDate = today,
        slot: String = "MORNING",
        code: Int? = 3,
        adi: Double = 11.0,
        capturedAt: Long = System.currentTimeMillis()
    ) =
        """{"recordguid":"$guid","cribNumber":"$crib","readingDate":"$date","slot":"$slot",""" +
            """"code":${code ?: "null"},"adi":$adi,"capturedAt":$capturedAt,"deviceId":"d"}"""

    private suspend fun HttpClient.sync(token: String, vararg records: String, batchName: String? = null): HttpResponse =
        post("/api/feed-crib-entries/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            val batch = batchName?.let { ""","batchName":"$it"""" }.orEmpty()
            setBody("""{"deviceId":"d","records":[${records.joinToString(",")}]$batch}""")
        }

    private suspend fun HttpClient.download(token: String, query: String = ""): HttpResponse =
        get("/api/feed-cribs$query") { header("Authorization", "Bearer $token") }

    private fun JsonElement.data(): JsonObject = jsonObject["data"]!!.jsonObject

    private suspend fun HttpResponse.results() =
        Json.parseToJsonElement(bodyAsText()).data()["results"]!!.jsonArray.map { it.jsonObject }

    private suspend fun HttpResponse.statuses() = results().map { it["status"]!!.jsonPrimitive.content }

    private suspend fun HttpResponse.entries() =
        Json.parseToJsonElement(bodyAsText()).data()["entries"]!!.jsonArray.map { it.jsonObject }

    private suspend fun HttpResponse.crib(number: String) =
        Json.parseToJsonElement(bodyAsText()).data()["cribs"]!!.jsonArray
            .map { it.jsonObject }
            .single { it["cribNumber"]!!.jsonPrimitive.content == number }

    private fun JsonObject.adi(field: String) = this[field]?.jsonPrimitive?.doubleOrNull

    @Test
    fun `download needs a token`() = testApplication {
        startApp()
        val client = createClient { }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/feed-cribs").status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/feed-crib-entries/sync") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"d","records":[]}""")
        }.status)
    }

    @Test
    fun `download returns the site's cribs and the code table`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        val body = Json.parseToJsonElement(client.download(worker).bodyAsText()).data()

        assertEquals("dev-site-1", body["siteId"]!!.jsonPrimitive.content)
        assertEquals((1..8).map { "A%02d".format(it) }, body["cribs"]!!.jsonArray.map { it.jsonObject["cribNumber"]!!.jsonPrimitive.content })
        assertEquals((0..5).toList(), body["codes"]!!.jsonArray.map { it.jsonObject["code"]!!.jsonPrimitive.content.toInt() })
        assertTrue(body["entries"]!!.jsonArray.isEmpty())
    }

    @Test
    fun `sync stores an entry and the next download returns it`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        val synced = client.sync(worker, record("g-1", code = 3, adi = 11.4))
        assertEquals(listOf("SYNCED"), synced.statuses())
        assertTrue(synced.results().single()["serverSyncedAt"]!!.jsonPrimitive.content.toLong() > 0)

        val entry = client.download(worker).entries().single()
        assertEquals("g-1", entry["recordguid"]!!.jsonPrimitive.content)
        assertEquals("A01", entry["cribNumber"]!!.jsonPrimitive.content)
        assertEquals("MORNING", entry["slot"]!!.jsonPrimitive.content)
        assertEquals("3", entry["code"]!!.jsonPrimitive.content)
        assertEquals("d", entry["deviceId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `an ADI only entry has no code`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        assertEquals(listOf("SYNCED"), client.sync(worker, record("g-adi", code = null, adi = 12.0)).statuses())

        assertNull(client.download(worker).entries().single()["code"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `sending the same entry again is idempotent`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        val body = record("g-dup", adi = 11.0, capturedAt = 1_000L)
        assertEquals(listOf("SYNCED"), client.sync(worker, body).statuses())
        assertEquals(listOf("SYNCED"), client.sync(worker, body).statuses())

        assertEquals(1, client.download(worker, "?days=14").entries().size)
    }

    @Test
    fun `a repeat of the same guid with the same data does not change current ADI`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        client.sync(worker, record("g-1", adi = 11.0, capturedAt = 1_000L))
        client.sync(worker, record("g-2", adi = 12.0, capturedAt = 2_000L))
        client.sync(worker, record("g-1", adi = 11.0, capturedAt = 1_000L))

        assertEquals(12.0, client.download(worker).crib("A01").adi("currentAdi"))
    }

    @Test
    fun `current ADI follows the newest capture and a late older entry cannot roll it back`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        assertNull(client.download(worker).crib("A01").adi("currentAdi"))

        client.sync(worker, record("g-new", adi = 12.5, capturedAt = 2_000L))
        assertEquals(12.5, client.download(worker).crib("A01").adi("currentAdi"))

        client.sync(worker, record("g-old", adi = 9.0, capturedAt = 1_000L))
        assertEquals(12.5, client.download(worker).crib("A01").adi("currentAdi"))

        client.sync(worker, record("g-newer", adi = 13.0, capturedAt = 3_000L))
        assertEquals(13.0, client.download(worker).crib("A01").adi("currentAdi"))

        /* Another crib is not touched. */
        assertNull(client.download(worker).crib("A02").adi("currentAdi"))
    }

    @Test
    fun `an invalid crib, code, slot, date or ADI is rejected and the rest of the batch still syncs`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        val results = client.sync(
            worker,
            record("g-ok"),
            record("g-no-crib", crib = "Z99"),
            record("g-bad-code", code = 9),
            record("g-bad-slot", slot = "NIGHT"),
            record("g-bad-adi", adi = -1.0),
            record("g-ok-2", slot = "EVENING")
        ).results()

        assertEquals(
            listOf("SYNCED", "ERROR", "ERROR", "ERROR", "ERROR", "SYNCED"),
            results.map { it["status"]!!.jsonPrimitive.content }
        )
        assertTrue("Z99" in results[1]["message"]!!.jsonPrimitive.content)
        assertTrue("code" in results[2]["message"]!!.jsonPrimitive.content)
        assertEquals(
            setOf("g-ok", "g-ok-2"),
            client.download(worker).entries().map { it["recordguid"]!!.jsonPrimitive.content }.toSet()
        )
    }

    @Test
    fun `a malformed reading date is rejected`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        val bad = record("g-date").replace("\"readingDate\":\"$today\"", "\"readingDate\":\"05/01/2026\"")

        assertEquals(listOf("ERROR"), client.sync(worker, bad).statuses())
    }

    @Test
    fun `entries are shared across the site and kept apart from other sites`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()

        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val other = client.login("other", "40004")

        client.sync(worker, record("g-worker", slot = "MORNING", adi = 11.0))
        client.sync(manager, record("g-manager", slot = "MIDDAY", adi = 12.0))
        /* The same crib number on another site is a different crib. */
        assertEquals(listOf("SYNCED"), client.sync(other, record("g-other", adi = 20.0)).statuses())

        val siteGuids = setOf("g-worker", "g-manager")
        assertEquals(siteGuids, client.download(worker).entries().map { it["recordguid"]!!.jsonPrimitive.content }.toSet())
        assertEquals(siteGuids, client.download(manager).entries().map { it["recordguid"]!!.jsonPrimitive.content }.toSet())
        assertEquals(setOf("g-other"), client.download(other).entries().map { it["recordguid"]!!.jsonPrimitive.content }.toSet())

        assertEquals(12.0, client.download(worker).crib("A01").adi("currentAdi"))
        assertEquals(20.0, client.download(other).crib("A01").adi("currentAdi"))
    }

    @Test
    fun `another user or site cannot overwrite an entry by its guid`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()

        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val other = client.login("other", "40004")

        client.sync(worker, record("g-mine", adi = 11.0))

        assertEquals(listOf("ERROR"), client.sync(manager, record("g-mine", adi = 99.0)).statuses())
        assertEquals(listOf("ERROR"), client.sync(other, record("g-mine", adi = 99.0)).statuses())

        assertEquals(11.0, client.download(worker).entries().single()["adi"]!!.jsonPrimitive.doubleOrNull)
    }

    @Test
    fun `a crib that is not on the caller's site is rejected`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()

        val other = client.login("other", "40004")

        /* other-site has A01..A08 only. */
        assertEquals(listOf("ERROR"), client.sync(other, record("g-x", crib = "B01")).statuses())
    }

    @Test
    fun `an admin has no site, so must name one to download and cannot save entries`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")

        assertEquals(HttpStatusCode.BadRequest, client.download(admin).status)

        val named = client.download(admin, "?siteId=dev-site-1")
        assertEquals(HttpStatusCode.OK, named.status)
        assertEquals("A01", Json.parseToJsonElement(named.bodyAsText()).data()["cribs"]!!.jsonArray.first().jsonObject["cribNumber"]!!.jsonPrimitive.content)

        assertEquals(listOf("ERROR"), client.sync(admin, record("g-admin")).statuses())
    }

    @Test
    fun `only an admin can pick the site to download`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")

        client.sync(worker, record("g-1"))

        /* The worker's ?siteId= is ignored: they still get their own site. */
        val body = Json.parseToJsonElement(client.download(worker, "?siteId=other-site").bodyAsText()).data()
        assertEquals("dev-site-1", body["siteId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `download returns the last 3 days of entries and leaves older ones out`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        client.sync(
            worker,
            record("g-today", date = today),
            record("g-2-days", date = today.minusDays(2), slot = "EVENING"),
            record("g-10-days", date = today.minusDays(10), slot = "MIDDAY")
        )

        val guids = client.download(worker).entries().map { it["recordguid"]!!.jsonPrimitive.content }.toSet()

        assertEquals(setOf("g-today", "g-2-days"), guids)
        assertFalse("g-10-days" in guids)
        assertTrue("g-10-days" in client.download(worker, "?days=14").entries().map { it["recordguid"]!!.jsonPrimitive.content })
    }

    @Test
    fun `a batch name for another project or farm is refused and the right one is accepted`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        val wrongProject = client.sync(worker, record("g-1"), batchName = "S001-CALF_REG-20260101-120000-dev_jvdm")
        assertEquals(HttpStatusCode.BadRequest, wrongProject.status)

        val wrongFarm = client.sync(worker, record("g-1"), batchName = "ZZZZ-FEED_CRIB-20260101-120000-dev_jvdm")
        assertEquals(HttpStatusCode.BadRequest, wrongFarm.status)

        val ok = client.sync(worker, record("g-1"), batchName = "S001-FEED_CRIB-20260101-120000-dev_jvdm")
        assertEquals(HttpStatusCode.OK, ok.status)
        assertEquals(listOf("SYNCED"), ok.statuses())
    }

    @Test
    fun `entries survive a restart`() = testApplication {
        val url = uniqueTestDbUrl()
        System.setProperty("beeftech.db.url", url)
        application { module() }
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        client.sync(worker, record("g-keep", adi = 11.5, capturedAt = 5_000L))

        /* A fresh connection, as after a backend restart. */
        DatabaseFactory.init(url)

        val body = client.download(worker)
        assertEquals(listOf("g-keep"), body.entries().map { it["recordguid"]!!.jsonPrimitive.content })
        assertEquals(11.5, body.crib("A01").adi("currentAdi"))
    }

    @Test
    fun `the old flat feed crib routes are gone`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        assertEquals(HttpStatusCode.NotFound, client.get("/api/feed-crib") { header("Authorization", "Bearer $worker") }.status)
    }
}
