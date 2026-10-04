package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.beeftech.database.entity.DeviceConfigEntry
import com.beeftech.database.entity.ReferenceItem

/* A value from the server's reference-data snapshot. [id] is the server's id. */
data class ReferenceValue(
    val id: Int,
    val name: String,
    val active: Boolean
)

data class CostTypeValue(
    val code: String,
    val displayName: String,
    val sortOrder: Int,
    val active: Boolean
)

data class ReferenceSnapshot(
    val version: Long,
    val diseases: List<ReferenceValue>,
    val treatmentTypes: List<ReferenceValue>,
    val costTypes: List<CostTypeValue>
)

@Dao
abstract class ReferenceDataDao {

    @Query("SELECT * FROM reference_items WHERE kind = :kind AND active = 1 ORDER BY display_name COLLATE NOCASE")
    abstract suspend fun getActive(kind: String): List<ReferenceItem>

    @Query("SELECT * FROM reference_items WHERE kind = :kind ORDER BY display_name COLLATE NOCASE")
    abstract suspend fun getAll(kind: String): List<ReferenceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertItems(items: List<ReferenceItem>)

    /* The treatment screen's disease picker points at `diseases`, so every server disease must exist there. */
    @Query("INSERT OR IGNORE INTO diseases (diseaseId, name) VALUES (:name, :name)")
    abstract suspend fun ensureDisease(name: String)

    @Query("INSERT OR IGNORE INTO cost_types (code, display_name, sort_order, is_active) VALUES (:code, :displayName, :sortOrder, :active)")
    abstract suspend fun ensureCostType(code: String, displayName: String, sortOrder: Int, active: Boolean)

    @Query("UPDATE cost_types SET display_name = :displayName, sort_order = :sortOrder, is_active = :active WHERE code = :code")
    abstract suspend fun updateCostType(code: String, displayName: String, sortOrder: Int, active: Boolean)

    @Query("SELECT value FROM device_config WHERE config_key = :key")
    abstract suspend fun getConfig(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putConfig(entry: DeviceConfigEntry)

    /*
     * Applies a snapshot in one transaction, so a device never ends up with half of one.
     *
     * Nothing here deletes. A value the server turned off keeps its row and only its active
     * flag changes, so records that already use it (treatments point at `diseases`, costs at
     * `cost_types`) still resolve. A value the server doesn't mention is left alone.
     */
    @Transaction
    open suspend fun apply(snapshot: ReferenceSnapshot, now: Long) {

        snapshot.diseases.forEach { ensureDisease(it.name) }

        upsertItems(
            snapshot.diseases.map {
                ReferenceItem(ReferenceItem.KIND_DISEASES, it.name, it.name, it.active, 0, it.id, now)
            } + snapshot.treatmentTypes.map {
                ReferenceItem(ReferenceItem.KIND_TREATMENT_TYPES, it.name, it.name, it.active, 0, it.id, now)
            }
        )

        snapshot.costTypes.forEach {
            /* Every treatment saves a TREATMENT cost, so that type is never switched off here. */
            val active = it.active || it.code == PROTECTED_COST_CODE
            ensureCostType(it.code, it.displayName, it.sortOrder, active)
            updateCostType(it.code, it.displayName, it.sortOrder, active)
        }

        putConfig(DeviceConfigEntry(DeviceConfigEntry.REFERENCE_DATA_VERSION, snapshot.version.toString(), now))
    }

    companion object {
        const val PROTECTED_COST_CODE = "TREATMENT"
    }
}
