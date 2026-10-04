package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.CreateReferenceBody
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceDataApiClientTest {

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
    fun `referenceData sends the version only when given and decodes both shapes`() = runTest {
        val urls = mutableListOf<String>()
        var body = """{"success":true,"message":"ok","data":{"version":3,"unchanged":true}}"""
        val api = client { urls += it.url.toString(); HttpStatusCode.OK to body }

        val unchanged = api.referenceData(ifVersion = 3)
        body = """{"success":true,"message":"ok","data":{"version":4,"diseases":[{"id":1,"name":"Anthrax","active":false}],
            "costTypes":[{"code":"FEED","displayName":"Feed","sortOrder":5,"active":true}],"extra":1}}"""
        val full = api.referenceData()

        assertEquals("http://test-host/api/reference-data?ifVersion=3", urls[0])
        assertEquals("http://test-host/api/reference-data", urls[1])
        assertTrue((unchanged as ManagementResult.Success).value.unchanged)
        assertNull(unchanged.value.diseases)
        val snapshot = (full as ManagementResult.Success).value
        assertEquals(false, snapshot.unchanged)
        assertEquals(false, snapshot.diseases!!.single().active)
        assertEquals("FEED", snapshot.costTypes!!.single().code)
        assertNull(snapshot.treatmentTypes)
    }

    @Test
    fun `create and set active use the right methods, paths and bodies`() = runTest {
        val seen = mutableListOf<Triple<HttpMethod, String, String>>()
        val api = client {
            seen += Triple(it.method, it.url.encodedPath, String(it.body.toByteArray()))
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"version":9,"item":{"kind":"diseases","id":"1","name":"Anthrax","active":true}}}"""
        }

        api.createReferenceValue("diseases", CreateReferenceBody(name = "Anthrax"))
        api.createReferenceValue("cost-types", CreateReferenceBody(code = "AUCTION", displayName = "Auction fees", sortOrder = 3))
        val change = api.setReferenceActive("treatment-types", "5", false)

        assertEquals(Triple(HttpMethod.Post, "/api/reference-data/diseases", """{"name":"Anthrax"}"""), seen[0])
        assertEquals(
            Triple(HttpMethod.Post, "/api/reference-data/cost-types", """{"code":"AUCTION","displayName":"Auction fees","sortOrder":3}"""),
            seen[1]
        )
        assertEquals(Triple(HttpMethod.Patch, "/api/reference-data/treatment-types/5", """{"active":false}"""), seen[2])
        assertEquals(9L, (change as ManagementResult.Success).value.version)
    }

    @Test
    fun `a 409 comes back as a rejection with the server's message`() = runTest {
        val api = client { HttpStatusCode.Conflict to """{"success":false,"message":"That disease already exists"}""" }

        assertEquals(
            ManagementResult.Rejected("That disease already exists"),
            api.createReferenceValue("diseases", CreateReferenceBody(name = "Anthrax"))
        )
    }
}
