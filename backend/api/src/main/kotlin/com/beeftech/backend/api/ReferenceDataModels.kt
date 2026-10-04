package com.beeftech.backend.api

import kotlinx.serialization.Serializable

/* The three kinds of reference value, with the path segment used by the API. */
enum class ReferenceKind(val slug: String, val entityType: String) {
    DISEASES("diseases", "DISEASE"),
    TREATMENT_TYPES("treatment-types", "TREATMENT_TYPE"),
    COST_TYPES("cost-types", "COST_TYPE");

    companion object {
        fun fromSlug(slug: String): ReferenceKind? = entries.firstOrNull { it.slug == slug }
    }
}

@Serializable
data class ReferenceItemDto(
    val id: Int,
    val name: String,
    val active: Boolean
)

@Serializable
data class CostTypeDto(
    val code: String,
    val displayName: String,
    val sortOrder: Int,
    val active: Boolean
)

/*
 * Everything the app needs, inactive values included (so a device can hide them from pickers
 * without deleting anything it already stored). When the caller's [ifVersion] is current,
 * [unchanged] is true and the lists are left out.
 */
@Serializable
data class ReferenceDataSnapshot(
    val version: Long,
    val unchanged: Boolean = false,
    val diseases: List<ReferenceItemDto>? = null,
    val treatmentTypes: List<ReferenceItemDto>? = null,
    val costTypes: List<CostTypeDto>? = null
)

/* name for diseases and treatment types; code, displayName and sortOrder for cost types. */
@Serializable
data class CreateReferenceItemRequest(
    val name: String? = null,
    val code: String? = null,
    val displayName: String? = null,
    val sortOrder: Int? = null
)

@Serializable
data class SetReferenceActiveRequest(
    val active: Boolean
)

/* One value as a flat entry, the same shape for every kind. [id] is the cost type code for cost types. */
@Serializable
data class ReferenceEntryDto(
    val kind: String,
    val id: String,
    val name: String,
    val active: Boolean,
    val sortOrder: Int? = null
)

@Serializable
data class ReferenceChangeResponse(
    /* The version after the change (unchanged when nothing changed). */
    val version: Long,
    val item: ReferenceEntryDto
)
