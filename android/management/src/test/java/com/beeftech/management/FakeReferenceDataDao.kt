package com.beeftech.management

import com.beeftech.database.dao.ReferenceDataDao
import com.beeftech.database.entity.DeviceConfigEntry
import com.beeftech.database.entity.ReferenceItem

/*
 * In-memory stand-in for the Room DAO, so the pull logic can be tested on the JVM. It keeps the
 * same rules as the real SQL: values are added or updated, never removed. The real queries are
 * covered by ReferenceDataDaoTest on a device.
 */
class FakeReferenceDataDao : ReferenceDataDao() {

    val items = mutableMapOf<Pair<String, String>, ReferenceItem>()
    val diseases = mutableSetOf<String>()
    val costTypes = mutableMapOf<String, Triple<String, Int, Boolean>>()
    val config = mutableMapOf<String, String>()
    var applyCount = 0

    override suspend fun getActive(kind: String) =
        items.values.filter { it.kind == kind && it.active }.sortedBy { it.displayName.lowercase() }

    override suspend fun getAll(kind: String) =
        items.values.filter { it.kind == kind }.sortedBy { it.displayName.lowercase() }

    override suspend fun upsertItems(items: List<ReferenceItem>) {
        items.forEach { this.items[it.kind to it.itemKey] = it }
    }

    override suspend fun ensureDisease(name: String) {
        diseases += name
    }

    override suspend fun ensureCostType(code: String, displayName: String, sortOrder: Int, active: Boolean) {
        costTypes.putIfAbsent(code, Triple(displayName, sortOrder, active))
    }

    override suspend fun updateCostType(code: String, displayName: String, sortOrder: Int, active: Boolean) {
        if (code in costTypes) costTypes[code] = Triple(displayName, sortOrder, active)
    }

    override suspend fun getConfig(key: String): String? = config[key]

    override suspend fun putConfig(entry: DeviceConfigEntry) {
        config[entry.configKey] = entry.value
    }

    override suspend fun apply(snapshot: com.beeftech.database.dao.ReferenceSnapshot, now: Long) {
        applyCount++
        super.apply(snapshot, now)
    }
}
