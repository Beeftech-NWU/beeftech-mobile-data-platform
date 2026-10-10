package com.beeftech.backend.api.feedcrib

import kotlinx.serialization.Serializable

/** The three blocks of a feeding day. The app files a reading under one by the clock. */
enum class FeedSlot { MORNING, MIDDAY, EVENING }

@Serializable
data class FeedCribEntrySyncRequest(
    val deviceId: String,
    val records: List<FeedCribEntrySyncRecord>,
    /* [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]; absent from app versions that predate it. */
    val batchName: String? = null
)

/* Submitter and site come from the token, never the body. */
@Serializable
data class FeedCribEntrySyncRecord(
    val recordguid: String,
    val cribNumber: String,
    /** The device's local date, yyyy-MM-dd. */
    val readingDate: String,
    /** MORNING, MIDDAY or EVENING. */
    val slot: String,
    val code: Int? = null,
    val adi: Double,
    val capturedAt: Long,
    val deviceId: String = "",
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

@Serializable
data class FeedCribDto(
    val cribNumber: String,
    val siteId: String,
    val penDescription: String,
    val ration: String,
    val method: String,
    val description: String,
    val requiredKg: Double? = null,
    val animalsBegin: Int,
    val animalsIn: Int,
    val animalsOut: Int,
    val animalsClose: Int,
    val currentAdi: Double? = null,
    val active: Boolean,
    val updatedAt: Long
)

@Serializable
data class CribReadingCodeDto(
    val code: Int,
    val label: String,
    val description: String,
    val active: Boolean
)

@Serializable
data class FeedCribEntryDto(
    val recordguid: String,
    val cribNumber: String,
    val readingDate: String,
    val slot: String,
    val code: Int? = null,
    val adi: Double,
    val capturedAt: Long,
    val deviceId: String,
    val submittedByUserId: String? = null,
    val syncedAt: Long? = null
)

/** What a phone downloads: the site's cribs, the code table and the recent entries from every phone on the site. */
@Serializable
data class FeedCribsResponse(
    val siteId: String,
    val cribs: List<FeedCribDto>,
    val codes: List<CribReadingCodeDto>,
    val entries: List<FeedCribEntryDto>,
    val serverTime: Long
)
