package com.beeftech.backend.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains

class FarmerSalesNotificationTest {

    private fun payload(farmCode: String?, deviceId: String = "MOB_DEV_a1b2c3d4") =
        FarmerSalesNotificationPayload(
            farmerId = "FARMER/1",
            farmCode = farmCode,
            deviceId = deviceId,
            submittedByUserId = "USER-1",
            submittedByUsername = "jvdm",
            serverSyncedAt = 1_791_468_309_000L
        )

    @Test
    fun `the attachment is named from the submitter's farm code, the device and the server time`() {
        kotlin.test.assertEquals(
            "BF01-FARMER_REG-20261008-140509-MOB_DEV_a1b2c3d4.json",
            attachmentFileName(payload("BF01"))
        )
    }

    @Test
    fun `the attachment keeps the older name when there is no farm code`() {
        kotlin.test.assertEquals("farmer-registration-FARMER_1.json", attachmentFileName(payload(null)))
        kotlin.test.assertEquals("farmer-registration-FARMER_1.json", attachmentFileName(payload("BF01", deviceId = "---")))
    }

    @Test
    fun `farmer sales notification serializes required registration data`() {

        val payload =
            FarmerSalesNotificationPayload(
                farmerId = "FARMER-TEST-1",
                clientCode = "CLIENT-001",
                organisationName = "Test Farm",
                emailAddress = "farmer@example.com",
                vatNumber = "VAT-001",
                herdCapacity = 450,
                interestStatus = "Follow-up needed",
                contactName = "Jan Botha",
                contactNumber = "+27 82 555 0101",
                farmSizeHa = 1250.5,
                headCount = 380,
                primaryBreed = "Bonsmara",
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
                assignedSalesmanEmail = "rep@example.com",
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
        assertContains(json, "\"submittedByRole\":3")
        assertContains(json, "\"registrationStatus\":\"REGISTERED\"")
        assertContains(json, "\"serverSyncedAt\":123456789")
        assertContains(json, "\"emailAddress\":\"farmer@example.com\"")
        assertContains(json, "\"vatNumber\":\"VAT-001\"")
        assertContains(json, "\"gpsLatitude\":-26.2041")
        assertContains(json, "\"gpsLongitude\":28.0473")
        assertContains(json, "\"herdCapacity\":450")
        assertContains(json, "\"interestStatus\":\"Follow-up needed\"")
        assertContains(json, "\"event\":\"NEW_FARMER_REGISTRATION\"")
        assertContains(json, "\"assignedSalesmanEmail\":\"rep@example.com\"")
        assertContains(json, "\"contactName\":\"Jan Botha\"")
        assertContains(json, "\"contactNumber\":\"+27 82 555 0101\"")
        assertContains(json, "\"farmSizeHa\":1250.5")
        assertContains(json, "\"headCount\":380")
        assertContains(json, "\"primaryBreed\":\"Bonsmara\"")
        assertContains(json, "\"addresses\"")
        assertContains(json, "\"roles\"")
    }
}