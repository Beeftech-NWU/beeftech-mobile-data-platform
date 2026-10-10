package com.beeftech.backend.api

import com.beeftech.backend.api.common.FileNaming.ProjectCode
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.common.ApiResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


@Serializable
data class TraceabilityEventUpload(
    val entityType: String,
    val recordGuid: String,
    val animalId: String? = null,
    val capturedAt: Long,
    val payload: String
)


@Serializable
data class TraceabilityEventSyncRequest(
    val records:
        List<TraceabilityEventUpload>,
    val batchName: String? = null
)


@Serializable
data class TraceabilityEventSyncResult(
    val recordGuid: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)


@Serializable
data class TraceabilityEventSyncResponse(
    val results:
        List<TraceabilityEventSyncResult>
)


object TraceabilityEventTable :
        Table(
            "traceability_events"
        ) {

    val recordGuid =
        varchar(
            "record_guid",
            128
        )

    val entityType =
        varchar(
            "entity_type",
            64
        )

    val animalId =
        varchar(
            "animal_id",
            128
        ).nullable()

    val capturedAt =
        long(
            "captured_at"
        )

    val payload =
        text(
            "payload"
        )

    val submittedByUserId =
        varchar(
            "submitted_by_user_id",
            128
        ).nullable()

    val siteId =
        varchar(
            "site_id",
            128
        ).nullable()

    val serverSyncedAt =
        long(
            "server_synced_at"
        )

    override val primaryKey =
        PrimaryKey(
            recordGuid
        )
}


fun Route.traceabilityEventRoutes(
    jwtService: JwtService
) {

    post(
        "/api/traceability-events/sync"
    ) {

        val principal =
            call.requireAuthPrincipal(
                jwtService
            )
                ?: return@post


        val request =
            call.receive<
                TraceabilityEventSyncRequest
                >()


        if (!call.acceptBatch(principal, ProjectCode.TRACE_EVENT, request.batchName)) return@post


        val response =
            transaction {

                val results =
                    request.records.map {
                            record ->

                        try {

                            require(
                                record.recordGuid
                                    .isNotBlank()
                            ) {
                                "recordGuid is required"
                            }

                            require(
                                record.entityType
                                    .isNotBlank()
                            ) {
                                "entityType is required"
                            }


                            val now =
                                System.currentTimeMillis()


                            val existing =
                                TraceabilityEventTable
                                    .selectAll()
                                    .where {
                                        TraceabilityEventTable
                                            .recordGuid eq
                                            record.recordGuid
                                    }
                                    .limit(1)
                                    .firstOrNull()


                            if (
                                existing == null
                            ) {

                                TraceabilityEventTable
                                    .insert {

                                        it[recordGuid] =
                                            record.recordGuid

                                        it[entityType] =
                                            record.entityType

                                        it[animalId] =
                                            record.animalId

                                        it[capturedAt] =
                                            record.capturedAt

                                        it[payload] =
                                            record.payload

                                        it[submittedByUserId] =
                                            principal.userId

                                        it[siteId] =
                                            principal.siteId

                                        it[serverSyncedAt] =
                                            now
                                    }

                            } else {

                                TraceabilityEventTable
                                    .update(
                                        {
                                            TraceabilityEventTable
                                                .recordGuid eq
                                                record.recordGuid
                                        }
                                    ) {

                                        it[entityType] =
                                            record.entityType

                                        it[animalId] =
                                            record.animalId

                                        it[capturedAt] =
                                            record.capturedAt

                                        it[payload] =
                                            record.payload

                                        it[submittedByUserId] =
                                            principal.userId

                                        it[siteId] =
                                            principal.siteId

                                        it[serverSyncedAt] =
                                            now
                                    }
                            }


                            TraceabilityEventSyncResult(
                                recordGuid =
                                    record.recordGuid,

                                status =
                                    "SYNCED",

                                serverSyncedAt =
                                    now
                            )

                        } catch (
                            exception: Exception
                        ) {

                            TraceabilityEventSyncResult(
                                recordGuid =
                                    record.recordGuid,

                                status =
                                    "FAILED",

                                message =
                                    exception.message
                                        ?: "Traceability event synchronization failed."
                            )
                        }
                    }


                TraceabilityEventSyncResponse(
                    results =
                        results
                )
            }


        SyncUploadLog.record(principal, ProjectCode.TRACE_EVENT, request.batchName, response.results.map { it.status })

        call.respond(
            ApiResponse(
                success = true,
                message =
                    "Traceability synchronization completed.",
                data =
                    response
            )
        )
    }
}
