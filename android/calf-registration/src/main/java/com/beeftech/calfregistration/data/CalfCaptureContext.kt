package com.beeftech.calfregistration.data

data class CalfCaptureContext(
    val deviceId: String,
    val captureAt: Long = System.currentTimeMillis(),
    // TODO(D1-gap): calf registration has no GPS capture UI yet.
    // Values are stored on the animal row and synced from there, so adding
    // capture later needs no sync or schema change.
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0
)
