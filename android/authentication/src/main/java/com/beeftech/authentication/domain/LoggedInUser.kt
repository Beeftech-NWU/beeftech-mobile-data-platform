package com.beeftech.authentication.domain

data class LoggedInUser(
    val userId: String,
    val username: String,
    val role: Int?,
    val deviceId: String
)
