package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.database.entity.CalfRegistration
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

    private fun buildCalf(animalId: String = "RMB12345") = CalfRegistration(
        id = 1,
        animalId = animalId,
        birthdate = 111L,
        breed = "Bonsmara",
        gpsLat = 0.0,
        gpsLng = 0.0,
        captureAt = 222L,
        deviceId = "TEST-DEVICE",
        recordguid = "guid-1",
        syncStatus = "PENDING",
        syncedat = null
    )

    @Test
    fun `syncCalves logs in then sends bearer token to sync endpoint`() = runTest {
        var capturedAuthHeader: String? = null

        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/api/auth/login") -> {
                    respond(
                        content = """
                            {"success":true,"message":"Login successful","data":{"token":"test-token"}}
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

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
            baseUrl = "http://test-host/",
            httpClient = httpClient
        )

        val result = apiClient.syncCalves(listOf(buildCalf()), deviceId = "TEST-DEVICE")

        assertTrue(result.isSuccess)
        assertEquals("Bearer test-token", capturedAuthHeader)

        val response = result.getOrThrow()
        assertEquals(1, response.results.size)
        assertEquals("SYNCED", response.results.first().status)
        assertEquals("RMB12345", response.results.first().animalId)
        assertEquals(999L, response.results.first().serverSyncedAt)
    }

    @Test
    fun `syncCalves returns failure when login fails`() = runTest {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/api/auth/login") -> {
                    respond(
                        content = """
                            {"success":false,"message":"Invalid credentials","data":null}
                        """.trimIndent(),
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
            baseUrl = "http://test-host/",
            httpClient = httpClient
        )

        val result = apiClient.syncCalves(listOf(buildCalf()), deviceId = "TEST-DEVICE")

        assertTrue(result.isFailure)
    }
}
