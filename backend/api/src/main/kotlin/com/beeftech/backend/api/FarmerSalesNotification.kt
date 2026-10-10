package com.beeftech.backend.api

import com.beeftech.backend.api.common.FileNaming
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
    val event: String = NEW_FARMER_REGISTRATION_EVENT,
    val farmerId: String,
    val clientCode: String? = null,
    val organisationName: String? = null,
    val emailAddress: String? = null,
    val vatNumber: String? = null,
    val coRegIdNo: String? = null,
    val landOwnership: String? = null,
    val faCodeRmis: String? = null,
    val glnNumber: String? = null,
    val herdCapacity: Int? = null,
    val interestStatus: String? = null,
    val contactName: String? = null,
    val contactNumber: String? = null,
    val farmSizeHa: Double? = null,
    val headCount: Int? = null,
    val primaryBreed: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val addresses: List<FarmerAddressDto> = emptyList(),
    val roles: List<FarmerRoleDto> = emptyList(),
    val deviceId: String,
    /* The submitter's site farm code; names the attachment. Null for a user with no site. */
    val farmCode: String? = null,
    /* The submitter's site sales rep, who the email goes to. Null when the site has none. */
    val assignedSalesmanEmail: String? = null,
    val submittedByUserId: String,
    val submittedByUsername: String,
    val submittedByRole: Int? = null,
    val registrationStatus: String = "REGISTERED",
    val serverSyncedAt: Long
)

/** Calf receipt is separate from the later ownership/transfer events. */
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
    val serverSyncedAt: Long,
    val assignedSalesmanEmail: String? = null
)

const val NEW_FARMER_REGISTRATION_EVENT = "NEW_FARMER_REGISTRATION"

interface FarmerSalesNotificationService {

    /**
     * True only when an email actually went out. False (nothing sent, e.g. no recipient or no
     * SMTP) lets the farmer be notified on a later sync. A failed send throws.
     */
    fun notifyRegistration(
        payload: FarmerSalesNotificationPayload
    ): Boolean

    /** A default no-op keeps existing farmer-only implementations compatible. */
    fun notifyCalfRegistration(payload: CalfRegistrationNotificationPayload) = Unit
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
    /* BEEFTECH_SALES_REP_EMAIL: used only when the farmer's site has no sales rep. */
    val recipientAddress: String?,
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

