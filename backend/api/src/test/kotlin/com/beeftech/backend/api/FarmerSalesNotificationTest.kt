package com.beeftech.backend.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains

class FarmerSalesNotificationTest {

    @Test
    fun `farmer sales notification serializes required registration data`() {

        val payload =
            FarmerSalesNotificationPayload(
                farmerId = "FARMER-TEST-1",
                clientCode = "CLIENT-001",
                organisationName = "Test Farm",
                emailAddress = "farmer@example.com",
                vatNumber = "VAT-001",
                gpsLatitude = -26.2041,
                gpsLongitude = 28.0473,
                addresses =
                    listOf(
                        FarmerAddressDto(
                            addressId = "ADDRESS-1",
                            farmerId = "FARMER-TEST-1",
                            addressType = "PRIMARY",
                            addressLine1 = "1 Test Road",
                            province = "Gauteng",
                            postalCode = "1459",
                            gpsLatitude = -26.2041,
                            gpsLongitude = 28.0473
                        )
                    ),
                roles =
                    listOf(
                        FarmerRoleDto(
                            farmerRoleId = "ROLE-1",
                            farmerId = "FARMER-TEST-1",
                            roleId = "6"
                        )
                    ),
                deviceId = "TEST-DEVICE",
                submittedByUserId = "USER-1",
                submittedByUsername = "jvdm",
                submittedByRole = 3,
                registrationStatus = "REGISTERED",
                serverSyncedAt = 123456789L
            )

        val json =
            Json {
                explicitNulls = false
                encodeDefaults = true
            }.encodeToString(payload)

        assertContains(json, "\"farmerId\":\"FARMER-TEST-1\"")
        assertContains(json, "\"clientCode\":\"CLIENT-001\"")
        assertContains(json, "\"organisationName\":\"Test Farm\"")
        assertContains(json, "\"registrationStatus\":\"REGISTERED\"")
        assertContains(json, "\"deviceId\":\"TEST-DEVICE\"")
        assertContains(json, "\"submittedByUserId\":\"USER-1\"")
        assertContains(json, "\"submittedByUsername\":\"jvdm\"")
        assertContains(json, "\"addresses\"")
        assertContains(json, "\"roles\"")
    }
}