package com.beeftech.backend.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConciseJsonReceiptTest {
    private fun assertNoEmptyValues(json: String): JsonObject {
        val obj = Json.parseToJsonElement(json).jsonObject
        obj.values.filterIsInstance<JsonPrimitive>().filter { it.isString }.forEach { value ->
            assertTrue(value.content.isNotBlank(), "Receipt contains an empty string")
        }
        return obj
    }

    @Test fun `farmer receipt has only essential nonempty values`() {
        val payload = FarmerSalesNotificationPayload(
            farmerId = "F-101", clientCode = "  ", organisationName = " GauFarm ",
            contactName = "Not included", vatNumber = "Not included",
            deviceId = "DEVICE-1", submittedByUserId = "WORKER-1",
            submittedByUsername = "worker", serverSyncedAt = 100L
        )
        val obj = assertNoEmptyValues(conciseFarmerReceiptJson(payload))
        assertEquals(setOf("event", "farmerId", "organisationName", "deviceId", "registrationStatus", "serverSyncedAt"), obj.keys)
        assertEquals("GauFarm", obj.getValue("organisationName").jsonPrimitive.content)
        assertFalse(obj.containsKey("contactName"))
    }

    @Test fun `required farmer receipt fields cannot be blank`() {
        val payload = FarmerSalesNotificationPayload(
            farmerId = " ", deviceId = "DEVICE-1", submittedByUserId = "WORKER-1",
            submittedByUsername = "worker", serverSyncedAt = 100L
        )
        assertFailsWith<IllegalArgumentException> { conciseFarmerReceiptJson(payload) }
    }

    @Test fun `registration receipt does not contain a large calf snapshot`() {
        val payload = CalfRegistrationNotificationPayload(
            recordGuid = "REG-1", tagNumber = "Blu100", breed = "Bonsmara",
            birthdate = 123L, captureAt = 124L, deviceId = "DEVICE-1", serverSyncedAt = 125L,
            siteName = "  ", siteId = "site-1", damTagNumber = "DAM-1", gpsLatitude = -26.0
        )
        val obj = assertNoEmptyValues(conciseCalfReceiptJson(payload))
        assertEquals(setOf("schemaVersion", "eventType", "recordGuid", "tagNumber", "breed", "birthdate", "siteId", "serverSyncedAt"), obj.keys)
        assertFalse(obj.containsKey("calfDetails"))
        assertFalse(obj.containsKey("assignment"))
    }

    @Test fun `assignment receipt includes required assignment ids but no blank optional fields`() {
        val payload = CalfRegistrationNotificationPayload(
            eventType = "CALF_ASSIGNED_TO_FARMER", recordGuid = "REG-1",
            tagNumber = "Blu100", breed = "Bonsmara", birthdate = 123L,
            captureAt = 124L, deviceId = "DEVICE-1", serverSyncedAt = 125L,
            siteName = "GauFarm", assignment = CalfFarmerAssignmentDetails(
                assignmentRecordGuid = "ASSIGN-1", farmerId = "FARMER-1",
                farmerOrganisationName = " ", effectiveFrom = 126L, assignedAtServer = 127L
            )
        )
        val obj = assertNoEmptyValues(conciseCalfReceiptJson(payload))
        assertEquals(setOf("schemaVersion", "eventType", "assignmentRecordGuid", "recordGuid", "tagNumber", "farmerId", "siteName", "assignedAtServer"), obj.keys)
        assertEquals("FARMER-1", obj.getValue("farmerId").jsonPrimitive.content)
        assertFalse(obj.containsKey("farmerOrganisationName"))
        assertFalse(obj.containsKey("calfDetails"))
    }

    @Test fun `missing assignment is rejected rather than emailed as an empty object`() {
        val payload = CalfRegistrationNotificationPayload(
            eventType = "CALF_ASSIGNED_TO_FARMER", recordGuid = "REG-1",
            tagNumber = "Blu100", breed = "Bonsmara", birthdate = 123L,
            captureAt = 124L, deviceId = "DEVICE-1", serverSyncedAt = 125L
        )
        assertFailsWith<IllegalArgumentException> { conciseCalfReceiptJson(payload) }
    }

    @Test fun `blank calf tag is rejected`() {
        val payload = CalfRegistrationNotificationPayload(
            recordGuid = "REG-1", tagNumber = " ", breed = "Bonsmara",
            birthdate = 123L, captureAt = 124L, deviceId = "DEVICE-1", serverSyncedAt = 125L
        )
        assertFailsWith<IllegalArgumentException> { conciseCalfReceiptJson(payload) }
    }
}