            /* Optional: sites carry their own sales rep, and this is only the fallback. */
            val recipientAddress =
                environment["BEEFTECH_SALES_REP_EMAIL"]
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }

            if (
                fromAddress.isBlank()
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
    private val config:
        SmtpFarmerSalesNotificationConfig
) : FarmerSalesNotificationService {

    private val json =
        Json {
            prettyPrint = true
            explicitNulls = false
            encodeDefaults = true
        }

    override fun notifyRegistration(
        payload: FarmerSalesNotificationPayload
    ): Boolean {

        val recipientAddress =
            salesRecipientFor(
                payload,
                config.recipientAddress
            )

        if (recipientAddress == null) {

            println(
                "No sales rep email for farmer " +
                    payload.farmerId +
                    ": the site has none and BEEFTECH_SALES_REP_EMAIL is not set. Email skipped."
            )

            return false
        }

        val summaryJson =
            json.encodeToString(payload)

        val properties =
            Properties().apply {

                put(
                    "mail.smtp.host",
                    config.host
                )

                put(
                    "mail.smtp.port",
                    config.port.toString()
                )

                put(
                    "mail.smtp.auth",
                    (
                        config.username != null &&
                            config.password != null
                    ).toString()
                )

                when (config.security) {

                    SmtpSecurity.STARTTLS -> {
                        put(
                            "mail.smtp.starttls.enable",
                            "true"
                        )
                        put(
                            "mail.smtp.starttls.required",
                            "true"
                        )
                        put(
                            "mail.smtp.ssl.enable",
                            "false"
                        )
                    }

                    SmtpSecurity.SSL -> {
                        put(
                            "mail.smtp.starttls.enable",
                            "false"
                        )
                        put(
                            "mail.smtp.ssl.enable",
                            "true"
                        )
                    }

                    SmtpSecurity.NONE -> {
                        put(
                            "mail.smtp.starttls.enable",
                            "false"
                        )
                        put(
                            "mail.smtp.ssl.enable",
                            "false"
                        )
                    }
                }
            }

        val authenticator =
            if (
                config.username != null &&
                config.password != null
            ) {

                object : Authenticator() {

                    override fun getPasswordAuthentication():
                        PasswordAuthentication {

                        return PasswordAuthentication(
                            config.username,
                            config.password
                        )
                    }
                }

            } else {
                null
            }

        val session =
            Session.getInstance(
                properties,
                authenticator
            )

        val message =
            MimeMessage(session).apply {

                setFrom(
                    InternetAddress(
                        config.fromAddress
                    )
                )

                setRecipient(
                    Message.RecipientType.TO,
                    InternetAddress(
                        recipientAddress
                    )
                )

                subject =
                    "BeefTech Farmer Registration - " +
                        (
                            payload.organisationName
                                ?: payload.farmerId
                        )

                val emailBody =
                    MimeBodyPart().apply {

                        setText(
                            buildString {

                                appendLine(
                                    "A farmer registration has been successfully synchronized with BeefTech."
                                )

                                appendLine()

                                appendLine(
                                    "Registration status: ${payload.registrationStatus}"
                                )

                                appendLine(
                                    "Farmer ID: ${payload.farmerId}"
                                )

                                appendLine(
                                    "Client code: ${payload.clientCode ?: "N/A"}"
                                )

                                appendLine(
                                    "Organisation: ${payload.organisationName ?: "N/A"}"
                                )

                                appendLine(
                                    "Co-Reg / ID No.: ${payload.coRegIdNo ?: "N/A"}"
                                )

                                appendLine(
                                    "Land ownership: ${payload.landOwnership ?: "N/A"}"
                                )

                                appendLine(
                                    "FA code (RMIS): ${payload.faCodeRmis ?: "N/A"}"
                                )

                                appendLine(
                                    "GLN number: ${payload.glnNumber ?: "N/A"}"
                                )

                                appendLine(
                                    "Herd capacity: ${payload.herdCapacity ?: "N/A"}"
                                )

                                appendLine(
                                    "Interest status: ${payload.interestStatus ?: "N/A"}"
                                )

                                appendLine(
                                    "Contact person: ${payload.contactName ?: "N/A"}"
                                )

                                appendLine(
                                    "Contact number: ${payload.contactNumber ?: "N/A"}"
                                )

                                appendLine(
                                    "Farm size (ha): ${payload.farmSizeHa ?: "N/A"}"
                                )

                                appendLine(
                                    "Head count: ${payload.headCount ?: "N/A"}"
                                )

                                appendLine(
                                    "Primary breed: ${payload.primaryBreed ?: "N/A"}"
                                )

                                appendLine(
                                    "Country: ${
                                        (
                                            payload.addresses
                                                .firstOrNull { it.addressType == "PRIMARY" }
                                                ?: payload.addresses.firstOrNull()
                                        )?.country ?: "N/A"
                                    }"
                                )

                                appendLine(
                                    "Submitted by: ${payload.submittedByUsername}"
                                )

                                appendLine()

                                appendLine(
                                    "The complete JSON registration summary is attached."
                                )
                            },
                            StandardCharsets.UTF_8.name()
                        )
                    }

                val jsonAttachment =
                    MimeBodyPart().apply {

                        val dataSource =
                            ByteArrayDataSource(
                                summaryJson.toByteArray(
                                    StandardCharsets.UTF_8
                                ),
                                "application/json; charset=UTF-8"
                            )

                        dataHandler =
                            DataHandler(
                                dataSource
                            )

                        fileName =
                            attachmentFileName(payload)
                    }

                val multipart =
                    MimeMultipart().apply {

                        addBodyPart(
                            emailBody
                        )

                        addBodyPart(
                            jsonAttachment
                        )
                    }

                setContent(
                    multipart
                )
            }

        Transport.send(
            message
        )

        println(
            "Farmer registration notification sent using " +
                config.provider +
                " to " +
                recipientAddress +
                " for farmer " +
                payload.farmerId
        )

        return true
    }

    override fun notifyCalfRegistration(payload: CalfRegistrationNotificationPayload) {
        val recipient = payload.assignedSalesmanEmail?.trim()?.takeIf { it.isNotEmpty() }
            ?: config.recipientAddress?.trim()?.takeIf { it.isNotEmpty() }
        if (recipient == null) {
            println("Calf registration email skipped: no site sales rep or fallback recipient")
            return
        }

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
        val authenticator = if (config.username != null && config.password != null) {
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(config.username, config.password)
            }
        } else null
        val message = MimeMessage(Session.getInstance(properties, authenticator)).apply {
            setFrom(InternetAddress(config.fromAddress))
            setRecipient(Message.RecipientType.TO, InternetAddress(recipient))
            setSubject("BeefTech Calf Registration - ${payload.tagNumber}", StandardCharsets.UTF_8.name())
            val body = MimeBodyPart().apply {
                setText(
                    "Calf registration confirmed by BeefTech server.\n" +
                        "Animal tag: ${payload.tagNumber}\n" +
                        "Registration: ${payload.recordGuid}\n" +
                        "Farmer assignment is a separate record.\n" +
                        "Full JSON receipt attached.",
                    StandardCharsets.UTF_8.name()
                )
            }
            val attachment = MimeBodyPart().apply {
                dataHandler = DataHandler(ByteArrayDataSource(
                    json.encodeToString(payload).toByteArray(StandardCharsets.UTF_8),
                    "application/json; charset=UTF-8"
                ))
                fileName = "calf-registration-${safeFileName(payload.recordGuid)}.json"
            }
            setContent(MimeMultipart().apply {
                addBodyPart(body)
                addBodyPart(attachment)
            })
        }
        Transport.send(message)
        println("Calf registration JSON receipt sent for ${payload.recordGuid}")
    }

    private fun safeFileName(
        value: String
    ): String {

        return value.replace(
            Regex("[^A-Za-z0-9._-]"),
            "_"
        )
    }
}

