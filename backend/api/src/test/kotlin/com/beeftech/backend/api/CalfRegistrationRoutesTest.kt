package com.beeftech.backend.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CalfRegistrationRoutesTest {

    private fun uniqueTestDbUrl(): String {

        val tempFile = Files.createTempFile("beeftech-backend-test", ".db")
        tempFile.toFile().deleteOnExit()

        return "jdbc:sqlite:${tempFile}"
    }

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    @Test
    fun `listing calf registrations without token returns unauthorized`() = testApplication {

        System.setProperty("beeftech.db.url", uniqueTestDbUrl())

        application { module() }

        val client = createClient { }

        val response = client.get("/api/calf-registrations")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `sync then list then get returns the persisted record`() = testApplication {

        System.setProperty("beeftech.db.url", uniqueTestDbUrl())

        application { module() }

        val client = createClient { }

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"TEST_DEV_01"}""")
        }

        val loginBody = Json.parseToJsonElement(loginResponse.bodyAsText())
        val token = loginBody.jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content

        val recordGuid = "test-guid-${System.nanoTime()}"
        val animalId = "test-animal-${System.nanoTime()}"

        val syncResponse = client.post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "deviceId": "device-test",
                  "records": [
                    {
                      "animalId": "$animalId",
                      "birthdate": 1700000000000,
                      "breed": "Angus",
                      "gpsLat": -26.1,
                      "gpsLng": 27.9,
                      "captureAt": 1700000100000,
                      "deviceId": "device-test",
                      "recordguid": "$recordGuid"
                    }
                  ]
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, syncResponse.status)

        val listResponse = client.get("/api/calf-registrations") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.bodyAsText().contains(animalId))

        val getResponse = client.get("/api/calf-registrations/$animalId") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains(recordGuid))
    }

    @Test
    fun `media attachment update and birth certificate PDF endpoint`() = testApplication {

        System.setProperty("beeftech.db.url", uniqueTestDbUrl())

        application { module() }

        val client = createClient { }

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"TEST_DEV_02"}""")
        }

        val loginBody = Json.parseToJsonElement(loginResponse.bodyAsText())
        val token = loginBody.jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content

        val recordGuid = "guid-cert-${System.nanoTime()}"
        val animalId = "Blu0000064"

        client.post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "deviceId": "device-test",
                  "records": [
                    {
                      "animalId": "$animalId",
                      "birthdate": 1700000000000,
                      "breed": "BRN — Brangus",
                      "damId": "Blu0000011",
                      "sireId": "Blu0000902",
                      "gpsLat": -26.1,
                      "gpsLng": 27.9,
                      "captureAt": 1700000100000,
                      "deviceId": "device-test",
                      "recordguid": "$recordGuid"
                    }
                  ]
                }
                """.trimIndent()
            )
        }

        val mediaResponse = client.post("/api/calf-registrations/$animalId/media") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, mediaResponse.status)
        assertTrue(mediaResponse.bodyAsText().contains("/media/photos/calf_$animalId.jpg"))

        val certResponse = client.get("/api/calf-registrations/$animalId/certificate") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, certResponse.status)
        assertEquals("application/pdf", certResponse.headers["Content-Type"])
    }
}
