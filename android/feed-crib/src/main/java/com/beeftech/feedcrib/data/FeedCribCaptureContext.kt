package com.beeftech.feedcrib.data

data class FeedCribCaptureContext(
    val deviceId: String,
    val captureAt: Long = System.currentTimeMillis(),
    // TODO(D1-gap): feed crib has no GPS capture yet.
    // Values are stored on the entry row and synced from there, so adding
    // capture later needs no sync or schema change.
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0
)
