package com.beeftech.backend.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CalfFarmerAssignmentNotificationTest {
    @Test
    fun `assignment JSON contains final calf and farmer details`() {
        val payload = CalfRegistrationNotificationPayload(
            eventType = "CALF_ASSIGNED_TO_FARMER",
            recordGuid = "REG-100",
            animalUuid = "ANIMAL-100",
            tagNumber = "Blu0002201",
            breed = "Bonsmara",
            birthdate = 1690000000000L,
            captureAt = 1690100000000L,
            deviceId = "DEVICE-1",
            siteId = "dev-site-1",
            siteName = "Dev Feedlot",
            submittedByUserId = "WORKER-1",
            serverSyncedAt = 1690200000000L,
            damTagNumber = "DAM-1",
            sireTagNumber = "SIRE-1",
            gpsLatitude = -26.1,
            gpsLongitude = 28.1,
            calfDetails = CalfRegistrationDto(
                tagNumber = "Blu0002201", animalUuid = "ANIMAL-100", birthdate = 1690000000000L,
                breed = "Bonsmara", gender = "FEMALE", birthWeightKg = 32.5,
                bodyCondition = "GOOD", damTagNumber = "DAM-1", sireTagNumber = "SIRE-1",
                gpsLat = -26.1, gpsLng = 28.1, captureAt = 1690100000000L,
                deviceId = "DEVICE-1", recordguid = "REG-100", syncStatus = "SYNCED"
            ),
            assignment = CalfFarmerAssignmentDetails(
                assignmentRecordGuid = "ASSIGN-1",
                farmerId = "FARMER-1",
                farmerClientCode = "FARM001",
                farmerOrganisationName = "Test Ranch",
                effectiveFrom = 1690300000000L,
                assignedAtServer = 1690400000000L
            )
        )
        val json = Json { encodeDefaults = true }.encodeToString(payload)
        assertContains(json, "\"eventType\":\"CALF_ASSIGNED_TO_FARMER\"")
        assertContains(json, "\"farmerId\":\"FARMER-1\"")
        assertContains(json, "\"farmerOrganisationName\":\"Test Ranch\"")
        assertContains(json, "\"damTagNumber\":\"DAM-1\"")
        assertContains(json, "\"siteName\":\"Dev Feedlot\"")
        assertContains(json, "\"assignmentRecordGuid\":\"ASSIGN-1\"")
        assertContains(json, "\"birthWeightKg\":32.5")
        assertContains(json, "\"bodyCondition\":\"GOOD\"")
    }

    @Test
    fun `registration does not falsely invent a farmer assignment`() {
        val payload = CalfRegistrationNotificationPayload(
            recordGuid = "REG-101", tagNumber = "Blu0002202", breed = "Brangus",
            birthdate = 1L, captureAt = 2L, deviceId = "DEVICE-1", serverSyncedAt = 3L
        )
        val json = Json { explicitNulls = false; encodeDefaults = true }.encodeToString(payload)
        assertContains(json, "\"eventType\":\"CALF_REGISTERED\"")
        assertFalse(json.contains("\"assignment\""))
    }
}
