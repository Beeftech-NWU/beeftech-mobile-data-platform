package com.beeftech.backend.api

import kotlin.test.Test

class FarmerSalesNotificationLiveTest {

    @Test
    fun `send live farmer registration email when enabled`() {

        if (
            System.getenv("BEEFTECH_SMTP_LIVE_TEST")
                ?.equals("true", ignoreCase = true)
                != true
        ) {
            return
        }

        val config =
            SmtpFarmerSalesNotificationConfig
                .fromEnvironment()
                ?: error(
                    "SMTP environment variables are incomplete."
                )

        val service =
            SmtpFarmerSalesNotificationService(
                config
            )

        service.notifyRegistration(
            FarmerSalesNotificationPayload(
                farmerId = "LIVE-EMAIL-TEST-001",
                clientCode = "EMAIL-TEST",
                organisationName =
                    "BeefTech SMTP Test Farm",
                emailAddress =
                    "farmer@example.com",
                vatNumber =
                    "TEST-VAT",
                gpsLatitude =
                    -26.2041,
                gpsLongitude =
                    28.0473,
                addresses =
                    emptyList(),
                roles =
                    emptyList(),
                deviceId =
                    "SMTP-TEST-DEVICE",
                submittedByUserId =
                    "SMTP-TEST-USER",
                submittedByUsername =
                    "smtp-test",
                submittedByRole =
                    3,
                registrationStatus =
                    "REGISTERED",
                serverSyncedAt =
                    System.currentTimeMillis()
            )
        )
    }
}