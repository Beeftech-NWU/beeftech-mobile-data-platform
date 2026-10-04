package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/*
 * New tables, so SchemaUtils.create makes them on existing databases; no ALTER migration needed.
 * Diseases and treatment types keep their existing tables (TreatmentReferenceTable.kt).
 */

/*
 * The cost types the app offers. Mirrors android CostTypeSeed; the code is the key the app's
 * animal_costs rows point at, so it can be added and deactivated but never renamed or deleted.
 */
object CostTypeTable : Table("cost_types") {
    val code = varchar("code", 64)
    val displayName = varchar("display_name", 100)
    val sortOrder = integer("sort_order").default(0)
    val active = bool("active").default(true)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(code)
}

/* Small key/value settings. Holds the reference-data version now, and the sync policy later. */
object AppSettingsTable : Table("app_settings") {
    val key = varchar("setting_key", 100)
    val value = text("setting_value")
    val updatedAt = long("updated_at")
    val updatedByUserId = varchar("updated_by_user_id", 64).nullable()

    override val primaryKey = PrimaryKey(key)
}

object AppSettingKeys {
    /* Bumped whenever a reference value is added or its active flag changes. */
    const val REFERENCE_DATA_VERSION = "reference_data_version"

    /* The days of unsynced data that raise the three warnings, e.g. "2,4,6". */
    const val SYNC_WARNING_DAYS = "sync.warning_days"

    /* Hours without contact after which the dashboard flags a worker. */
    const val SYNC_STALE_ALERT_HOURS = "sync.stale_alert_hours"

    /* Bumped whenever the sync policy changes. */
    const val SYNC_POLICY_VERSION = "sync_policy_version"
}
