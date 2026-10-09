package com.beeftech.backend.api

import jakarta.activation.DataHandler
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.Properties

@Serializable
data class FarmerSalesNotificationPayload(
    val farmerId: String,
    val clientCode: String? = null,
    val organisationName: String? = null,
    val emailAddress: String? = null,
    val vatNumber: String? = null,
    val coRegIdNo: String? = null,
    val landOwnership: String? = null,
    val faCodeRmis: String? = null,
    val glnNumber: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val addresses: List<FarmerAddressDto> = emptyList(),
    val roles: List<FarmerRoleDto> = emptyList(),
    val deviceId: String,
    val submittedByUserId: String,
    val submittedByUsername: String,
    val submittedByRole: Int? = null,
    val registrationStatus: String = "REGISTERED",
    val serverSyncedAt: Long,
    val siteId: String? = null
)

/** Receipt for a calf first accepted by the backend. Ownership is a separate later event. */
@Serializable
data class CalfRegistrationNotificationPayload(
    val schemaVersion: Int = 1,
    val eventType: String = "CALF_REGISTERED",
    val recordGuid: String,
    val animalUuid: String? = null,
    val tagNumber: String,
    val breed: String,
    val birthdate: Long,
    val captureAt: Long,
    val deviceId: String,
    val siteId: String? = null,
    val submittedByUserId: String? = null,
    val serverSyncedAt: Long
)

interface FarmerSalesNotificationService {

    fun notifyRegistration(payload: FarmerSalesNotificationPayload)

    /** Called only for newly persisted calves, never for an upload replay. */
    fun notifyCalfRegistration(payload: CalfRegistrationNotificationPayload)
}

enum class SmtpProvider {
    GMAIL,
    OUTLOOK,
    CUSTOM
}

enum class SmtpSecurity {
    STARTTLS,
    SSL,
    NONE
}

data class SmtpFarmerSalesNotificationConfig(
    val provider: SmtpProvider,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val fromAddress: String,
    val recipientAddress: String,
    val security: SmtpSecurity
) {

    companion object {

        fun fromEnvironment(
            environment: Map<String, String> =
                System.getenv()
        ): SmtpFarmerSalesNotificationConfig? {

            val provider =
                when (
                    environment["BEEFTECH_SMTP_PROVIDER"]
                        ?.trim()
                        ?.lowercase()
                        ?.ifBlank { "custom" }
                        ?: "custom"
                ) {
                    "gmail" ->
                        SmtpProvider.GMAIL

                    "outlook",
                    "microsoft",
                    "microsoft365",
                    "office365" ->
                        SmtpProvider.OUTLOOK

                    "custom" ->
                        SmtpProvider.CUSTOM

                    else ->
                        throw IllegalArgumentException(
                            "Unsupported BEEFTECH_SMTP_PROVIDER. " +
                                "Use gmail, outlook, or custom."
                        )
                }

            val username =
                environment["BEEFTECH_SMTP_USERNAME"]
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }

            val password =
                environment["BEEFTECH_SMTP_PASSWORD"]
                    ?.takeIf {
                        it.isNotBlank()
                    }

            if (
                (username == null) !=
                (password == null)
            ) {
                throw IllegalStateException(
                    "SMTP username and password must either both be configured or both be omitted."
                )
            }

            /*
             * Gmail and Outlook require authenticated SMTP
             * for this BeefTech configuration.
             */
            if (
                provider != SmtpProvider.CUSTOM &&
                (
                    username == null ||
                    password == null
                )
            ) {
                return null
            }

            val fromAddress =
                environment["BEEFTECH_SMTP_FROM"]
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: username
                    ?: ""

            val recipientAddress =
                environment["BEEFTECH_SALES_REP_EMAIL"]
                    ?.trim()
                    .orEmpty()

            if (
                fromAddress.isBlank() ||
                recipientAddress.isBlank()
            ) {
                return null
            }

            val defaultSecurity =
                when (provider) {
                    SmtpProvider.GMAIL ->
                        SmtpSecurity.STARTTLS

                    SmtpProvider.OUTLOOK ->
                        SmtpSecurity.STARTTLS

                    SmtpProvider.CUSTOM ->
                        SmtpSecurity.STARTTLS
                }

            val security =
                environment["BEEFTECH_SMTP_SECURITY"]
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        when (it) {
                            "starttls",
                            "tls" ->
                                SmtpSecurity.STARTTLS

                            "ssl",
                            "smtps" ->
                                SmtpSecurity.SSL

                            "none",
                            "plain" ->
                                SmtpSecurity.NONE

                            else ->
                                throw IllegalArgumentException(
                                    "Unsupported BEEFTECH_SMTP_SECURITY. " +
                                        "Use starttls, ssl, or none."
                                )
                        }
                    }
                    ?: defaultSecurity

            val host =
                when (provider) {
                    SmtpProvider.GMAIL ->
                        "smtp.gmail.com"

                    SmtpProvider.OUTLOOK ->
                        "smtp.office365.com"

                    SmtpProvider.CUSTOM ->
                        environment["BEEFTECH_SMTP_HOST"]
                            ?.trim()
                            .orEmpty()
                }

            if (host.isBlank()) {
                return null
            }

            val defaultPort =
                when (security) {
                    SmtpSecurity.STARTTLS ->
                        587

                    SmtpSecurity.SSL ->
                        465

                    SmtpSecurity.NONE ->
                        25
                }

            val port =
                environment["BEEFTECH_SMTP_PORT"]
                    ?.trim()
                    ?.toIntOrNull()
                    ?: defaultPort

            return SmtpFarmerSalesNotificationConfig(
                provider = provider,
                host = host,
                port = port,
                username = username,
                password = password,
                fromAddress = fromAddress,
                recipientAddress =
                    recipientAddress,
                security = security
            )
        }
    }
}

