package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.fakes.FakeTokenProvider
import com.beeftech.database.entity.CalfRegistrationEntity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalfRegistrationApiClientTest {

    private fun buildCalf(animalId: String = "RMB12345") = CalfRegistrationEntity(
        registrationId = "guid-1",
        registeredAnimalId = animalId,
        damId = null,
        sireId = null,
        birthWeightKg = null,
        calvingEase = null,
        registrationDate = "2023-01-01"
    )

    @Test
    fun `syncCalves sends bearer token from TokenProvider to sync endpoint`() = runTest {
        var capturedAuthHeader: String? = null
        var requestCount = 0

        val mockEngine = MockEngine { request ->
            requestCount++
            when {
                request.url.encodedPath.endsWith("/api/calf-registrations/sync") -> {
                    capturedAuthHeader = request.headers[HttpHeaders.Authorization]

                    respond(
                        content = """
                            {"success":true,"message":"Sync complete","data":{"results":[
                                {"recordguid":"guid-1","animalId":"RMB12345","status":"SYNCED","serverSyncedAt":999,"message":null}
                            ]}}
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                else -> error("Unhandled request: ${request.url}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val apiClient = CalfRegistrationApiClient(
            tokenProvider = FakeTokenProvider("test-token"),
            baseUrl = "http://test-host/",
            httpClient = httpClient
        )

        val result = apiClient.syncCalves(listOf(buildCalf()), deviceId = "TEST-DEVICE")

        assertTrue(result.isSuccess)
        assertEquals(1, requestCount)
        assertEquals("Bearer test-token", capturedAuthHeader)

        val response = result.getOrThrow()
        assertEquals(1, response.results.size)
        assertEquals("SYNCED", response.results.first().status)
        assertEquals("RMB12345", response.results.first().animalId)
        assertEquals(999L, response.results.first().serverSyncedAt)
    }

    @Test
    fun `syncCalves returns failure when token is null and makes no request`() = runTest {
        var requestCount = 0

        val mockEngine = MockEngine { _ ->
            requestCount++
            respond(
                content = """{"success":false,"message":"Error","data":null}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val apiClient = CalfRegistrationApiClient(
            tokenProvider = FakeTokenProvider(null),
            baseUrl = "http://test-host/",
            httpClient = httpClient
        )

        val result = apiClient.syncCalves(listOf(buildCalf()), deviceId = "TEST-DEVICE")

        assertTrue(result.isFailure)
        assertEquals(0, requestCount)
    }

    @Test
    fun `syncCalves returns failure when server reports 401 Unauthorized without retrying`() = runTest {
        var syncAttempts = 0

        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/api/calf-registrations/sync") -> {
                    syncAttempts++

                    respond(
                        content = """{"success":false,"message":"Invalid token","data":null}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                else -> error("Unhandled request: ${request.url}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val apiClient = CalfRegistrationApiClient(
            tokenProvider = FakeTokenProvider("test-token"),
            baseUrl = "http://test-host/",
            httpClient = httpClient
        )

        val result = apiClient.syncCalves(listOf(buildCalf()), deviceId = "TEST-DEVICE")

        assertTrue(result.isFailure)
        assertEquals(1, syncAttempts)
    }
}
