package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.CreateUserBody
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.UpdateUserBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ManagementApiClientTest {

    private class FakeTokenProvider(private val value: String?) : TokenProvider {
        override suspend fun token(): String? = value
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(token: String? = "tok", handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): ManagementApiClient {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, jsonHeaders)
        }
        return ManagementApiClient(
            tokenProvider = FakeTokenProvider(token),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        )
    }

    private val memberJson =
        """{"user_id":"u1","username":"jvdm","role":3,"site_id":"s1","active":true,"device_assigned_id":null,"extra":1}"""

    @Test
    fun `listUsers sends the bearer token and decodes members`() = runTest {
        var seen: HttpRequestData? = null
        val api = client {
            seen = it
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":[$memberJson]}"""
        }

        val result = api.listUsers() as ManagementResult.Success

        assertEquals("jvdm", result.value.single().username)
        assertEquals("Bearer tok", seen!!.headers[HttpHeaders.Authorization])
        assertEquals("http://test-host/api/users", seen!!.url.toString())
    }

    @Test
    fun `no token means unauthorized without calling the server`() = runTest {
        var called = false
        val api = client(token = null) {
            called = true
            HttpStatusCode.OK to "{}"
        }

        assertEquals(ManagementResult.Unauthorized, api.listUsers())
        assertFalse(called)
    }

    @Test
    fun `createUser posts the body and omits unset fields`() = runTest {
        var body = ""
        var method: HttpMethod? = null
        val api = client {
            method = it.method
            body = String(it.body.toByteArray())
            HttpStatusCode.Created to """{"success":true,"message":"ok","data":$memberJson}"""
        }

        val result = api.createUser(CreateUserBody(username = "jvdm", pin = "12345"))

        assertTrue(result is ManagementResult.Success)
        assertEquals(HttpMethod.Post, method)
        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals(setOf("username", "pin"), json.keys)
    }

    @Test
    fun `updateUser patches only the given field`() = runTest {
        var body = ""
        var url = ""
        var method: HttpMethod? = null
        val api = client {
            method = it.method
            url = it.url.toString()
            body = String(it.body.toByteArray())
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":$memberJson}"""
        }

        api.updateUser("u1", UpdateUserBody(active = false))

        assertEquals(HttpMethod.Patch, method)
        assertEquals("http://test-host/api/users/u1", url)
        assertEquals(setOf("active"), Json.parseToJsonElement(body).jsonObject.keys)
    }

    @Test
    fun `resetPin returns the generated pin`() = runTest {
        val api = client {
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"pin":"48213"}}"""
        }

        val result = api.resetPin("u1") as ManagementResult.Success

        assertEquals("48213", result.value.pin)
    }

    @Test
    fun `status codes map to results with the server message`() = runTest {
        fun body(message: String) = """{"success":false,"message":"$message"}"""

        assertEquals(
            ManagementResult.Forbidden("Forbidden"),
            client { HttpStatusCode.Forbidden to body("Forbidden") }.listUsers()
        )
        assertEquals(
            ManagementResult.NotFound,
            client { HttpStatusCode.NotFound to body("User not found") }.unbindDevice("u1")
        )
        assertEquals(
            ManagementResult.Rejected("Username already taken"),
            client { HttpStatusCode.Conflict to body("Username already taken") }
                .createUser(CreateUserBody("jvdm", "12345"))
        )
        assertEquals(
            ManagementResult.Rejected("PIN must be exactly 5 digits"),
            client { HttpStatusCode.BadRequest to body("PIN must be exactly 5 digits") }.resetPin("u1", "12")
        )
        assertEquals(
            ManagementResult.Unauthorized,
            client { HttpStatusCode.Unauthorized to body("Invalid token") }.listUsers()
        )
        assertEquals(
            ManagementResult.Error("Server error: 500"),
            client { HttpStatusCode.InternalServerError to "oops" }.listUsers()
        )
    }

    @Test
    fun `a network failure becomes NoConnection`() = runTest {
        val api = ManagementApiClient(
            tokenProvider = FakeTokenProvider("tok"),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(MockEngine { throw IOException("offline") })
        )

        assertEquals(ManagementResult.NoConnection, api.listUsers())
    }
}