class SmtpFarmerSalesNotificationService(
    private val config: SmtpFarmerSalesNotificationConfig
) : FarmerSalesNotificationService {
    private val json = Json {
        prettyPrint = true
        explicitNulls = false
        encodeDefaults = true
    }

    override fun notifyRegistration(payload: FarmerSalesNotificationPayload) {
        sendJsonReceipt(
            subject = "BeefTech Farmer Registration - ${payload.organisationName ?: payload.farmerId}",
            body = "Farmer registration confirmed by BeefTech server.\n" +
                "Farmer ID: ${payload.farmerId}\n" +
                "Organisation: ${payload.organisationName ?: "N/A"}\n" +
                "Full JSON receipt attached.",
            fileName = "farmer-registration-${safeFileName(payload.farmerId)}.json",
            content = json.encodeToString(payload)
        )
    }

    override fun notifyCalfRegistration(payload: CalfRegistrationNotificationPayload) {
        sendJsonReceipt(
            subject = "BeefTech Calf Registration - ${payload.tagNumber}",
            body = "Calf registration confirmed by BeefTech server.\n" +
                "Animal tag: ${payload.tagNumber}\n" +
                "Calf record: ${payload.recordGuid}\n" +
                "A future farmer assignment is a separate record.\n" +
                "Full JSON receipt attached.",
            fileName = "calf-registration-${safeFileName(payload.recordGuid)}.json",
            content = json.encodeToString(payload)
        )
    }

    private fun sendJsonReceipt(subject: String, body: String, fileName: String, content: String) {
        val properties = Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", (config.username != null && config.password != null).toString())
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
            put("mail.smtp.writetimeout", "10000")
            when (config.security) {
                SmtpSecurity.STARTTLS -> {
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                    put("mail.smtp.ssl.enable", "false")
                }
                SmtpSecurity.SSL -> {
                    put("mail.smtp.starttls.enable", "false")
                    put("mail.smtp.ssl.enable", "true")
                }
                SmtpSecurity.NONE -> {
                    put("mail.smtp.starttls.enable", "false")
                    put("mail.smtp.ssl.enable", "false")
                }
            }
        }
        // Do not leak SMTP credentials in errors or logs.
        val auth = if (config.username != null && config.password != null) {
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(config.username, config.password)
            }
        } else null
        val message = MimeMessage(Session.getInstance(properties, auth)).apply {
            setFrom(InternetAddress(config.fromAddress))
            setRecipient(Message.RecipientType.TO, InternetAddress(config.recipientAddress))
            setSubject(subject, StandardCharsets.UTF_8.name())
            val plainText = MimeBodyPart().apply { setText(body, StandardCharsets.UTF_8.name()) }
            val attachment = MimeBodyPart().apply {
                dataHandler = DataHandler(ByteArrayDataSource(
                    content.toByteArray(StandardCharsets.UTF_8), "application/json; charset=UTF-8"
                ))
                this.fileName = fileName
            }
            setContent(MimeMultipart().apply {
                addBodyPart(plainText)
                addBodyPart(attachment)
            })
        }
        Transport.send(message)
        println("BeefTech JSON notification delivered via configured SMTP recipient")
    }

    private fun safeFileName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

/** Development fallback: never print personal details or JSON payloads to logs. */
class LoggingFarmerSalesNotificationService : FarmerSalesNotificationService {
    override fun notifyRegistration(payload: FarmerSalesNotificationPayload) {
        println("Farmer registration receipt not emailed: SMTP is not configured")
    }

    override fun notifyCalfRegistration(payload: CalfRegistrationNotificationPayload) {
        println("Calf registration receipt not emailed: SMTP is not configured")
    }
}

fun createFarmerSalesNotificationServiceFromEnvironment():
    FarmerSalesNotificationService {

    val config =
        SmtpFarmerSalesNotificationConfig
            .fromEnvironment()

    return if (config == null) {

        println(
            "SMTP configuration is incomplete. " +
                "Farmer sales notifications will be logged instead of emailed."
        )

        LoggingFarmerSalesNotificationService()

    } else {

        println(
            "Farmer sales email notifications enabled using " +
                config.provider +
                " via " +
                config.host +
                ":" +
                config.port
        )

        SmtpFarmerSalesNotificationService(
            config
        )
    }
}