package com.beeftech.backend.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class SiteDto(
    val siteId: String,
    val name: String,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long? = null,
    val farmCode: String? = null,
    /* Active users of any role on this site. */
    val activeUserCount: Long
)

@Serializable
data class CreateSiteRequest(
    val name: String,
    val farmCode: String? = null
)

/* A null field means "leave unchanged". */
@Serializable
data class UpdateSiteRequest(
    val name: String? = null,
    val active: Boolean? = null,
    val farmCode: String? = null
)
