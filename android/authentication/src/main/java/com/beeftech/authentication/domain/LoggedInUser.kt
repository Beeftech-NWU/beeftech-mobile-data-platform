package com.beeftech.authentication.domain

data class LoggedInUser(
    val userId: String,
    val username: String,
    val role: Int?,
    val deviceId: String,
    val siteId: String? = null,
    /* The site's four-character farm code; null until a first online sign-in has fetched it. */
    val farmCode: String? = null
) {

    val roleEnum: Role?
        get() = Role.fromId(role)
}
