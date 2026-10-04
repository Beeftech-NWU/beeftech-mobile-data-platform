package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicesApiClientTest {

    private val deviceJson =
        """{"deviceId":"d1","model":"Pixel","status":"REVOKED","firstSeenAt":1,"lastSeenAt":2,"revokeReason":"Lost","boundUsernames":["jvdm"],"extra":1}"""

    private fun client(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): ManagementApiClient {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return ManagementApiClient(
            tokenProvider = object : TokenProvider {
                override suspend fun token(): String? = "tok"
            },
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        )
    }

    @Test
    fun `devices decodes phones and sends the status filter only when given`() = runTest {
        val urls = mutableListOf<String>()
        val api = client { urls += it.url.toString(); HttpStatusCode.OK to """{"success":true,"message":"ok","data":[$deviceJson]}""" }

        val all = api.devices()
        api.devices("REVOKED")

        assertEquals("http://test-host/api/devices", urls[0])
        assertEquals("http://test-host/api/devices?status=REVOKED", urls[1])
        val device = (all as ManagementResult.Success).value.single()
        assertTrue(device.isRevoked)
        assertEquals(listOf("jvdm"), device.boundUsernames)
        assertEquals("Lost", device.revokeReason)
    }

    @Test
    fun `revoke and reinstate post the reason to the phone's path`() = runTest {
        val seen = mutableListOf<Pair<String, String>>()
        val api = client {
            seen += it.url.encodedPath to String(it.body.toByteArray())
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":$deviceJson}"""
        }

        api.revokeDevice("d1", "Stolen")
        api.reinstateDevice("d1", "Found")

        assertEquals(listOf("/api/devices/d1/revoke" to """{"reason":"Stolen"}""", "/api/devices/d1/reinstate" to """{"reason":"Found"}"""), seen)
    }

    @Test
    fun `login events and lockouts and unlock use the right paths and parameters`() = runTest {
        val seen = mutableListOf<Pair<HttpMethod, String>>()
        val api = client {
            seen += it.method to it.url.toString()
            when {
                it.url.encodedPath.endsWith("lockouts") ->
                    HttpStatusCode.OK to """{"success":true,"message":"ok","data":[{"username":"jvdm","userId":"u1","failedAttempts":5,"lockedUntil":9}]}"""
                it.method == HttpMethod.Post ->
                    HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"user_id":"u1","username":"jvdm","role":3,"active":true}}"""
                else ->
                    HttpStatusCode.OK to """{"success":true,"message":"ok","data":[{"id":7,"createdAt":1,"usernameAttempted":"jvdm","deviceId":"d","outcome":"SUCCESS"}]}"""
            }
        }

        val events = api.loginEvents(outcome = "SUCCESS", before = 50)
        val lockouts = api.lockouts()
        api.unlockLogin("u1")

        assertEquals("http://test-host/api/login-events?outcome=SUCCESS&before=50&limit=50", seen[0].second)
        assertEquals("http://test-host/api/login-security/lockouts", seen[1].second)
        assertEquals(HttpMethod.Post to "http://test-host/api/users/u1/unlock-login", seen[2])
        assertEquals(7L, (events as ManagementResult.Success).value.single().id)
        assertEquals("u1", (lockouts as ManagementResult.Success).value.single().userId)
    }

    @Test
    fun `a 409 on revoke comes back as a rejection with the server's message`() = runTest {
        val api = client { HttpStatusCode.Conflict to """{"success":false,"message":"That device is already revoked"}""" }

        assertEquals(ManagementResult.Rejected("That device is already revoked"), api.revokeDevice("d1", "x"))
    }
}
