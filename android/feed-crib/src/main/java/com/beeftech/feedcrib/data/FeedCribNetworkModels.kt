package com.beeftech.feedcrib.data

import kotlinx.serialization.Serializable

/*
 * Copies of the backend DTOs (backend:api, feedcrib/FeedCribModels.kt). Keep the field names in step.
 */

@Serializable
data class FeedCribApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class FeedCribEntrySyncRequest(
    val deviceId: String,
    val records: List<FeedCribEntryDto>,
    /* [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]; null until this device has the farm code. */
    val batchName: String? = null
)

/** One reading as the server takes it. Submitter and site come from the token, not from here. */
@Serializable
data class FeedCribEntryDto(
    val recordguid: String,
    val cribNumber: String,
    /** The device's local date, yyyy-MM-dd. */
    val readingDate: String,
    /** MORNING, MIDDAY or EVENING. */
    val slot: String,
    val code: Int? = null,
    val adi: Double,
    val capturedAt: Long,
    val deviceId: String,
    val gpsLat: Double? = null,
    val gpsLng: Double? = null
)

@Serializable
data class FeedCribEntrySyncResponse(
    val results: List<FeedCribEntrySyncResult>
)

@Serializable
data class FeedCribEntrySyncResult(
    val recordguid: String,
    val cribNumber: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

/** What a phone downloads: the site's cribs, the code table and recent entries from every phone on the site. */
@Serializable
data class FeedCribsResponse(
    val siteId: String,
    val cribs: List<FeedCribNetworkDto>,
    val codes: List<CribReadingCodeNetworkDto>,
    val entries: List<FeedCribEntryDownloadDto>,
    val serverTime: Long = 0
)

@Serializable
data class FeedCribNetworkDto(
    val cribNumber: String,
    val siteId: String,
    val penDescription: String = "",
    val ration: String = "",
    val method: String = "",
    val description: String = "",
    val requiredKg: Double? = null,
    val animalsBegin: Int = 0,
    val animalsIn: Int = 0,
    val animalsOut: Int = 0,
    val animalsClose: Int = 0,
    val currentAdi: Double? = null,
    val active: Boolean = true,
    val updatedAt: Long = 0
)

@Serializable
data class CribReadingCodeNetworkDto(
    val code: Int,
    val label: String,
    val description: String = "",
    val active: Boolean = true
)

@Serializable
data class FeedCribEntryDownloadDto(
    val recordguid: String,
    val cribNumber: String,
    val readingDate: String,
    val slot: String,
    val code: Int? = null,
    val adi: Double,
    val capturedAt: Long,
    val deviceId: String = "",
    val submittedByUserId: String? = null,
    val syncedAt: Long? = null
)
