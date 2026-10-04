package com.beeftech.farmtraceability.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.BeefTechDatabase
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.farmtraceability.data.TraceabilityEventUpload
import com.beeftech.farmtraceability.data.TraceabilityOutboxApiClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TraceabilityOutboxWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork():
            Result {

        val database =
            DatabaseProvider
                .getDatabase()
                ?: return Result.retry()

        val tokenProvider =
            TokenProviderRegistry
                .get()
                ?: return Result.retry()

        val pendingRepository =
            PendingSyncRepository(
                database.pendingSyncDao()
            )

        return try {

            val operations =
                pendingRepository
                    .getPendingOperations()
                    .filter {
                        it.entityType in
                            SUPPORTED_ENTITY_TYPES
                    }

            if (
                operations.isEmpty()
            ) {

                return Result.success()
            }

            val uploads =
                mutableListOf<
                    Pair<
                        PendingSync,
                        TraceabilityEventUpload
                        >
                    >()

            for (
                operation in operations
            ) {

                val upload =
                    buildUpload(
                        database,
                        operation
                    )

                if (upload == null) {

                    /*
                     * Record no longer exists locally.
                     * Remove stale queue entry.
                     */
                    pendingRepository
                        .markSyncSuccessful(
                            operation.id
                        )

                } else {

                    uploads +=
                        operation to upload
                }
            }

            if (
                uploads.isEmpty()
            ) {

                return Result.success()
            }

            val apiClient =
                TraceabilityOutboxApiClient(
                    tokenProvider =
                        tokenProvider
                )

            val response =
                apiClient.syncEvents(
                    uploads.map {
                        it.second
                    }
                )

            if (
                response.isFailure
            ) {

                Log.w(
                    TAG,
                    "Outbox request failed: " +
                        response
                            .exceptionOrNull()
                            ?.message
                            .orEmpty()
                )

                return Result.retry()
            }

            val byGuid =
                response
                    .getOrThrow()
                    .results
                    .associateBy {
                        it.recordGuid
                    }

            uploads.forEach {
                    pair ->

                val operation =
                    pair.first

                val upload =
                    pair.second

                val syncResult =
                    byGuid[
                        upload.recordGuid
                    ]

                if (
                    syncResult
                        ?.status
                        ?.equals(
                            "SYNCED",
                            ignoreCase =
                                true
                        ) == true
                ) {

                    if (
                        operation.entityType ==
                        ENTITY_LOCATION_FEED
                    ) {

                        database
                            .animalMovementDao()
                            .markSynced(
                                upload.recordGuid,
                                syncResult
                                    .serverSyncedAt
                                    ?: System.currentTimeMillis()
                            )
                    }

                    pendingRepository
                        .markSyncSuccessful(
                            operation.id
                        )

                } else {

                    pendingRepository
                        .markSyncFailed(
                            operation.id
                        )
                }
            }

            val remaining =
                pendingRepository
                    .getPendingOperations()
                    .any {
                        it.entityType in
                            SUPPORTED_ENTITY_TYPES
                    }

            if (remaining) {
                Result.retry()
            } else {
                Result.success()
            }

        } catch (
            exception: Exception
        ) {

            Log.e(
                TAG,
                "Traceability outbox failed.",
                exception
            )

            Result.retry()
        }
    }


    private suspend fun buildUpload(
        database: BeefTechDatabase,
        operation: PendingSync
    ): TraceabilityEventUpload? {

        return when (
            operation.entityType
        ) {

            ENTITY_ANIMAL_PURCHASE -> {

                val purchase =
                    database
                        .animalPurchaseDao()
                        .findByRecordGuid(
                            operation.entityId
                        )
                        ?: return null

                TraceabilityEventUpload(
                    entityType =
                        ENTITY_ANIMAL_PURCHASE,

                    recordGuid =
                        purchase.recordGuid,

                    animalId =
                        purchase.animalId,

                    capturedAt =
                        purchase.purchaseDate,

                    payload =
                        buildJsonObject {

                            put(
                                "purchaseId",
                                purchase.purchaseId
                            )

                            put(
                                "sellerName",
                                purchase.sellerName
                            )

                            put(
                                "supplierFarmerId",
                                purchase
                                    .supplierFarmerId
                                    .orEmpty()
                            )

                            put(
                                "glnNumber",
                                purchase
                                    .glnNumber
                                    .orEmpty()
                            )

                            put(
                                "purchaseBatchNumber",
                                purchase
                                    .purchaseBatchNumber
                                    .orEmpty()
                            )

                            put(
                                "purchasePrice",
                                purchase.purchasePrice
                            )

                            put(
                                "notes",
                                purchase
                                    .notes
                                    .orEmpty()
                            )
                        }.toString()
                )
            }


            ENTITY_LOCATION_FEED -> {

                val record =
                    database
                        .animalMovementDao()
                        .findByRecordGuid(
                            operation.entityId
                        )
                        ?: return null

                TraceabilityEventUpload(
                    entityType =
                        ENTITY_LOCATION_FEED,

                    recordGuid =
                        record.recordGuid,

                    animalId =
                        record.animalId,

                    capturedAt =
                        record.capturedAt
                            .takeIf {
                                it > 0L
                            }
                            ?: record.movementDate,

                    payload =
                        buildJsonObject {

                            put(
                                "destination",
                                record.destinationFarmId
                            )

                            put(
                                "destinationPen",
                                record.destinationPenId
                            )

                            put(
                                "ration",
                                record
                                    .feedLocationType
                                    .orEmpty()
                            )

                            put(
                                "notes",
                                record
                                    .notes
                                    .orEmpty()
                            )
                        }.toString()
                )
            }


            ENTITY_MORTALITY -> {

                val mortality =
                    database
                        .mortalityDao()
                        .findByRecordGuid(
                            operation.entityId
                        )
                        ?: return null

                TraceabilityEventUpload(
                    entityType =
                        ENTITY_MORTALITY,

                    recordGuid =
                        mortality.recordGuid,

                    animalId =
                        mortality.animalId,

                    capturedAt =
                        mortality.timestamp,

                    payload =
                        buildJsonObject {

                            put(
                                "causeOfDeath",
                                mortality.causeOfDeath
                            )

                            put(
                                "responsibleWorker",
                                mortality.responsibleWorker
                            )

                            put(
                                "necropsyCodeId",
                                mortality
                                    .necropsyCodeId
                                    .orEmpty()
                            )

                            put(
                                "notes",
                                mortality
                                    .notes
                                    .orEmpty()
                            )
                        }.toString()
                )
            }


            ENTITY_ANIMAL_COST -> {

                val cost =
                    database
                        .animalCostDao()
                        .findByRecordGuid(
                            operation.entityId
                        )
                        ?: return null

                TraceabilityEventUpload(
                    entityType =
                        ENTITY_ANIMAL_COST,

                    recordGuid =
                        cost.recordGuid,

                    animalId =
                        cost.animalId,

                    capturedAt =
                        cost.timestamp,

                    payload =
                        buildJsonObject {

                            put(
                                "costType",
                                cost.costType
                            )

                            put(
                                "amount",
                                cost.amount
                            )

                            put(
                                "description",
                                cost.description
                            )

                            put(
                                "sourceEntity",
                                cost
                                    .sourceEntity
                                    .orEmpty()
                            )

                            put(
                                "sourceRecordId",
                                cost
                                    .sourceRecordId
                                    .orEmpty()
                            )
                        }.toString()
                )
            }


            else -> null
        }
    }


    companion object {

        private const val TAG =
            "TraceabilityOutbox"

        const val ENTITY_ANIMAL_PURCHASE =
            "ANIMAL_PURCHASE"

        const val ENTITY_LOCATION_FEED =
            "LOCATION_FEED"

        const val ENTITY_MORTALITY =
            "MORTALITY"

        const val ENTITY_ANIMAL_COST =
            "ANIMAL_COST"

        val SUPPORTED_ENTITY_TYPES =
            setOf(
                ENTITY_ANIMAL_PURCHASE,
                ENTITY_LOCATION_FEED,
                ENTITY_MORTALITY,
                ENTITY_ANIMAL_COST
            )
    }
}
