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
import kotlinx.serialization.json.jsonArray
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
        val tagNumber = "Blu${System.nanoTime() % 10_000_000L}".padEnd(10, '0')
        val animalUuid = java.util.UUID.randomUUID().toString()

        val syncResponse = client.post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "deviceId": "device-test",
                  "records": [
                    {
                      "tagNumber": "$tagNumber",
                      "animalUuid": "$animalUuid",
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
        assertTrue(listResponse.bodyAsText().contains(tagNumber))

        val getResponse = client.get("/api/calf-registrations/$tagNumber") {
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
        val tagNumber = "Blu0000064"
        val animalUuid = java.util.UUID.randomUUID().toString()

        client.post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "deviceId": "device-test",
                  "records": [
                    {
                      "tagNumber": "$tagNumber",
                      "animalUuid": "$animalUuid",
                      "birthdate": 1700000000000,
                      "breed": "BRN — Brangus",
                      "damTagNumber": "Blu0000011",
                      "sireTagNumber": "Blu0000902",
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

        val mediaResponse = client.post("/api/calf-registrations/$tagNumber/media") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, mediaResponse.status)
        assertTrue(mediaResponse.bodyAsText().contains("/media/photos/calf_$tagNumber.jpg"))

        val certResponse = client.get("/api/calf-registrations/$tagNumber/certificate") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, certResponse.status)
        assertEquals("application/pdf", certResponse.headers["Content-Type"])
    }

    @Test
    fun `syncing the same recordguid twice keeps a single row`() = testApplication {

        System.setProperty("beeftech.db.url", uniqueTestDbUrl())

        application { module() }

        val client = createClient { }

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"TEST_DEV_03"}""")
        }
        val token = Json.parseToJsonElement(loginResponse.bodyAsText())
            .jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content

        val body = """
            {
              "deviceId": "device-test",
              "records": [
                {
                  "tagNumber": "Blu1234567",
                  "animalUuid": "11111111-1111-1111-1111-111111111111",
                  "birthdate": 1700000000000,
                  "breed": "Brangus",
                  "gpsLat": 0.0,
                  "gpsLng": 0.0,
                  "captureAt": 1700000100000,
                  "deviceId": "device-test",
                  "recordguid": "idempotent-guid"
                }
              ]
            }
        """.trimIndent()

        repeat(2) {
            val response = client.post("/api/calf-registrations/sync") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"status\":\"SYNCED\""))
        }

        val list = client.get("/api/calf-registrations") {
            header("Authorization", "Bearer $token")
        }
        val records = Json.parseToJsonElement(list.bodyAsText())
            .jsonObject["data"]!!.jsonArray
        assertEquals(1, records.size)
    }
}
