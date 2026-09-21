package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class TreatmentRepository {

    private fun ResultRow.toDto(): TreatmentDto =
        TreatmentDto(
            animalId = this[TreatmentTable.animalId],
            disease = this[TreatmentTable.disease],
            treatmentName = this[TreatmentTable.treatmentName],
            batchNumber = this[TreatmentTable.batchNumber],
            volumeUsed = this[TreatmentTable.volumeUsed],
            cost = this[TreatmentTable.cost],
            timestamp = this[TreatmentTable.timestamp],
            deviceId = this[TreatmentTable.deviceId],
            recordguid = this[TreatmentTable.recordguid],
            syncStatus = this[TreatmentTable.syncStatus],
            syncedAt = this[TreatmentTable.syncedAt]
        )

    suspend fun upsertByRecordGuid(
        dto: TreatmentDto,
        serverSyncedAt: Long
    ): TreatmentDto =
        newSuspendedTransaction(Dispatchers.IO) {

            val existing =
                TreatmentTable
                    .selectAll()
                    .where {
                        TreatmentTable.recordguid eq dto.recordguid
                    }
                    .singleOrNull()

            if (existing != null) {

                TreatmentTable.update(
                    {
                        TreatmentTable.recordguid eq dto.recordguid
                    }
                ) {
                    it[animalId] = dto.animalId
                    it[disease] = dto.disease
                    it[treatmentName] = dto.treatmentName
                    it[batchNumber] = dto.batchNumber
                    it[volumeUsed] = dto.volumeUsed
                    it[cost] = dto.cost
                    it[timestamp] = dto.timestamp
                    it[deviceId] = dto.deviceId
                    it[syncStatus] = "SYNCED"
                    it[syncedAt] = serverSyncedAt
                }

            } else {

                TreatmentTable.insert {
                    it[animalId] = dto.animalId
                    it[disease] = dto.disease
                    it[treatmentName] = dto.treatmentName
                    it[batchNumber] = dto.batchNumber
                    it[volumeUsed] = dto.volumeUsed
                    it[cost] = dto.cost
                    it[timestamp] = dto.timestamp
                    it[deviceId] = dto.deviceId
                    it[recordguid] = dto.recordguid
                    it[syncStatus] = "SYNCED"
                    it[syncedAt] = serverSyncedAt
                }
            }

            TreatmentDto(
                animalId = dto.animalId,
                disease = dto.disease,
                treatmentName = dto.treatmentName,
                batchNumber = dto.batchNumber,
                volumeUsed = dto.volumeUsed,
                cost = dto.cost,
                timestamp = dto.timestamp,
                deviceId = dto.deviceId,
                recordguid = dto.recordguid,
                syncStatus = "SYNCED",
                syncedAt = serverSyncedAt
            )
        }

    suspend fun findAll(): List<TreatmentDto> =
        newSuspendedTransaction(Dispatchers.IO) {
            TreatmentTable
                .selectAll()
                .map { it.toDto() }
        }

    suspend fun findByAnimalId(
        animalId: String
    ): List<TreatmentDto> =
        newSuspendedTransaction(Dispatchers.IO) {
            TreatmentTable
                .selectAll()
                .where {
                    TreatmentTable.animalId eq animalId
                }
                .map { it.toDto() }
        }
}
