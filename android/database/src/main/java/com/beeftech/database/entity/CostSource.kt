package com.beeftech.database.entity

/**
 * Allowed values for [AnimalCost.sourceEntity].
 *
 * A cost derived from another record carries the source table
 * here and the source row's record GUID in
 * [AnimalCost.sourceRecordId]. GUIDs are used rather than primary
 * keys because they are stable across devices and sync.
 *
 * Manually captured costs leave both fields null.
 */
object CostSource {
    const val TREATMENT = "TREATMENT"
    const val LOCATION_FEED = "LOCATION_FEED"
}
