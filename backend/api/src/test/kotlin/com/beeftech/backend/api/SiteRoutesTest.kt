package com.beeftech.backend.api

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
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SiteRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-sites-test", ".db")
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

    private fun dataOf(text: String) = Json.parseToJsonElement(text).jsonObject["data"]!!

    private suspend fun HttpClient.sites(token: String): List<JsonObject> =
        dataOf(send("GET", "/api/sites", token).bodyAsText()).jsonArray.map { it.jsonObject }

    private suspend fun HttpClient.auditRows(token: String): List<JsonObject> =
        dataOf(send("GET", "/api/audit-log", token).bodyAsText()).jsonArray.map { it.jsonObject }

    private fun JsonObject.str(key: String) = this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

    private val nextCode = java.util.concurrent.atomic.AtomicInteger(100)

    /* Each call gets its own code unless one is given, so tests that do not care about it never collide. */
    private suspend fun HttpClient.createSite(
        token: String,
        name: String,
        farmCode: String = "T" + nextCode.incrementAndGet().toString(36).uppercase().padStart(3, '0')
    ): HttpResponse =
        send("POST", "/api/sites", token, """{"name":"$name","farmCode":"$farmCode"}""")

    private suspend fun HttpClient.siteIdOf(response: HttpResponse) = dataOf(response.bodyAsText()).jsonObject.str("siteId")!!

    @Test
    fun `admin lists all sites with active user counts and a manager only their own`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val manager = client.login("fmanager", "20002")
        client.createSite(admin, "Second Farm")

        val adminSites = client.sites(admin)
        assertEquals(listOf("Dev Feedlot", "Second Farm"), adminSites.map { it.str("name") })
        val dev = adminSites.first { it.str("siteId") == "dev-site-1" }
        assertEquals("2", dev.str("activeUserCount"))
        assertEquals("true", dev.str("active"))
        assertEquals("0", adminSites.first { it.str("name") == "Second Farm" }.str("activeUserCount"))

        assertEquals(listOf("dev-site-1"), client.sites(manager).map { it.str("siteId") })
    }

    @Test
    fun `workers and missing tokens are rejected and managers cannot write`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")

        assertEquals(HttpStatusCode.Forbidden, client.send("GET", "/api/sites", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/sites").status)
        assertEquals(HttpStatusCode.Forbidden, client.createSite(manager, "Mine").status)
        assertEquals(
            HttpStatusCode.Forbidden,
            client.send("PATCH", "/api/sites/dev-site-1", manager, """{"name":"Renamed"}""").status
        )
        assertEquals("Dev Feedlot", client.sites(manager).single().str("name"))
    }

    @Test
    fun `creating a site generates its id and checks the name`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")

        val created = client.createSite(admin, "  North Pens  ")
        assertEquals(HttpStatusCode.Created, created.status)
        val site = dataOf(created.bodyAsText()).jsonObject
        assertTrue(Regex("site-[0-9a-f]{8}").matches(site.str("siteId")!!), site.str("siteId"))
        assertEquals("North Pens", site.str("name"))
        assertEquals("true", site.str("active"))

        val other = client.siteIdOf(client.createSite(admin, "South Pens"))
        assertNotEquals(site.str("siteId"), other)

        assertEquals(HttpStatusCode.Conflict, client.createSite(admin, "north pens").status)
        assertEquals(HttpStatusCode.Conflict, client.createSite(admin, "dev feedlot").status)
        assertEquals(HttpStatusCode.BadRequest, client.createSite(admin, "   ").status)
        assertEquals(HttpStatusCode.BadRequest, client.createSite(admin, "x".repeat(101)).status)
    }

    @Test
    fun `a site needs a unique four character farm code that is stored upper case`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")

        val created = client.createSite(admin, "Coded", farmCode = " bf01 ")
        assertEquals(HttpStatusCode.Created, created.status)
        assertEquals("BF01", dataOf(created.bodyAsText()).jsonObject.str("farmCode"))

        assertEquals(HttpStatusCode.Conflict, client.createSite(admin, "Other", farmCode = "BF01").status)
        assertEquals(HttpStatusCode.BadRequest, client.createSite(admin, "Short", farmCode = "BF1").status)
        assertEquals(HttpStatusCode.BadRequest, client.createSite(admin, "Long", farmCode = "BF012").status)
        assertEquals(HttpStatusCode.BadRequest, client.createSite(admin, "Symbol", farmCode = "BF-1").status)
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send("POST", "/api/sites", admin, """{"name":"No code"}""").status
        )
        assertEquals("S001", client.sites(admin).first { it.str("siteId") == "dev-site-1" }.str("farmCode"))
    }

    @Test
    fun `an admin can change a farm code but not to one another site has`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val north = client.siteIdOf(client.createSite(admin, "North", farmCode = "NRTH"))

        assertEquals(HttpStatusCode.Conflict, client.send("PATCH", "/api/sites/$north", admin, """{"farmCode":"S001"}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.send("PATCH", "/api/sites/$north", admin, """{"farmCode":"no"}""").status)
        assertEquals(HttpStatusCode.OK, client.send("PATCH", "/api/sites/$north", admin, """{"farmCode":"NRTH"}""").status)

        val changed = client.send("PATCH", "/api/sites/$north", admin, """{"farmCode":"nr02"}""")
        assertEquals("NR02", dataOf(changed.bodyAsText()).jsonObject.str("farmCode"))
    }

    @Test
    fun `login returns the site's farm code`() = testApplication {
        startApp()
        val client = createClient { }
        val text = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"jvdm","pin":"30003","device_id":"dev-jvdm"}""")
        }.bodyAsText()

        val user = Json.parseToJsonElement(text).jsonObject["data"]!!.jsonObject["user"]!!.jsonObject
        assertEquals("S001", user.str("farm_code"))
    }

    @Test
    fun `renaming checks for duplicates and a same-name patch is not a conflict`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val north = client.siteIdOf(client.createSite(admin, "North"))
        client.createSite(admin, "South")

        assertEquals(HttpStatusCode.OK, client.send("PATCH", "/api/sites/$north", admin, """{"name":"North"}""").status)
        assertEquals(HttpStatusCode.OK, client.send("PATCH", "/api/sites/$north", admin, """{"name":"NORTH"}""").status)
        assertEquals(HttpStatusCode.Conflict, client.send("PATCH", "/api/sites/$north", admin, """{"name":"south"}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.send("PATCH", "/api/sites/$north", admin, """{"name":" "}""").status)
        assertEquals(HttpStatusCode.NotFound, client.send("PATCH", "/api/sites/nope", admin, """{"name":"Z"}""").status)

        val renamed = client.send("PATCH", "/api/sites/$north", admin, """{"name":"North Farm"}""")
        assertEquals("North Farm", dataOf(renamed.bodyAsText()).jsonObject.str("name"))
        assertNotEquals(null, dataOf(renamed.bodyAsText()).jsonObject.str("updatedAt"))
    }

    @Test
    fun `a site with active users cannot be deactivated until they are moved or deactivated`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val north = client.siteIdOf(client.createSite(admin, "North"))
        client.send("POST", "/api/users", admin, """{"username":"nora","pin":"12345","site_id":"$north"}""")

        val blocked = client.send("PATCH", "/api/sites/$north", admin, """{"active":false}""")
        assertEquals(HttpStatusCode.Conflict, blocked.status)
        assertTrue("1 active user" in blocked.bodyAsText())

        val norId = dataOf(client.send("GET", "/api/users", admin).bodyAsText()).jsonArray
            .first { it.jsonObject["username"]!!.jsonPrimitive.content == "nora" }.jsonObject["user_id"]!!.jsonPrimitive.content
        client.send("PATCH", "/api/users/$norId", admin, """{"active":false}""")

        val deactivated = client.send("PATCH", "/api/sites/$north", admin, """{"active":false}""")
        assertEquals(HttpStatusCode.OK, deactivated.status)
        assertEquals("false", dataOf(deactivated.bodyAsText()).jsonObject.str("active"))

        val reactivated = client.send("PATCH", "/api/sites/$north", admin, """{"active":true}""")
        assertEquals("true", dataOf(reactivated.bodyAsText()).jsonObject.str("active"))
    }

    @Test
    fun `an inactive site takes no new users but existing users and role changes keep working`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val north = client.siteIdOf(client.createSite(admin, "North"))
        client.send("POST", "/api/users", admin, """{"username":"nora","pin":"12345","site_id":"$north"}""")
        val noraId = dataOf(client.send("GET", "/api/users", admin).bodyAsText()).jsonArray
            .first { it.jsonObject["username"]!!.jsonPrimitive.content == "nora" }.jsonObject["user_id"]!!.jsonPrimitive.content
        /* A site can only be deactivated with no active users, so park nora, deactivate, then bring her back. */
        client.send("PATCH", "/api/users/$noraId", admin, """{"active":false}""")
        assertEquals(HttpStatusCode.OK, client.send("PATCH", "/api/sites/$north", admin, """{"active":false}""").status)
        assertEquals(HttpStatusCode.OK, client.send("PATCH", "/api/users/$noraId", admin, """{"active":true}""").status)

        /* No new users, and nobody can be moved onto it. */
        val create = client.send("POST", "/api/users", admin, """{"username":"newbie","pin":"12345","site_id":"$north"}""")
        assertEquals(HttpStatusCode.BadRequest, create.status)
        assertTrue("inactive" in create.bodyAsText())
        val jvdmId = dataOf(client.send("GET", "/api/users", admin).bodyAsText()).jsonArray
            .first { it.jsonObject["username"]!!.jsonPrimitive.content == "jvdm" }.jsonObject["user_id"]!!.jsonPrimitive.content
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send("PATCH", "/api/users/$jvdmId", admin, """{"site_id":"$north"}""").status
        )

        /* Someone already there can still sign in and have their role changed. */
        assertEquals(north, client.sites(admin).first { it.str("siteId") == north }.str("siteId"))
        client.login("nora", "12345")
        assertEquals(HttpStatusCode.OK, client.send("PATCH", "/api/users/$noraId", admin, """{"role":2}""").status)
    }

    @Test
    fun `an unknown site on user create is still rejected`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")

        val response = client.send("POST", "/api/users", admin, """{"username":"nora","pin":"12345","site_id":"nope"}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue("Unknown site" in response.bodyAsText())
    }

    @Test
    fun `site changes are audited and a no-op leaves no row`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")
        val manager = client.login("fmanager", "20002")
        val north = client.siteIdOf(client.createSite(admin, "North"))
        client.send("PATCH", "/api/sites/$north", admin, """{"name":"North Farm"}""")
        client.send("PATCH", "/api/sites/$north", admin, """{"name":"North Farm"}""")
        client.send("PATCH", "/api/sites/$north", admin, """{"active":false}""")
        client.createSite(admin, "North Farm")

        val log = client.auditRows(admin)
        assertEquals(listOf("SITE_UPDATE", "SITE_UPDATE", "SITE_CREATE"), log.map { it.str("action") })
        assertTrue(log.all { it.str("entityType") == "SITE" && it.str("entityId") == north && it.str("siteId") == north })
        assertEquals("admin", log[0].str("actorUsername"))
        assertTrue("true->false" in log[0].str("details")!!)
        assertTrue("North->North Farm" in log[1].str("details")!!)
        assertTrue("North" in log[2].str("details")!!)

        /* A manager only reads the log for their own site, so these rows stay hidden from them. */
        assertEquals(emptyList(), client.auditRows(manager).filter { it.str("entityType") == "SITE" })
    }

    @Test
    fun `a deactivated manager can no longer read sites`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val managerId = dataOf(client.send("GET", "/api/users", admin).bodyAsText()).jsonArray
            .first { it.jsonObject["username"]!!.jsonPrimitive.content == "fmanager" }.jsonObject["user_id"]!!.jsonPrimitive.content
        client.send("PATCH", "/api/users/$managerId", admin, """{"active":false}""")

        assertEquals(HttpStatusCode.Unauthorized, client.send("GET", "/api/sites", manager).status)
    }
}
