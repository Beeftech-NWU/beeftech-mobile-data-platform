package com.beeftech.backend.api

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * R3 acceptance: "a backend test shows that re-sending a batch creates no
 * duplicates." TreatmentRepository.upsertByRecordGuid already upserts by
 * `recordguid`; this proves it, mirroring
 * CalfRegistrationRoutesTest's equivalent test.
 */
class TreatmentRoutesTest {

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
    fun `syncing the same recordguid twice keeps a single row`() = testApplication {

        System.setProperty("beeftech.db.url", uniqueTestDbUrl())

        application { module() }

        val client = createClient { }

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","pin":"10001","device_id":"TEST_DEV_TREATMENT"}""")
        }
        val token = Json.parseToJsonElement(loginResponse.bodyAsText())
            .jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content

        val body = """
            {
              "deviceId": "device-test",
              "records": [
                {
                  "animalId": "11111111-1111-1111-1111-111111111111",
                  "disease": "Foot Rot",
                  "treatmentName": "Antibiotic",
                  "batchNumber": "B1",
                  "volumeUsed": "10ml",
                  "cost": 50.0,
                  "timestamp": 1700000000000,
                  "deviceId": "device-test",
                  "recordguid": "idempotent-treatment-guid"
                }
              ]
            }
        """.trimIndent()

        repeat(2) {
            val response = client.post("/api/treatments/sync") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"status\":\"SYNCED\""))
        }

        val rowCount = withContext(Dispatchers.IO) {
            newSuspendedTransaction {
                TreatmentTable
                    .selectAll()
                    .where { TreatmentTable.recordguid eq "idempotent-treatment-guid" }
                    .count()
            }
        }
        assertEquals(1, rowCount)
    }
}
