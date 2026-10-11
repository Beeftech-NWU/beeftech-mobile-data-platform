package com.beeftech.backend.api.feedcrib

import kotlinx.serialization.Serializable

@Serializable
data class FeedCribRequest(
    val penName: String,
    val adiValue: Double,
    val morning: String,
    val midDay: String,
    val evening: String,
    val timestamp: Long
)

@Serializable
data class FeedCribResponse(
    val id: Long,
    val penName: String,
    val adiValue: Double,
    val morning: String,
    val midDay: String,
    val evening: String,
    val timestamp: Long
)