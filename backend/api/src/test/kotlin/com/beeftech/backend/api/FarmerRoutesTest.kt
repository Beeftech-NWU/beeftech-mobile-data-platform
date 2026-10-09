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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FarmerRoutesTest {

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
    fun `sync then get returns the persisted registration fields`() = testApplication {

        System.setProperty("beeftech.db.url", uniqueTestDbUrl())

        application { module() }

        val client = createClient { }

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"TEST_DEV_FARMER"}""")
        }

        val token = Json.parseToJsonElement(loginResponse.bodyAsText())
            .jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content

        val syncResponse = client.post("/api/farmers/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "deviceId": "device-test",
                  "records": [
                    {
                      "farmerId": "farmer-1",
                      "clientCode": "KAR001",
                      "organisationName": "Karoo Vryburg",
                      "coRegIdNo": "9608551/07",
                      "landOwnership": "Owned",
                      "faCodeRmis": "FA-RMIS-01",
                      "glnNumber": "GLN-123",
                      "herdCapacity": 450,
                      "interestStatus": "Interested",
                      "contactName": "Jan Botha",
                      "contactNumber": "+27 82 555 0101",
                      "farmSizeHa": 1250.5,
                      "headCount": 380,
                      "primaryBreed": "Bonsmara",
                      "addresses": [
                        {
                          "addressId": "addr-1",
                          "farmerId": "farmer-1",
                          "addressType": "PRIMARY",
                          "addressLine1": "Mark Str 93",
                          "postalCode": "8600",
                          "streetCode": "8600",
                          "postalAddress": "POSBUS 117",
                          "country": "South Africa"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, syncResponse.status)

        val firstGet = client.get("/api/farmers/farmer-1") {
            header("Authorization", "Bearer $token")
        }
        val firstFarmer = Json.parseToJsonElement(firstGet.bodyAsText())
            .jsonObject["data"]!!.jsonObject
        assertEquals(450, firstFarmer["herdCapacity"]!!.jsonPrimitive.intOrNull)
        assertEquals("Interested", firstFarmer["interestStatus"]!!.jsonPrimitive.content)
        assertEquals("Jan Botha", firstFarmer["contactName"]!!.jsonPrimitive.content)
        assertEquals("+27 82 555 0101", firstFarmer["contactNumber"]!!.jsonPrimitive.content)
        assertEquals(1250.5, firstFarmer["farmSizeHa"]!!.jsonPrimitive.doubleOrNull)
        assertEquals(380, firstFarmer["headCount"]!!.jsonPrimitive.intOrNull)
        assertEquals("Bonsmara", firstFarmer["primaryBreed"]!!.jsonPrimitive.content)

        /* A retry must update in place and still carry the fields. */
        val retryResponse = client.post("/api/farmers/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "deviceId": "device-test",
                  "records": [
                    {
                      "farmerId": "farmer-1",
                      "clientCode": "KAR001",
                      "coRegIdNo": "9608551/07",
                      "landOwnership": "Leased",
                      "addresses": [
                        {
                          "addressId": "addr-1",
                          "farmerId": "farmer-1",
                          "addressType": "PRIMARY",
                          "country": "Namibia"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, retryResponse.status)

        val getResponse = client.get("/api/farmers/farmer-1") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)

        val farmer = Json.parseToJsonElement(getResponse.bodyAsText())
            .jsonObject["data"]!!.jsonObject

        assertEquals("9608551/07", farmer["coRegIdNo"]!!.jsonPrimitive.content)
        assertEquals("Leased", farmer["landOwnership"]!!.jsonPrimitive.content)
        assertEquals(null, farmer["herdCapacity"]?.jsonPrimitive?.intOrNull)
        assertEquals(null, farmer["interestStatus"]?.jsonPrimitive?.contentOrNull)

        val address = farmer["addresses"]!!.jsonArray[0].jsonObject
        assertEquals("Namibia", address["country"]!!.jsonPrimitive.content)
    }
}
