package com.beeftech.backend.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Small, customer-facing email JSON receipts. The complete sync API payloads and
 * database entities are unchanged; never use these summaries to reconstruct data.
 * Required receipt identifiers are validated. Unknown optional fields are omitted,
 * not populated with empty strings, nulls or fabricated values.
 */
private val conciseReceiptEncoder = Json { prettyPrint = true }

private fun requiredReceiptText(name: String, raw: String): String {
    val value = raw.trim()
    require(value.isNotEmpty()) { "Required JSON receipt field '$name' cannot be blank" }
    return value
}

private fun requiredReceiptTime(name: String, value: Long): Long {
    require(value > 0L) { "Required JSON receipt field '$name' must be positive" }
    return value
}

private fun JsonObjectBuilder.optionalReceiptText(name: String, raw: String?) {
    val value = raw?.trim()?.takeIf(String::isNotBlank) ?: return
    put(name, value)
}

private fun JsonObjectBuilder.receiptSite(payload: CalfRegistrationNotificationPayload) {
    val readableName = payload.siteName?.trim()?.takeIf(String::isNotBlank)
    if (readableName != null) {
        put("siteName", readableName)
    } else {
        optionalReceiptText("siteId", payload.siteId)
    }
}

/** Keep farmer registration receipt small and avoid exposing unused personal data. */
internal fun conciseFarmerReceiptJson(payload: FarmerSalesNotificationPayload): String {
    val receipt = buildJsonObject {
        put("event", requiredReceiptText("event", payload.event))
        put("farmerId", requiredReceiptText("farmerId", payload.farmerId))
        optionalReceiptText("clientCode", payload.clientCode)
        optionalReceiptText("organisationName", payload.organisationName)
        optionalReceiptText("farmCode", payload.farmCode)
        put("deviceId", requiredReceiptText("deviceId", payload.deviceId))
        put("registrationStatus", requiredReceiptText("registrationStatus", payload.registrationStatus))
        put("serverSyncedAt", requiredReceiptTime("serverSyncedAt", payload.serverSyncedAt))
    }
    return conciseReceiptEncoder.encodeToString(receipt)
}

/** Keep calf registration separate from later ownership assignments. */
internal fun conciseCalfReceiptJson(payload: CalfRegistrationNotificationPayload): String {
    require(payload.schemaVersion > 0) { "schemaVersion must be positive" }
    val eventType = requiredReceiptText("eventType", payload.eventType)
    require(eventType == "CALF_REGISTERED" || eventType == "CALF_ASSIGNED_TO_FARMER") {
        "Unsupported JSON receipt eventType: $eventType"
    }
    val receipt = buildJsonObject {
        put("schemaVersion", payload.schemaVersion)
        put("eventType", eventType)
        if (eventType == "CALF_REGISTERED") {
            require(payload.assignment == null) { "Calf registration receipt must not imply an assignment" }
            put("recordGuid", requiredReceiptText("recordGuid", payload.recordGuid))
            put("tagNumber", requiredReceiptText("tagNumber", payload.tagNumber))
            put("breed", requiredReceiptText("breed", payload.breed))
            put("birthdate", requiredReceiptTime("birthdate", payload.birthdate))
            receiptSite(payload)
            put("serverSyncedAt", requiredReceiptTime("serverSyncedAt", payload.serverSyncedAt))
        } else {
            val assignment = requireNotNull(payload.assignment) {
                "CALF_ASSIGNED_TO_FARMER receipt requires assignment details"
            }
            put("assignmentRecordGuid", requiredReceiptText("assignmentRecordGuid", assignment.assignmentRecordGuid))
            put("recordGuid", requiredReceiptText("recordGuid", payload.recordGuid))
            put("tagNumber", requiredReceiptText("tagNumber", payload.tagNumber))
            put("farmerId", requiredReceiptText("farmerId", assignment.farmerId))
            optionalReceiptText("farmerOrganisationName", assignment.farmerOrganisationName)
            receiptSite(payload)
            put("assignedAtServer", requiredReceiptTime("assignedAtServer", assignment.assignedAtServer))
        }
    }
    return conciseReceiptEncoder.encodeToString(receipt)
}
