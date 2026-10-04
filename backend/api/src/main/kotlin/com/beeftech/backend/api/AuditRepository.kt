package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/* The values of audit_log.action. */
object AuditActions {
    const val VOID = "VOID"
    const val USER_CREATE = "USER_CREATE"
    const val USER_UPDATE = "USER_UPDATE"
    const val USER_RESET_PIN = "USER_RESET_PIN"
    const val USER_UNBIND_DEVICE = "USER_UNBIND_DEVICE"
    const val SITE_CREATE = "SITE_CREATE"
    const val SITE_UPDATE = "SITE_UPDATE"
    const val DEVICE_REVOKE = "DEVICE_REVOKE"
    const val DEVICE_REINSTATE = "DEVICE_REINSTATE"
    const val LOGIN_UNLOCK = "LOGIN_UNLOCK"
    const val REFDATA_CREATE = "REFDATA_CREATE"
    const val REFDATA_ACTIVATE = "REFDATA_ACTIVATE"
    const val REFDATA_DEACTIVATE = "REFDATA_DEACTIVATE"
    const val SYNC_POLICY_UPDATE = "SYNC_POLICY_UPDATE"

    /* entity_type for actions on a user account. */
    const val ENTITY_USER = "USER"

    /* entity_type for actions on a site. */
    const val ENTITY_SITE = "SITE"

    /* entity_type for actions on a phone. */
    const val ENTITY_DEVICE = "DEVICE"

    /* entity_type for the sync policy (there is only one). */
    const val ENTITY_SYNC_POLICY = "SYNC_POLICY"
}

class AuditEntry(
    val action: String,
    val entityType: String,
    val entityId: String,
    val actorUserId: String,
    val actorUsername: String,
    val actorRole: Int,
    /* The affected record's (or user's) site, so a manager can read the log for their own site. */
    val siteId: String?,
    val reason: String = "",
    val details: JsonObject? = null
)

/* Details are strings only, so a PIN or hash can't slip in as a typed value. */
fun auditDetails(vararg pairs: Pair<String, String>): JsonObject =
    buildJsonObject { pairs.forEach { (key, value) -> put(key, value) } }

/*
 * Writes the audit row in the caller's transaction, so a change is never recorded
 * without its audit entry or the other way round. Call it inside a transaction.
 */
fun insertAuditRow(entry: AuditEntry, now: Long = System.currentTimeMillis()) {
    AuditLogTable.insert {
        it[action] = entry.action
        it[entityType] = entry.entityType
        it[entityId] = entry.entityId
        it[reason] = entry.reason
        it[details] = entry.details?.toString()
        it[actorUserId] = entry.actorUserId
        it[actorUsername] = entry.actorUsername
        it[actorRole] = entry.actorRole
        it[siteId] = entry.siteId
        it[createdAt] = now
    }
}

/* All fields optional; [before] is an audit row id, for paging. */
class AuditFilter(
    val action: String? = null,
    val entityType: String? = null,
    val entityId: String? = null,
    val actorUserId: String? = null,
    val siteId: String? = null,
    val from: Long? = null,
    val to: Long? = null,
    val before: Long? = null
)

class AuditRepository {

    /* Newest first by id, so "before" pages stably. A manager's [siteScope] is their own site. */
    suspend fun query(filter: AuditFilter, siteScope: SiteScope, limit: Int): List<AuditLogEntryDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            var where: Op<Boolean> = Op.TRUE
            if (siteScope is SiteScope.Only) where = where and (AuditLogTable.siteId eq siteScope.siteId)
            filter.siteId?.let { where = where and (AuditLogTable.siteId eq it) }
            filter.action?.let { where = where and (AuditLogTable.action eq it) }
            filter.entityType?.let { where = where and (AuditLogTable.entityType eq it) }
            filter.entityId?.let { where = where and (AuditLogTable.entityId eq it) }
            filter.actorUserId?.let { where = where and (AuditLogTable.actorUserId eq it) }
            filter.from?.let { where = where and (AuditLogTable.createdAt greaterEq it) }
            filter.to?.let { where = where and (AuditLogTable.createdAt lessEq it) }
            filter.before?.let { where = where and (AuditLogTable.id less it) }

            AuditLogTable
                .selectAll()
                .where { where }
                .orderBy(AuditLogTable.id, SortOrder.DESC)
                .limit(limit)
                .map {
                    AuditLogEntryDto(
                        id = it[AuditLogTable.id],
                        action = it[AuditLogTable.action],
                        entityType = it[AuditLogTable.entityType],
                        entityId = it[AuditLogTable.entityId],
                        reason = it[AuditLogTable.reason],
                        actorUserId = it[AuditLogTable.actorUserId],
                        actorUsername = it[AuditLogTable.actorUsername],
                        actorRole = it[AuditLogTable.actorRole],
                        siteId = it[AuditLogTable.siteId],
                        createdAt = it[AuditLogTable.createdAt],
                        details = it[AuditLogTable.details]
                    )
                }
        }
}
