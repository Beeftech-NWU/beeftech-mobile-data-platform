package com.beeftech.authentication.domain

data class LoggedInUser(
    val userId: String,
    val username: String,
    val role: Int?,
    val deviceId: String,
    val siteId: String? = null
) {

    val roleEnum: Role?
        get() = Role.fromId(role)
}
