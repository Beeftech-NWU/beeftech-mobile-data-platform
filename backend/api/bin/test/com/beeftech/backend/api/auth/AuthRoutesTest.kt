package com.beeftech.backend.api.auth

import com.beeftech.backend.api.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthRoutesTest {

    private fun uniqueTestDbUrl(): String {
        val tempFile = Files.createTempFile("beeftech-auth-test", ".db")
        tempFile.toFile().deleteOnExit()
        return "jdbc:sqlite:${tempFile}"
    }

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    @Test
    fun `correct username and pin returns 200 with token and pin_hash`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }

        val client = createClient { }

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"jvdm","pin":"30003","device_id":"DEV_01"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("true", json["success"]!!.jsonPrimitive.content)

        val data = json["data"]!!.jsonObject
        assertNotNull(data["token"]!!.jsonPrimitive.content)

        val user = data["user"]!!.jsonObject
        assertEquals("jvdm", user["username"]!!.jsonPrimitive.content)
        assertNotNull(user["pin_hash"]!!.jsonPrimitive.content)
        assertEquals("DEV_01", user["device_assigned_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `wrong pin returns 401`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }

        val client = createClient { }

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"jvdm","pin":"99999","device_id":"DEV_01"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Incorrect username or PIN", json["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `unknown username returns 401 with same message`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }

        val client = createClient { }

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"unknown_user","pin":"10001","device_id":"DEV_01"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Incorrect username or PIN", json["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `multiple wrong attempts lead to 423 locked`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }

        val client = createClient { }

        val username = "fmanager"
        repeat(5) {
            val res = client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"$username","pin":"wrong","device_id":"DEV_01"}""")
            }
            if (it < 4) {
                assertEquals(HttpStatusCode.Unauthorized, res.status)
            }
        }

        val lockedResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","pin":"wrong","device_id":"DEV_01"}""")
        }

        assertEquals(HttpStatusCode.Locked, lockedResponse.status)
        val json = Json.parseToJsonElement(lockedResponse.bodyAsText()).jsonObject
        assertEquals("Account locked", json["message"]!!.jsonPrimitive.content)
        assertNotNull(json["data"]!!.jsonObject["remainingSeconds"])
    }

    @Test
    fun `second device for claimed user returns 409`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }

        val client = createClient { }

        // First device claims
        val res1 = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"DEV_FIRST"}""")
        }
        assertEquals(HttpStatusCode.OK, res1.status)

        // Second device attempts
        val res2 = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"DEV_SECOND"}""")
        }
        assertEquals(HttpStatusCode.Conflict, res2.status)
        val json = Json.parseToJsonElement(res2.bodyAsText()).jsonObject
        assertEquals("This phone is registered to another worker", json["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `same device logging in again succeeds`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }

        val client = createClient { }

        val res1 = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"DEV_SAME"}""")
        }
        assertEquals(HttpStatusCode.OK, res1.status)

        val res2 = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"DEV_SAME"}""")
        }
        assertEquals(HttpStatusCode.OK, res2.status)
    }

    @Test
    fun `profile endpoint authentication`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }

        val client = createClient { }

        // Without token -> 401
        val unauthRes = client.get("/api/profile")
        assertEquals(HttpStatusCode.Unauthorized, unauthRes.status)

        // Login first
        val loginRes = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"jvdm","pin":"30003","device_id":"DEV_01"}""")
        }
        val json = Json.parseToJsonElement(loginRes.bodyAsText()).jsonObject
        val token = json["data"]!!.jsonObject["token"]!!.jsonPrimitive.content

        // With token -> 200
        val profileRes = client.get("/api/profile") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, profileRes.status)
        assertTrue(profileRes.bodyAsText().contains("jvdm"))
    }
}
