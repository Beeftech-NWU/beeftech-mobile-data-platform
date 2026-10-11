package com.beeftech.feedcrib.data

data class FeedCribCaptureContext(
    val deviceId: String,
    val captureAt: Long = System.currentTimeMillis(),
    // A recent permitted location fix, or null when unavailable.
    val gpsLat: Double? = null,
    val gpsLng: Double? = null
)
