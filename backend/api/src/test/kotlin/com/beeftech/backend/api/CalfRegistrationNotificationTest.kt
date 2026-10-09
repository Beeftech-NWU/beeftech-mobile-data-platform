package com.beeftech.backend.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CalfRegistrationNotificationTest {
    @Test
    fun `new calf JSON receipt contains original identifiers and farm scope`() {
        val payload = CalfRegistrationNotificationPayload(
            recordGuid = "REG-EXAMPLE-001",
            animalUuid = "00000000-0000-0000-0000-000000000123",
            tagNumber = "Blu0000001",
            breed = "Simmentaler",
            birthdate = 1690000000000L,
            captureAt = 1690100000000L,
            deviceId = "TEST-DEVICE",
            siteId = "SITE-TEST",
            submittedByUserId = "USER-TEST",
            serverSyncedAt = 1690200000000L
        )
        val json = Json { encodeDefaults = true }.encodeToString(payload)
        assertContains(json, "\"eventType\":\"CALF_REGISTERED\"")
        assertContains(json, "\"schemaVersion\":1")
        assertContains(json, "\"recordGuid\":\"REG-EXAMPLE-001\"")
        assertContains(json, "\"animalUuid\":\"00000000-0000-0000-0000-000000000123\"")
        assertContains(json, "\"siteId\":\"SITE-TEST\"")
        assertFalse(json.contains("\"farmerId\""), "A calf has no owner until separately assigned")
    }
}
