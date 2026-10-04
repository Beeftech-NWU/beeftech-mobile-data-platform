package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A reference value the server publishes that has no table of its own on the device: the
 * disease and treatment-type pickers on the Treatment screen. Diseases are also copied into
 * `diseases` (a treatment points at that table), and cost types live in `cost_types`.
 *
 * A value the server turns off is kept here with [active] = false, never deleted, so a record
 * that already uses it still resolves. No foreign keys point at this table.
 */
@Entity(
    tableName = "reference_items",
    primaryKeys = ["kind", "item_key"]
)
data class ReferenceItem(
    /* [KIND_DISEASES] or [KIND_TREATMENT_TYPES]. */
    val kind: String,

    @ColumnInfo(name = "item_key")
    val itemKey: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,

    /* The server's id for the value; null if it didn't send one. */
    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    companion object {
        const val KIND_DISEASES = "diseases"
        const val KIND_TREATMENT_TYPES = "treatment_types"
    }
}
