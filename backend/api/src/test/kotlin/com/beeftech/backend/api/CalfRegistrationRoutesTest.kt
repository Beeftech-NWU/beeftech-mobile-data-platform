package com.beeftech.backend.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.readRawBytes
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

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3, 4, 5)

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
        System.setProperty("beeftech.media.dir", Files.createTempDirectory("beeftech-media-test").toFile().also { it.deleteOnExit() }.absolutePath)
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
                      "gender": "Male",
                      "hideColour": "BLACK",
                      "brandMark": "K7",
                      "birthWeightKg": 34.5,
                      "ageClass": "< 1 Week",
                      "bodyCondition": "Excellent",
                      "conformity": "G — Good",
                      "oldTagNumber": "OLD-1",
                      "referenceNumber": "REF-9",
                      "processProof": "P-77",
                      "implantProof": "I-12",
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

        val saved = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject["data"]!!.jsonObject
        assertEquals("Male", saved["gender"]!!.jsonPrimitive.content)
        assertEquals("BLACK", saved["hideColour"]!!.jsonPrimitive.content)
        assertEquals("K7", saved["brandMark"]!!.jsonPrimitive.content)
        assertEquals(34.5, saved["birthWeightKg"]!!.jsonPrimitive.content.toDouble())
        assertEquals("< 1 Week", saved["ageClass"]!!.jsonPrimitive.content)
        assertEquals("Excellent", saved["bodyCondition"]!!.jsonPrimitive.content)
        assertEquals("G — Good", saved["conformity"]!!.jsonPrimitive.content)
        assertEquals("OLD-1", saved["oldTagNumber"]!!.jsonPrimitive.content)
        assertEquals("REF-9", saved["referenceNumber"]!!.jsonPrimitive.content)
        assertEquals("P-77", saved["processProof"]!!.jsonPrimitive.content)
        assertEquals("I-12", saved["implantProof"]!!.jsonPrimitive.content)
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

        val photoResponse = client.put("/api/calf-registrations/$tagNumber/photo") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Image.JPEG)
            setBody(jpeg)
        }

        assertEquals(HttpStatusCode.OK, photoResponse.status)
        assertTrue(photoResponse.bodyAsText().contains("/api/calf-registrations/$tagNumber/photo"))

        val certResponse = client.get("/api/calf-registrations/$tagNumber/certificate") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, certResponse.status)
        assertEquals("application/pdf", certResponse.headers["Content-Type"])
    }

    private suspend fun io.ktor.client.HttpClient.adminToken(device: String): String {
        val login = post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"$device"}""")
        }
        return Json.parseToJsonElement(login.bodyAsText()).jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content
    }

    private suspend fun io.ktor.client.HttpClient.syncOne(token: String, tag: String, guid: String, photoPath: String? = null) =
        post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            val photo = photoPath?.let { """"photoPath":"$it",""" } ?: ""
            setBody(
                """{"deviceId":"d","records":[{"tagNumber":"$tag","birthdate":1700000000000,"breed":"Angus",$photo
                "gpsLat":0.0,"gpsLng":0.0,"captureAt":1700000100000,"deviceId":"d","recordguid":"$guid"}]}"""
            )
        }

    @Test
    fun `an uploaded photo is stored, returned byte for byte and recorded on the calf`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }
        val client = createClient { }
        val token = client.adminToken("TEST_DEV_10")
        client.syncOne(token, "Blu0000064", "photo-guid-1")

        val put = client.put("/api/calf-registrations/Blu0000064/photo") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Image.JPEG)
            setBody(jpeg)
        }
        assertEquals(HttpStatusCode.OK, put.status)

        val get = client.get("/api/calf-registrations/Blu0000064/photo") { header("Authorization", "Bearer $token") }
        assertEquals(HttpStatusCode.OK, get.status)
        assertEquals("image/jpeg", get.headers["Content-Type"])
        assertTrue(jpeg.contentEquals(get.readRawBytes()))

        val record = client.get("/api/calf-registrations/Blu0000064") { header("Authorization", "Bearer $token") }.bodyAsText()
        assertTrue("/api/calf-registrations/Blu0000064/photo" in record)
    }

    @Test
    fun `photo upload rejects non JPEG data, oversized data and unknown calves`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }
        val client = createClient { }
        val token = client.adminToken("TEST_DEV_11")
        client.syncOne(token, "Blu0000064", "photo-guid-2")

        suspend fun put(tag: String, body: ByteArray) =
            client.put("/api/calf-registrations/$tag/photo") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Image.JPEG)
                setBody(body)
            }.status

        assertEquals(HttpStatusCode.UnsupportedMediaType, put("Blu0000064", "not an image".toByteArray()))
        assertEquals(
            HttpStatusCode.PayloadTooLarge,
            put("Blu0000064", jpeg + ByteArray(CalfPhotoStore.MAX_BYTES))
        )
        assertEquals(HttpStatusCode.NotFound, put("Nope000001", jpeg))
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/calf-registrations/Blu0000064/photo") { header("Authorization", "Bearer $token") }.status
        )
    }

    @Test
    fun `photo upload needs a token`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }
        val client = createClient { }

        val response = client.put("/api/calf-registrations/Blu0000064/photo") {
            contentType(ContentType.Image.JPEG)
            setBody(jpeg)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `a later sync does not overwrite the server photo path with the phone's local path`() = testApplication {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application { module() }
        val client = createClient { }
        val token = client.adminToken("TEST_DEV_12")

        client.syncOne(token, "Blu0000064", "photo-guid-3", photoPath = "/storage/emulated/0/phone-only.jpg")
        val first = client.get("/api/calf-registrations/Blu0000064") { header("Authorization", "Bearer $token") }.bodyAsText()
        assertTrue("phone-only.jpg" !in first)

        client.put("/api/calf-registrations/Blu0000064/photo") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Image.JPEG)
            setBody(jpeg)
        }
        client.syncOne(token, "Blu0000064", "photo-guid-3", photoPath = "/storage/emulated/0/phone-only.jpg")

        val after = client.get("/api/calf-registrations/Blu0000064") { header("Authorization", "Bearer $token") }.bodyAsText()
        assertTrue("/api/calf-registrations/Blu0000064/photo" in after)
        assertTrue("phone-only.jpg" !in after)
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
