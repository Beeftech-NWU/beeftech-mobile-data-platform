package com.beeftech.feedcrib.ui

data class SessionItem(
    val name: String,
    val details: String,
    val isComplete: Boolean
)

data class CribReading(
    val morning: String,
    val midDay: String,
    val evening: String,
    val timestamp: Long = System.currentTimeMillis()
)
