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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserAdminRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-useradmin-test", ".db")
        file.toFile().deleteOnExit()
        System.setProperty("beeftech.db.url", "jdbc:sqlite:$file")
        application { module() }
    }

    private suspend fun HttpClient.rawLogin(username: String, pin: String, device: String = "dev-$username"): HttpResponse =
        post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","pin":"$pin","device_id":"$device"}""")
        }

    private suspend fun HttpClient.login(username: String, pin: String): String {
        val text = rawLogin(username, pin).bodyAsText()
        val data = Json.parseToJsonElement(text).jsonObject["data"]
        require(data != null && data !is JsonNull) { "login $username failed: $text" }
        return data.jsonObject["token"]!!.jsonPrimitive.content
    }

    private suspend fun HttpClient.send(
        method: String,
        path: String,
        token: String,
        body: String? = null
    ): HttpResponse {
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

    private suspend fun HttpClient.userId(adminToken: String, username: String): String =
        dataOf(send("GET", "/api/users", adminToken).bodyAsText()).jsonArray
            .first { it.jsonObject["username"]!!.jsonPrimitive.content == username }
            .jsonObject["user_id"]!!.jsonPrimitive.content

    /* The module loads lazily, so touch it before using repositories. */
    private suspend fun HttpClient.seedOtherSite() {
        get("/health")
        transaction(DatabaseFactory.getDatabase()) {
            com.beeftech.backend.api.auth.SitesTable.insert {
                it[siteId] = "other-site"
                it[name] = "Other"
                it[createdAt] = 0L
            }
        }
        val repo = UserRepository()
        repo.insertUser("other-worker", "other", PinHasher.hash("40004"), 3, siteId = "other-site")
        repo.insertUser("other-manager", "othermgr", PinHasher.hash("50005"), 2, siteId = "other-site")
    }

    @Test
    fun `workers and missing tokens are rejected`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        assertEquals(HttpStatusCode.Forbidden, client.send("GET", "/api/users", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/users").status)
    }

    @Test
    fun `manager lists only workers on their own site and admin lists everyone`() = testApplication {
        startApp()
        val client = createClient { }
        client.seedOtherSite()

        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")

        val managerNames = dataOf(client.send("GET", "/api/users", manager).bodyAsText()).jsonArray
            .map { it.jsonObject["username"]!!.jsonPrimitive.content }
        assertEquals(listOf("jvdm"), managerNames)

        val adminText = client.send("GET", "/api/users", admin).bodyAsText()
        assertTrue("other" in adminText && "fmanager" in adminText && "admin" in adminText)
        assertFalse("pin_hash" in adminText)
    }

    @Test
    fun `manager creates a worker on their own site and the worker can log in`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")

        val created = client.send(
            "POST", "/api/users", manager,
            """{"username":"newbie","pin":"12345"}"""
        )
        assertEquals(HttpStatusCode.Created, created.status)
        val user = dataOf(created.bodyAsText()).jsonObject
        assertEquals("3", user["role"]!!.jsonPrimitive.content)
        assertEquals("dev-site-1", user["site_id"]!!.jsonPrimitive.content)

        assertEquals(HttpStatusCode.OK, client.rawLogin("newbie", "12345").status)
    }

    @Test
    fun `manager cannot create managers or users on another site`() = testApplication {
        startApp()
        val client = createClient { }
        client.seedOtherSite()
        val manager = client.login("fmanager", "20002")

        assertEquals(
            HttpStatusCode.Forbidden,
            client.send("POST", "/api/users", manager, """{"username":"boss","pin":"12345","role":2}""").status
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            client.send("POST", "/api/users", manager, """{"username":"elsewhere","pin":"12345","site_id":"other-site"}""").status
        )
    }

    @Test
    fun `create validates username pin and uniqueness`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")

        assertEquals(
            HttpStatusCode.BadRequest,
            client.send("POST", "/api/users", manager, """{"username":"abc","pin":"123"}""").status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send("POST", "/api/users", manager, """{"username":"a b","pin":"12345"}""").status
        )
        assertEquals(
            HttpStatusCode.Conflict,
            client.send("POST", "/api/users", manager, """{"username":"jvdm","pin":"12345"}""").status
        )
    }

    @Test
    fun `admin can create a manager only on a real site`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")

        assertEquals(
            HttpStatusCode.BadRequest,
            client.send("POST", "/api/users", admin, """{"username":"mgr2","pin":"12345","role":2}""").status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send("POST", "/api/users", admin, """{"username":"mgr2","pin":"12345","role":2,"site_id":"nope"}""").status
        )
        assertEquals(
            HttpStatusCode.Created,
            client.send("POST", "/api/users", admin, """{"username":"mgr2","pin":"12345","role":2,"site_id":"dev-site-1"}""").status
        )
    }

    @Test
    fun `deactivated user cannot log in and manager cannot touch other sites`() = testApplication {
        startApp()
        val client = createClient { }
        client.seedOtherSite()
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")

        val workerId = client.userId(admin, "jvdm")
        val otherId = client.userId(admin, "other")
        val otherManagerId = client.userId(admin, "othermgr")

        assertEquals(
            HttpStatusCode.OK,
            client.send("PATCH", "/api/users/$workerId", manager, """{"active":false}""").status
        )
        val blocked = client.rawLogin("jvdm", "30003")
        assertEquals(HttpStatusCode.Unauthorized, blocked.status)
        assertTrue("deactivated" in blocked.bodyAsText())
        /* A wrong PIN must not reveal that the account is deactivated. */
        assertFalse("deactivated" in client.rawLogin("jvdm", "00000").bodyAsText())

        assertEquals(HttpStatusCode.NotFound, client.send("PATCH", "/api/users/$otherId", manager, """{"active":false}""").status)
        assertEquals(HttpStatusCode.NotFound, client.send("PATCH", "/api/users/$otherManagerId", manager, """{"active":false}""").status)
        assertEquals(HttpStatusCode.NotFound, client.send("POST", "/api/users/$otherId/reset-pin", manager).status)
        assertEquals(HttpStatusCode.NotFound, client.send("POST", "/api/users/$otherId/unbind-device", manager).status)

        assertEquals(
            HttpStatusCode.OK,
            client.send("PATCH", "/api/users/$workerId", manager, """{"active":true}""").status
        )
        assertEquals(HttpStatusCode.OK, client.rawLogin("jvdm", "30003").status)
    }

    @Test
    fun `manager cannot change role or site and nobody can deactivate themselves`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val workerId = client.userId(admin, "jvdm")
        val adminId = client.userId(admin, "admin")

        assertEquals(
            HttpStatusCode.Forbidden,
            client.send("PATCH", "/api/users/$workerId", manager, """{"role":2}""").status
        )
        assertEquals(
            HttpStatusCode.Conflict,
            client.send("PATCH", "/api/users/$adminId", admin, """{"active":false}""").status
        )
        assertEquals(
            HttpStatusCode.Conflict,
            client.send("PATCH", "/api/users/$adminId", admin, """{"role":3}""").status
        )
    }

    @Test
    fun `admin promotes a worker and a deactivated manager loses access immediately`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val managerId = client.userId(admin, "fmanager")
        val workerId = client.userId(admin, "jvdm")

        val promoted = client.send("PATCH", "/api/users/$workerId", admin, """{"role":2}""")
        assertEquals("2", dataOf(promoted.bodyAsText()).jsonObject["role"]!!.jsonPrimitive.content)

        client.send("PATCH", "/api/users/$managerId", admin, """{"active":false}""")
        /* The manager's token would last 24 h, but deactivating ended their session at once. */
        assertEquals(HttpStatusCode.Unauthorized, client.send("GET", "/api/users", manager).status)
    }

    @Test
    fun `reset pin generates or accepts a pin and the old one stops working`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val workerId = client.userId(admin, "jvdm")

        val generated = client.send("POST", "/api/users/$workerId/reset-pin", manager)
        assertEquals(HttpStatusCode.OK, generated.status)
        val pin = dataOf(generated.bodyAsText()).jsonObject["pin"]!!.jsonPrimitive.content
        assertTrue(Regex("\\d{5}").matches(pin))
        assertEquals(HttpStatusCode.OK, client.rawLogin("jvdm", pin).status)

        client.send("POST", "/api/users/$workerId/reset-pin", manager, """{"pin":"54321"}""")
        assertEquals(HttpStatusCode.OK, client.rawLogin("jvdm", "54321").status)
        assertEquals(HttpStatusCode.Unauthorized, client.rawLogin("jvdm", pin).status)

        assertEquals(
            HttpStatusCode.BadRequest,
            client.send("POST", "/api/users/$workerId/reset-pin", manager, """{"pin":"12"}""").status
        )
    }

    @Test
    fun `unbind device lets the worker log in from a new phone`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val workerId = client.userId(admin, "jvdm")

        client.login("jvdm", "30003")
        assertEquals(HttpStatusCode.Conflict, client.rawLogin("jvdm", "30003", "new-phone").status)

        val unbound = client.send("POST", "/api/users/$workerId/unbind-device", manager)
        assertEquals(HttpStatusCode.OK, unbound.status)
        assertNull(dataOf(unbound.bodyAsText()).jsonObject["device_assigned_id"]?.takeIf { it !is JsonNull })
        assertEquals(HttpStatusCode.OK, client.rawLogin("jvdm", "30003", "new-phone").status)
        assertNotNull(client.userId(admin, "jvdm"))
    }

    @Test
    fun `register stub is gone`() = testApplication {
        startApp()
        val client = createClient { }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"x","password":"y"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
