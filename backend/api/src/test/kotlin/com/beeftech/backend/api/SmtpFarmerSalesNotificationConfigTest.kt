package com.beeftech.backend.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SmtpFarmerSalesNotificationConfigTest {

    @Test
    fun `gmail provider uses gmail smtp defaults`() {

        val config =
            SmtpFarmerSalesNotificationConfig
                .fromEnvironment(
                    mapOf(
                        "BEEFTECH_SMTP_PROVIDER" to
                            "gmail",
                        "BEEFTECH_SMTP_USERNAME" to
                            "beeftech@gmail.com",
                        "BEEFTECH_SMTP_PASSWORD" to
                            "test-password",
                        "BEEFTECH_SALES_REP_EMAIL" to
                            "sales@example.com"
                    )
                )

        assertNotNull(config)

        assertEquals(
            SmtpProvider.GMAIL,
            config.provider
        )

        assertEquals(
            "smtp.gmail.com",
            config.host
        )

        assertEquals(
            587,
            config.port
        )

        assertEquals(
            SmtpSecurity.STARTTLS,
            config.security
        )

        assertEquals(
            "beeftech@gmail.com",
            config.fromAddress
        )
    }

    @Test
    fun `outlook provider uses microsoft smtp defaults`() {

        val config =
            SmtpFarmerSalesNotificationConfig
                .fromEnvironment(
                    mapOf(
                        "BEEFTECH_SMTP_PROVIDER" to
                            "outlook",
                        "BEEFTECH_SMTP_USERNAME" to
                            "beeftech@example.com",
                        "BEEFTECH_SMTP_PASSWORD" to
                            "test-password",
                        "BEEFTECH_SALES_REP_EMAIL" to
                            "sales@example.com"
                    )
                )

        assertNotNull(config)

        assertEquals(
            SmtpProvider.OUTLOOK,
            config.provider
        )

        assertEquals(
            "smtp.office365.com",
            config.host
        )

        assertEquals(
            587,
            config.port
        )

        assertEquals(
            SmtpSecurity.STARTTLS,
            config.security
        )
    }

    @Test
    fun `custom provider supports ssl smtp configuration`() {

        val config =
            SmtpFarmerSalesNotificationConfig
                .fromEnvironment(
                    mapOf(
                        "BEEFTECH_SMTP_PROVIDER" to
                            "custom",
                        "BEEFTECH_SMTP_HOST" to
                            "smtp.example.com",
                        "BEEFTECH_SMTP_PORT" to
                            "465",
                        "BEEFTECH_SMTP_SECURITY" to
                            "ssl",
                        "BEEFTECH_SMTP_FROM" to
                            "beeftech@example.com",
                        "BEEFTECH_SALES_REP_EMAIL" to
                            "sales@example.com"
                    )
                )

        assertNotNull(config)

        assertEquals(
            SmtpProvider.CUSTOM,
            config.provider
        )

        assertEquals(
            "smtp.example.com",
            config.host
        )

        assertEquals(
            465,
            config.port
        )

        assertEquals(
            SmtpSecurity.SSL,
            config.security
        )
    }

    @Test
    fun `smtp stays enabled without BEEFTECH_SALES_REP_EMAIL because sites carry their own rep`() {

        val config =
            SmtpFarmerSalesNotificationConfig
                .fromEnvironment(
                    mapOf(
                        "BEEFTECH_SMTP_HOST" to
                            "localhost",
                        "BEEFTECH_SMTP_PORT" to
                            "1025",
                        "BEEFTECH_SMTP_SECURITY" to
                            "none",
                        "BEEFTECH_SMTP_FROM" to
                            "noreply@example.com"
                    )
                )

        assertNotNull(config)

        assertNull(
            config.recipientAddress
        )
    }

    @Test
    fun `the site's sales rep wins over the fallback address`() {

        val payload =
            FarmerSalesNotificationPayload(
                farmerId = "f-1",
                deviceId = "MOB_DEV_1",
                assignedSalesmanEmail = "site.rep@example.com",
                submittedByUserId = "u-1",
                submittedByUsername = "jvdm",
                serverSyncedAt = 0L
            )

        assertEquals(
            "site.rep@example.com",
            salesRecipientFor(payload, "fallback@example.com")
        )

        assertEquals(
            "fallback@example.com",
            salesRecipientFor(payload.copy(assignedSalesmanEmail = null), "fallback@example.com")
        )

        assertEquals(
            "fallback@example.com",
            salesRecipientFor(payload.copy(assignedSalesmanEmail = "  "), " fallback@example.com ")
        )

        assertNull(
            salesRecipientFor(payload.copy(assignedSalesmanEmail = null), null)
        )
    }
}
