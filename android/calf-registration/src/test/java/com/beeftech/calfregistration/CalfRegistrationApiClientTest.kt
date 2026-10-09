package com.beeftech.calfregistration

import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.fakes.FakeTokenProvider
import com.beeftech.database.dao.CalfRegistrationView
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.beeftech.calfregistration.data.PhotoUploadResult
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent

class CalfRegistrationApiClientTest {

    private fun buildCalf(tagNumber: String = "Blu1234567") = CalfRegistrationView(
        registrationId = "reg-1",
        animalId = "animal-uuid-1",
        tagNumber = tagNumber,
        breed = "Brangus",
        gender = "Female",
        hideColour = null,
        brandMark = null,
        birthdate = 1_700_000_000_000L,
        damAnimalId = null,
        damTagNumber = null,
        sireAnimalId = null,
        sireTagNumber = null,
        birthWeightKg = null,
        calvingEase = null,
        ageClass = "< 1 Week",
        bodyCondition = "Excellent",
        conformity = "G — Good",
        processProof = null,
        implantProof = null,
        oldTagNumber = null,
        referenceNumber = null,
        registrationDate = 1_672_531_200_000L,
        gpsLat = 0.0,
        gpsLng = 0.0,
        deviceId = "TEST-DEVICE",
        captureAt = 1_700_000_100_000L,
        photoPath = null,
        recordGuid = "guid-1",
        syncStatus = "PENDING",
        syncedAt = null,
        syncError = null
    )

    @Test
    fun `syncCalves sends bearer token from TokenProvider to sync endpoint`() = runTest {
        var capturedAuthHeader: String? = null
        var capturedBody: String? = null
        var requestCount = 0

        val mockEngine = MockEngine { request ->
            requestCount++
            when {
                request.url.encodedPath.endsWith("/api/calf-registrations/sync") -> {
                    capturedAuthHeader = request.headers[HttpHeaders.Authorization]
                    capturedBody = (request.body as TextContent).text

                    respond(
                        content = """
                            {"success":true,"message":"Sync complete","data":{"results":[
                                {"recordguid":"guid-1","tagNumber":"Blu1234567","status":"SYNCED","serverSyncedAt":999,"message":null}
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
        assertTrue(capturedBody!!.contains("\"tagNumber\":\"Blu1234567\""))
        assertTrue(capturedBody!!.contains("\"animalUuid\":\"animal-uuid-1\""))
        assertTrue(capturedBody!!.contains("\"breed\":\"Brangus\""))

        val response = result.getOrThrow()
        assertEquals(1, response.results.size)
        assertEquals("SYNCED", response.results.first().status)
        assertEquals("Blu1234567", response.results.first().tagNumber)
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

    private fun photoClient(status: HttpStatusCode, onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {}) =
        CalfRegistrationApiClient(
            tokenProvider = FakeTokenProvider("test-token"),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(
                MockEngine { request ->
                    onRequest(request)
                    respond(content = "{}", status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
            ) { install(ContentNegotiation) { json() } }
        )

    @Test
    fun `uploadPhoto PUTs the JPEG bytes with the bearer token to the calf's photo route`() = runTest {
        var method: HttpMethod? = null
        var path: String? = null
        var auth: String? = null
        var contentType: String? = null
        var body: ByteArray? = null

        val result = photoClient(HttpStatusCode.OK) { request ->
            method = request.method
            path = request.url.encodedPath
            auth = request.headers[HttpHeaders.Authorization]
            contentType = request.body.contentType?.toString()
            body = (request.body as OutgoingContent.ByteArrayContent).bytes()
        }.uploadPhoto("Blu1234567", byteArrayOf(1, 2, 3))

        assertEquals(PhotoUploadResult.Uploaded, result)
        assertEquals(HttpMethod.Put, method)
        assertEquals("/api/calf-registrations/Blu1234567/photo", path)
        assertEquals("Bearer test-token", auth)
        assertEquals("image/jpeg", contentType)
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(body))
    }

    @Test
    fun `uploadPhoto treats a refusal as final and trouble as temporary`() = runTest {
        assertTrue(photoClient(HttpStatusCode.PayloadTooLarge).uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.Rejected)
        assertTrue(photoClient(HttpStatusCode.UnsupportedMediaType).uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.Rejected)
        assertTrue(photoClient(HttpStatusCode.NotFound).uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.Rejected)

        assertTrue(photoClient(HttpStatusCode.InternalServerError).uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.RetryLater)
        assertTrue(photoClient(HttpStatusCode.TooManyRequests).uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.RetryLater)
        assertTrue(photoClient(HttpStatusCode.RequestTimeout).uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.RetryLater)
    }

    @Test
    fun `uploadPhoto retries later when there is no token or the network throws`() = runTest {
        val noToken = CalfRegistrationApiClient(
            tokenProvider = FakeTokenProvider(null),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(MockEngine { error("must not be called") })
        )
        assertTrue(noToken.uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.RetryLater)

        val offline = CalfRegistrationApiClient(
            tokenProvider = FakeTokenProvider("t"),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(MockEngine { throw java.io.IOException("offline") })
        )
        assertTrue(offline.uploadPhoto("T", byteArrayOf(1)) is PhotoUploadResult.RetryLater)
    }
}