/** The site's sales rep when it has one, otherwise the BEEFTECH_SALES_REP_EMAIL fallback, otherwise null. */
fun salesRecipientFor(
    payload: FarmerSalesNotificationPayload,
    fallbackAddress: String?
): String? =
    payload.assignedSalesmanEmail
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: fallbackAddress
            ?.trim()
            ?.takeIf { it.isNotBlank() }

class LoggingFarmerSalesNotificationService :
    FarmerSalesNotificationService {

    private val json =
        Json {
            prettyPrint = true
            explicitNulls = false
            encodeDefaults = true
        }

    override fun notifyRegistration(
        payload: FarmerSalesNotificationPayload
    ): Boolean {

        println("Farmer registration receipt not emailed: SMTP is not configured")

        /* Logged, not emailed: leave the farmer unnotified so a configured server can still email. */
        return false
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

/**
 * [FarmCode]-FARMER_REG-[YYYYMMDD]-[HHMMSS]-[DeviceID].json, using the submitter's site code, the
 * device that took the record and the time the server received it (UTC). Falls back to the older
 * farmer-registration-<id>.json when there is no farm code.
 */
internal fun attachmentFileName(payload: FarmerSalesNotificationPayload): String {
    val named = runCatching {
        FileNaming.build(
            farmCode = payload.farmCode.orEmpty(),
            project = FileNaming.ProjectCode.FARMER_REG,
            instant = java.time.Instant.ofEpochMilli(payload.serverSyncedAt),
            deviceId = payload.deviceId,
            extension = "json",
            zone = java.time.ZoneOffset.UTC
        )
    }.getOrNull()

    return named ?: "farmer-registration-${payload.farmerId.replace(Regex("[^A-Za-z0-9._-]"), "_")}.json"
}
