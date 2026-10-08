package com.beeftech.backend.api

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The batch name sent with a sync upload is checked, and every upload leaves a row in sync_upload_log. */
class SyncUploadLogRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-upload-log-test", ".db")
        file.toFile().deleteOnExit()
        System.setProperty("beeftech.db.url", "jdbc:sqlite:$file")
        application { module() }
    }

    private suspend fun HttpClient.login(): String {
        val text = post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"jvdm","pin":"30003","device_id":"dev-jvdm"}""")
        }.bodyAsText()
        return Json.parseToJsonElement(text).jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content
    }

    private fun costRecord(guid: String) =
        """{"animalId":"A-1","costType":"FEED","amount":10.0,"timestamp":100,"recordguid":"$guid"}"""

    private suspend fun HttpClient.syncCosts(token: String, batchName: String?, vararg guids: String): HttpResponse =
        post("/api/costs/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            val name = batchName?.let { ""","batchName":"$it"""" }.orEmpty()
            setBody("""{"deviceId":"phone","records":[${guids.joinToString(",") { costRecord(it) }}]$name}""")
        }

    private fun logRows() = transaction(DatabaseFactory.getDatabase()) { SyncUploadLogTable.selectAll().toList() }

    @Test
    fun `a valid batch name is accepted and logged with its counts`() = testApplication {
        startApp()
        val client = createClient { }
        val token = client.login()

        val response = client.syncCosts(token, "S001-COST-20261008-140509-dev_jvdm", "g-1", "g-2")

        assertEquals(HttpStatusCode.OK, response.status)
        val row = logRows().single()
        assertEquals("S001-COST-20261008-140509-dev_jvdm", row[SyncUploadLogTable.batchName])
        assertEquals("COST", row[SyncUploadLogTable.project])
        assertEquals("dev-site-1", row[SyncUploadLogTable.siteId])
        assertEquals(2, row[SyncUploadLogTable.recordCount])
        assertEquals(2, row[SyncUploadLogTable.accepted])
        assertEquals(0, row[SyncUploadLogTable.rejected])
    }

    @Test
    fun `a malformed batch name is a 400 and nothing is saved or logged`() = testApplication {
        startApp()
        val client = createClient { }
        val token = client.login()

        val response = client.syncCosts(token, "not-a-batch-name", "g-1")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("agreed format"))
        assertTrue(logRows().isEmpty())
        assertTrue(transaction(DatabaseFactory.getDatabase()) { CostTable.selectAll().empty() })
    }

    @Test
    fun `a batch name for another farm, project or device is a 400`() = testApplication {
        startApp()
        val client = createClient { }
        val token = client.login()

        val otherFarm = client.syncCosts(token, "ZZZZ-COST-20261008-140509-dev_jvdm", "g-1")
        val otherProject = client.syncCosts(token, "S001-MORTALITY-20261008-140509-dev_jvdm", "g-1")
        val otherDevice = client.syncCosts(token, "S001-COST-20261008-140509-someone_else", "g-1")

        assertEquals(HttpStatusCode.BadRequest, otherFarm.status)
        assertTrue(otherFarm.bodyAsText().contains("farm code"))
        assertEquals(HttpStatusCode.BadRequest, otherProject.status)
        assertEquals(HttpStatusCode.BadRequest, otherDevice.status)
        assertTrue(otherDevice.bodyAsText().contains("device"))
        assertTrue(logRows().isEmpty())
    }

    @Test
    fun `a retried batch name keeps one row and updates it`() = testApplication {
        startApp()
        val client = createClient { }
        val token = client.login()
        val name = "S001-COST-20261008-140509-dev_jvdm"

        client.syncCosts(token, name, "g-1")
        client.syncCosts(token, name, "g-1", "g-2")

        val row = logRows().single()
        assertEquals(2, row[SyncUploadLogTable.recordCount])
        assertEquals(2, row[SyncUploadLogTable.accepted])
    }

    @Test
    fun `an upload without a batch name is still accepted and logged as legacy`() = testApplication {
        startApp()
        val client = createClient { }
        val token = client.login()

        val response = client.syncCosts(token, null, "g-1")

        assertEquals(HttpStatusCode.OK, response.status)
        val row = logRows().single()
        assertNull(row[SyncUploadLogTable.batchName])
        assertEquals("COST", row[SyncUploadLogTable.project])
        assertEquals(1, row[SyncUploadLogTable.accepted])
    }
}
