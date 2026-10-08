package com.beeftech.backend.api.auth

import com.beeftech.backend.api.AuditEntry
import com.beeftech.backend.api.DatabaseFactory
import com.beeftech.backend.api.insertAuditRow
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class SiteRepository {

    /* Sorted by name. [onlySiteId] narrows the list to one site (a manager's own). */
    suspend fun list(onlySiteId: String? = null): List<SiteDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val counts = activeUserCounts()
            SitesTable.selectAll()
                .apply { if (onlySiteId != null) where { SitesTable.siteId eq onlySiteId } }
                .map { it.toDto(counts[it[SitesTable.siteId]] ?: 0L) }
                .sortedBy { it.name.lowercase() }
        }

    suspend fun find(siteId: String): SiteDto? =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            SitesTable.selectAll()
                .where { SitesTable.siteId eq siteId }
                .singleOrNull()
                ?.toDto(activeUserCounts()[siteId] ?: 0L)
        }

    /* True when another site (not [exceptSiteId]) already has this name, ignoring case. */
    suspend fun nameTaken(name: String, exceptSiteId: String? = null): Boolean =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            SitesTable.selectAll()
                .where { SitesTable.name.lowerCase() eq name.lowercase() }
                .any { it[SitesTable.siteId] != exceptSiteId }
        }

    /* True when another site (not [exceptSiteId]) already has this farm code. */
    suspend fun farmCodeTaken(farmCode: String, exceptSiteId: String? = null): Boolean =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            SitesTable.selectAll()
                .where { SitesTable.farmCode eq farmCode }
                .any { it[SitesTable.siteId] != exceptSiteId }
        }

    suspend fun insert(siteId: String, name: String, farmCode: String, now: Long, audit: AuditEntry) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            insertAuditRow(audit, now)
            SitesTable.insert {
                it[SitesTable.siteId] = siteId
                it[SitesTable.name] = name
                it[SitesTable.farmCode] = farmCode
                it[createdAt] = now
                it[active] = true
            }
        }
    }

    suspend fun update(siteId: String, name: String, active: Boolean, farmCode: String?, now: Long, audit: AuditEntry) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            insertAuditRow(audit, now)
            SitesTable.update({ SitesTable.siteId eq siteId }) {
                it[SitesTable.name] = name
                it[SitesTable.active] = active
                it[SitesTable.farmCode] = farmCode
                it[updatedAt] = now
            }
        }
    }

    private fun activeUserCounts(): Map<String, Long> =
        UsersTable.select(UsersTable.siteId)
            .where { (UsersTable.active eq true) and UsersTable.siteId.isNotNull() }
            .groupingBy { it[UsersTable.siteId]!! }
            .eachCount()
            .mapValues { it.value.toLong() }

    private fun ResultRow.toDto(activeUserCount: Long) = SiteDto(
        siteId = this[SitesTable.siteId],
        name = this[SitesTable.name],
        active = this[SitesTable.active],
        createdAt = this[SitesTable.createdAt],
        updatedAt = this[SitesTable.updatedAt],
        farmCode = this[SitesTable.farmCode],
        activeUserCount = activeUserCount
    )
}
