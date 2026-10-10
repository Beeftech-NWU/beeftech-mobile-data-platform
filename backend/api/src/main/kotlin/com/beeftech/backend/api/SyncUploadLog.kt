package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.SitesTable
import com.beeftech.backend.api.common.ApiResponse
import com.beeftech.backend.api.common.FileNaming
import com.beeftech.backend.api.common.FileNaming.ProjectCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.beeftech.backend.api.SyncUploadLog")

/**
 * One row per sync upload, named by the phone as [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID].
 * [batchName] is null for uploads from app versions that do not send one yet.
 */
object SyncUploadLogTable : Table("sync_upload_log") {
    val id = integer("id").autoIncrement()
    val batchName = varchar("batch_name", 255).nullable().uniqueIndex()
    val project = varchar("project", 32)
    val deviceId = varchar("device_id", 255).nullable()
    val userId = varchar("user_id", 128).nullable()
    val siteId = varchar("site_id", 64).nullable()
    val recordCount = integer("record_count")
    val accepted = integer("accepted")
    val rejected = integer("rejected")
    val receivedAt = long("received_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Checks and logs the batch name sent with a sync upload.
 *
 * A missing name is accepted and logged as legacy, so devices on older app versions keep working.
 * A name that is present must match the agreed format, the caller's own site code, the project of
 * the endpoint and the caller's device. A repeated name (a retry) updates its row, so the
 * endpoints stay idempotent.
 */
object SyncUploadLog {

    /** Null when the name is acceptable, otherwise why it is not. */
    suspend fun problemWith(principal: AuthPrincipal, project: ProjectCode, batchName: String?): String? {
        if (batchName == null) return null

        val parts = FileNaming.parse(batchName)
            ?: return "Batch name is not in the agreed format [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]."

        if (parts.project != project.name) {
            return "Batch name is for ${parts.project}, not ${project.name}."
        }

        val siteFarmCode = principal.siteId?.let { farmCodeOfSite(it) }
        if (siteFarmCode != null && parts.farmCode != siteFarmCode) {
            return "Batch name farm code ${parts.farmCode} does not match this site's farm code."
        }

        val device = principal.deviceId?.let(::cleanDeviceId)
        if (device != null && parts.deviceId != device) {
            return "Batch name device ${parts.deviceId} does not match the signed-in device."
        }

        return null
    }

    /**
     * Records the upload. Never throws: a logging problem must not fail a sync that already saved
     * the records. [statuses] holds one status per record, and "SYNCED" counts as accepted.
     */
    suspend fun record(
        principal: AuthPrincipal,
        project: ProjectCode,
        batchName: String?,
        statuses: List<String>
    ) {
        try {
            val accepted = statuses.count { it == "SYNCED" }
            val now = System.currentTimeMillis()

            newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
                val existing = batchName?.let {
                    SyncUploadLogTable.selectAll().where { SyncUploadLogTable.batchName eq it }.any()
                } ?: false

                if (existing) {
                    SyncUploadLogTable.update({ SyncUploadLogTable.batchName eq batchName!! }) {
                        it[recordCount] = statuses.size
                        it[SyncUploadLogTable.accepted] = accepted
                        it[rejected] = statuses.size - accepted
                        it[receivedAt] = now
                    }
                } else {
                    SyncUploadLogTable.insert {
                        it[SyncUploadLogTable.batchName] = batchName
                        it[SyncUploadLogTable.project] = project.name
                        it[deviceId] = principal.deviceId
                        it[userId] = principal.userId
                        it[siteId] = principal.siteId
                        it[recordCount] = statuses.size
                        it[SyncUploadLogTable.accepted] = accepted
                        it[rejected] = statuses.size - accepted
                        it[receivedAt] = now
                    }
                }
            }
        } catch (exception: Exception) {
            log.warn("Could not log sync upload {}: {}", batchName ?: "(no batch name)", exception.message)
        }
    }

    /* The same cleaning the generator applies to a device id. */
    private fun cleanDeviceId(deviceId: String) = deviceId.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
}

/** Answers 400 and returns false when the upload's batch name is not acceptable. */
suspend fun ApplicationCall.acceptBatch(principal: AuthPrincipal, project: ProjectCode, batchName: String?): Boolean {
    val problem = SyncUploadLog.problemWith(principal, project, batchName) ?: return true

    respond(
        HttpStatusCode.BadRequest,
        ApiResponse<String>(success = false, message = problem)
    )
    return false
}

/** The site's sales rep email, or null when the site is unknown or has no rep set. */
suspend fun salesRepEmailOfSite(siteId: String): String? =
    newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
        SitesTable.selectAll()
            .where { SitesTable.siteId eq siteId }
            .singleOrNull()
            ?.get(SitesTable.salesRepEmail)
    }

/** The site's current farm code, or null when the site is unknown or has none yet. */
suspend fun farmCodeOfSite(siteId: String): String? =
    newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
        SitesTable.selectAll()
            .where { SitesTable.siteId eq siteId }
            .singleOrNull()
            ?.get(SitesTable.farmCode)
    }
