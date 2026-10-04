package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Small key/value settings the server hands to the device (the reference-data version now, the
 * sync-warning policy later). One row per key, replaced on each pull.
 */
@Entity(tableName = "device_config")
data class DeviceConfigEntry(
    @PrimaryKey
    @ColumnInfo(name = "config_key")
    val configKey: String,

    val value: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    companion object {
        const val REFERENCE_DATA_VERSION = "reference_data_version"

        /* "2,4,6": the days of unsynced data that raise the three warnings. The wipe day is not stored. */
        const val SYNC_WARNING_DAYS = "sync_warning_days"
        const val SYNC_POLICY_VERSION = "sync_policy_version"
    }
}
