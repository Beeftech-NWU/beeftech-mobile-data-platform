package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
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
import kotlinx.serialization.json.JsonNull
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuditLogRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-audit-test", ".db")
        file.toFile().deleteOnExit()
        System.setProperty("beeftech.db.url", "jdbc:sqlite:$file")
        application { module() }
    }

    private suspend fun HttpClient.login(username: String, pin: String): String {
        val text = post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","pin":"$pin","device_id":"dev-$username"}""")
        }.bodyAsText()
        val data = Json.parseToJsonElement(text).jsonObject["data"]
        require(data != null && data !is JsonNull) { "login $username failed: $text" }
        return data.jsonObject["token"]!!.jsonPrimitive.content
    }

    private suspend fun HttpClient.send(method: String, path: String, token: String, body: String? = null): HttpResponse {
        val block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {
            header("Authorization", "Bearer $token")
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        return when (method) {
            "GET" -> get(path, block)
            "PATCH" -> patch(path, block)
            else -> post(path, block)
        }
    }

    private suspend fun HttpClient.rows(path: String, token: String): List<JsonObject> =
        Json.parseToJsonElement(send("GET", path, token).bodyAsText())
            .jsonObject["data"]!!.jsonArray.map { it.jsonObject }

    private fun JsonObject.str(key: String) = this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

    private suspend fun HttpClient.userId(token: String, username: String): String =
        Json.parseToJsonElement(send("GET", "/api/users", token).bodyAsText())
            .jsonObject["data"]!!.jsonArray
            .first { it.jsonObject["username"]!!.jsonPrimitive.content == username }
            .jsonObject["user_id"]!!.jsonPrimitive.content

    /* The module loads lazily, so touch it before using repositories. */
    private suspend fun HttpClient.insertOtherSiteWorker() {
        get("/health")
        UserRepository().insertUser("other-worker", "other", PinHasher.hash("40004"), 3, siteId = "other-site")
    }

    private suspend fun HttpClient.syncMortality(token: String, animal: String, guid: String) =
        post("/api/mortalities/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """{"deviceId":"d","records":[{"animalId":"$animal","causeOfDeath":"Bloat","responsibleWorker":"w",
                "timestamp":1,"recordguid":"$guid"}]}"""
            )
        }

    private suspend fun HttpClient.void(token: String, guid: String) =
        send("POST", "/api/records/mortalities/$guid/void", token, """{"reason":"dup"}""")

    @Test
    fun `each admin action writes exactly one audit row with the actor and target site`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val manager = client.login("fmanager", "20002")
        val workerId = client.userId(admin, "jvdm")

        /* The manager creates a worker, resets their PIN, then unbinds their phone. */
        val created = client.send("POST", "/api/users", manager, """{"username":"newbie","pin":"12345"}""")
        assertEquals(HttpStatusCode.Created, created.status)
        client.send("POST", "/api/users/$workerId/reset-pin", manager, """{"pin":"99999"}""")
        client.login("jvdm", "99999")
        client.send("POST", "/api/users/$workerId/unbind-device", manager)
        /* The admin changes the worker's role. */
        client.send("PATCH", "/api/users/$workerId", admin, """{"role":2}""")

        val log = client.rows("/api/audit-log", admin)
        assertEquals(
            listOf("USER_UPDATE", "USER_UNBIND_DEVICE", "USER_RESET_PIN", "USER_CREATE"),
            log.map { it.str("action") }
        )
        assertTrue(log.all { it.str("entityType") == "USER" })
        assertEquals("admin", log[0].str("actorUsername"))
        assertEquals(1, log[0].str("actorRole")?.toInt())
        assertEquals("fmanager", log[1].str("actorUsername"))
        assertEquals(workerId, log[1].str("entityId"))
        assertEquals("dev-site-1", log[1].str("siteId"))
        assertEquals("", log[1].str("reason"))
        assertTrue("3->2" in log[0].str("details")!!)
        assertTrue("dev-jvdm->none" in log[1].str("details")!!)
        assertTrue("newbie" in log[3].str("details")!!)
    }

    @Test
    fun `the log never holds a PIN or hash and a no-op update leaves no row`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val workerId = client.userId(admin, "jvdm")

        client.send("POST", "/api/users", admin, """{"username":"newbie","pin":"54321","site_id":"dev-site-1"}""")
        client.send("POST", "/api/users/$workerId/reset-pin", admin, """{"pin":"99999"}""")
        client.send("POST", "/api/users/$workerId/reset-pin", admin)
        /* Same role and site as before. */
        client.send("PATCH", "/api/users/$workerId", admin, """{"role":3,"active":true}""")

        val text = client.send("GET", "/api/audit-log", admin).bodyAsText()
        listOf("54321", "99999", "hash", "bcrypt", "\$2a\$").forEach { assertFalse(it in text.lowercase(), "found $it") }
        val rows = client.rows("/api/audit-log", admin)
        assertEquals(3, rows.size)
        /* A PIN reset carries no details at all. */
        assertTrue(rows.filter { it.str("action") == "USER_RESET_PIN" }.all { it.str("details") == null })
        val rowCount = transaction(DatabaseFactory.getDatabase()) { AuditLogTable.selectAll().count() }
        assertEquals(3L, rowCount)
    }

    @Test
    fun `a failed admin action writes no row`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")

        /* Bad PIN, unknown user, manager changing a role: all rejected. */
        client.send("POST", "/api/users", manager, """{"username":"newbie","pin":"12"}""")
        client.send("POST", "/api/users/nobody/reset-pin", manager)
        client.send("PATCH", "/api/users/${client.userId(admin, "jvdm")}", manager, """{"role":2}""")

        assertEquals(emptyList(), client.rows("/api/audit-log", admin))
    }

    @Test
    fun `filters combine and a manager only sees their own site`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        client.syncMortality(worker, "A-1", "g-mine")
        client.syncMortality(other, "A-2", "g-other")
        client.void(manager, "g-mine")
        client.void(admin, "g-other")
        client.send("POST", "/api/users/${client.userId(admin, "jvdm")}/unbind-device", manager)

        assertEquals(3, client.rows("/api/audit-log", admin).size)
        assertEquals(listOf("g-mine"), client.rows("/api/audit-log?action=VOID&siteId=dev-site-1", admin).map { it.str("entityId") })
        assertEquals(listOf("g-other"), client.rows("/api/audit-log?entityType=MORTALITY&actorUserId=${client.userId(admin, "admin")}", admin).map { it.str("entityId") })
        assertEquals(1, client.rows("/api/audit-log?entityId=g-other", admin).size)
        assertEquals(0, client.rows("/api/audit-log?from=${System.currentTimeMillis() + 60_000}", admin).size)
        assertEquals(3, client.rows("/api/audit-log?to=${System.currentTimeMillis() + 60_000}", admin).size)

        val managerLog = client.rows("/api/audit-log", manager)
        assertEquals(setOf("g-mine"), managerLog.filter { it.str("action") == "VOID" }.map { it.str("entityId") }.toSet())
        assertTrue(managerLog.all { it.str("siteId") == "dev-site-1" })
        /* Their own site is fine; another site is refused. */
        assertEquals(managerLog.size, client.rows("/api/audit-log?siteId=dev-site-1", manager).size)
        assertEquals(HttpStatusCode.Forbidden, client.send("GET", "/api/audit-log?siteId=other-site", manager).status)
    }

    @Test
    fun `paging with before walks the log newest first and the limit is clamped`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        repeat(5) { client.send("POST", "/api/users", admin, """{"username":"user$it","pin":"12345","site_id":"dev-site-1"}""") }

        val first = client.rows("/api/audit-log?limit=2", admin)
        assertEquals(2, first.size)
        val second = client.rows("/api/audit-log?limit=2&before=${first.last().str("id")}", admin)
        val third = client.rows("/api/audit-log?limit=2&before=${second.last().str("id")}", admin)
        val ids = (first + second + third).map { it.str("id")!!.toLong() }

        assertEquals(5, ids.size)
        assertEquals(ids.sortedDescending(), ids)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(
            0,
            client.rows("/api/audit-log?before=${ids.last()}", admin).size
        )
        /* 0 and absurd limits are clamped to 1..500 rather than rejected. */
        assertEquals(1, client.rows("/api/audit-log?limit=0", admin).size)
        assertEquals(5, client.rows("/api/audit-log?limit=100000", admin).size)
    }

    @Test
    fun `workers and missing tokens are rejected and a bad number is a 400`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val admin = client.login("admin", "10001")

        assertEquals(HttpStatusCode.Forbidden, client.send("GET", "/api/audit-log", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/audit-log").status)
        assertEquals(HttpStatusCode.BadRequest, client.send("GET", "/api/audit-log?before=abc", admin).status)
        assertEquals(HttpStatusCode.BadRequest, client.send("GET", "/api/audit-log?from=yesterday", admin).status)
    }
}
