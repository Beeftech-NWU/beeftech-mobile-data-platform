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

class SitesApiClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val siteJson =
        """{"siteId":"site-1","name":"North","active":true,"createdAt":5,"updatedAt":null,"activeUserCount":2,"extra":1}"""

    private fun client(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): ManagementApiClient {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, jsonHeaders)
        }
        return ManagementApiClient(
            tokenProvider = object : TokenProvider {
                override suspend fun token(): String? = "tok"
            },
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        )
    }

    @Test
    fun `listSites sends the bearer token and decodes sites`() = runTest {
        var seen: HttpRequestData? = null
        val api = client {
            seen = it
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":[$siteJson]}"""
        }

        val result = api.listSites()

        assertEquals("http://test-host/api/sites", seen!!.url.toString())
        assertEquals("Bearer tok", seen!!.headers[HttpHeaders.Authorization])
        val site = (result as ManagementResult.Success).value.single()
        assertEquals("North", site.name)
        assertEquals(2L, site.activeUserCount)
        assertTrue(site.active)
    }

    @Test
    fun `createSite posts the name and farm code`() = runTest {
        var seen: HttpRequestData? = null
        var body = ""
        val api = client {
            seen = it
            body = String(it.body.toByteArray())
            HttpStatusCode.Created to """{"success":true,"message":"ok","data":$siteJson}"""
        }

        val result = api.createSite("North", "NRTH")

        assertEquals(HttpMethod.Post, seen!!.method)
        assertEquals("http://test-host/api/sites", seen!!.url.toString())
        assertEquals("""{"name":"North","farmCode":"NRTH"}""", body)
        assertEquals("site-1", (result as ManagementResult.Success).value.siteId)
    }

    @Test
    fun `updateSite sends only the fields that change`() = runTest {
        val bodies = mutableListOf<String>()
        var seen: HttpRequestData? = null
        val api = client {
            seen = it
            bodies += String(it.body.toByteArray())
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":$siteJson}"""
        }

        api.updateSite("site-1", name = "North Farm")
        api.updateSite("site-1", active = false)
        api.updateSite("site-1", farmCode = "NR02")

        assertEquals(HttpMethod.Patch, seen!!.method)
        assertEquals("http://test-host/api/sites/site-1", seen!!.url.toString())
        assertEquals("""{"name":"North Farm"}""", bodies[0])
        assertEquals("""{"active":false}""", bodies[1])
        assertEquals("""{"farmCode":"NR02"}""", bodies[2])
    }

    @Test
    fun `a 409 comes back as a rejection with the server's message`() = runTest {
        val api = client {
            HttpStatusCode.Conflict to """{"success":false,"message":"This site still has 2 active user(s). Move or deactivate them first."}"""
        }

        val result = api.updateSite("site-1", active = false)

        assertEquals(
            ManagementResult.Rejected("This site still has 2 active user(s). Move or deactivate them first."),
            result
        )
    }
}
