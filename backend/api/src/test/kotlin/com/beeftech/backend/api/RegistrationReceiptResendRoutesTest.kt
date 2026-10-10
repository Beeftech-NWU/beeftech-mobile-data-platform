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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RegistrationReceiptResendRoutesTest {

    @BeforeTest
    fun enableDevUsers() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    @Test
    fun `resend requires an admin, a reason, and an existing synced record`() = testApplication {
        val databaseFile = Files.createTempFile("beeftech-receipt-resend", ".db")
        databaseFile.toFile().deleteOnExit()
        System.setProperty("beeftech.db.url", "jdbc:sqlite:$databaseFile")
        application { module() }

        val endpoint = "/api/admin/registration-receipts/calf/Blu0002201/resend"
        val body = """{"reason":"Resend for email delivery testing"}"""
        val noToken = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Unauthorized, noToken.status)

        suspend fun login(username: String, pin: String): String {
            val response = client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"$username","pin":"$pin","device_id":"RESEND_TEST_$username"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            return Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content
        }
        val worker = login("jvdm", "30003")
        val notAdmin = client.post(endpoint) {
            header("Authorization", "Bearer $worker")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Forbidden, notAdmin.status)

        val admin = login("admin", "10001")
        val badReason = client.post(endpoint) {
            header("Authorization", "Bearer $admin")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"short"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, badReason.status)

        val noRecord = client.post(endpoint) {
            header("Authorization", "Bearer $admin")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.NotFound, noRecord.status)
    }

    @Test
    fun `manual receipt payload can be identified as a resend without changing original event`() {
        val farmer = FarmerSalesNotificationPayload(
            farmerId = "F100",
            deviceId = "SERVER_RESEND",
            submittedByUserId = "U1",
            submittedByUsername = "worker",
            serverSyncedAt = 1000L,
            resentAt = 2000L
        )
        val calf = CalfRegistrationNotificationPayload(
            recordGuid = "GUID-1",
            tagNumber = "Blu0001",
            breed = "Brangus",
            birthdate = 100L,
            captureAt = 200L,
            deviceId = "DEVICE-1",
            serverSyncedAt = 1000L,
            resentAt = 2000L
        )
        assertEquals(1000L, farmer.serverSyncedAt)
        assertEquals(2000L, farmer.resentAt)
        assertEquals("NEW_FARMER_REGISTRATION", farmer.event)
        assertEquals("CALF_REGISTERED", calf.eventType)
        assertEquals(2000L, calf.resentAt)
    }
}
