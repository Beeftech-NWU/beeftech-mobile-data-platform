package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Note on query style: `Table.selectAll().where { ... }` (rather than the
 * older `Table.select { ... }`) is intentional, not a stray refactor -
 * the predicate-taking `select { ... }` overload is deprecated (at
 * `DeprecationLevel.ERROR`, i.e. it no longer compiles) as of the Exposed
 * version pinned in this project (0.56.0). `selectAll().where { ... }` is
 * the current, supported replacement.
 */
class CalfRegistrationRepository {

    private fun ResultRow.toDto(): CalfRegistrationDto {

        return CalfRegistrationDto(
            animalId = this[CalfRegistrationTable.animalId],
            birthdate = this[CalfRegistrationTable.birthdate],
            breed = this[CalfRegistrationTable.breed],
            damId = this[CalfRegistrationTable.damId],
            sireId = this[CalfRegistrationTable.sireId],
            photoPath = this[CalfRegistrationTable.photoPath],
            videoPath = this[CalfRegistrationTable.videoPath],
            gpsLat = this[CalfRegistrationTable.gpsLat],
            gpsLng = this[CalfRegistrationTable.gpsLng],
            captureAt = this[CalfRegistrationTable.captureAt],
            deviceId = this[CalfRegistrationTable.deviceId],
            recordguid = this[CalfRegistrationTable.recordguid],
            syncStatus = this[CalfRegistrationTable.syncStatus],
            syncedAt = this[CalfRegistrationTable.syncedAt]
        )
    }

    suspend fun upsertByRecordGuid(
        dto: CalfRegistrationDto,
        serverSyncedAt: Long
    ): CalfRegistrationDto = newSuspendedTransaction(Dispatchers.IO) {

        val existing = CalfRegistrationTable
            .selectAll()
            .where { CalfRegistrationTable.recordguid eq dto.recordguid }
            .singleOrNull()

        if (existing != null) {

            CalfRegistrationTable.update(
                { CalfRegistrationTable.recordguid eq dto.recordguid }
            ) {
                it[animalId] = dto.animalId
                it[birthdate] = dto.birthdate
                it[breed] = dto.breed
                it[damId] = dto.damId
                it[sireId] = dto.sireId
                it[photoPath] = dto.photoPath
                it[videoPath] = dto.videoPath
                it[gpsLat] = dto.gpsLat
                it[gpsLng] = dto.gpsLng
                it[captureAt] = dto.captureAt
                it[deviceId] = dto.deviceId
                it[syncStatus] = "SYNCED"
                it[syncedAt] = serverSyncedAt
            }

        } else {

            CalfRegistrationTable.insert {
                it[animalId] = dto.animalId
                it[birthdate] = dto.birthdate
                it[breed] = dto.breed
                it[damId] = dto.damId
                it[sireId] = dto.sireId
                it[photoPath] = dto.photoPath
                it[videoPath] = dto.videoPath
                it[gpsLat] = dto.gpsLat
                it[gpsLng] = dto.gpsLng
                it[captureAt] = dto.captureAt
                it[deviceId] = dto.deviceId
                it[recordguid] = dto.recordguid
                it[syncStatus] = "SYNCED"
                it[syncedAt] = serverSyncedAt
            }
        }

        dto.copy(
            syncStatus = "SYNCED",
            syncedAt = serverSyncedAt
        )
    }

    suspend fun findAll(): List<CalfRegistrationDto> = newSuspendedTransaction(Dispatchers.IO) {

        CalfRegistrationTable
            .selectAll()
            .map { it.toDto() }
    }

    suspend fun findByAnimalId(animalId: String): CalfRegistrationDto? = newSuspendedTransaction(Dispatchers.IO) {

        CalfRegistrationTable
            .selectAll()
            .where { CalfRegistrationTable.animalId eq animalId }
            .map { it.toDto() }
            .singleOrNull()
    }

    suspend fun updatePhotoPath(
        animalId: String,
        photoPath: String
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {

        val updatedCount = CalfRegistrationTable.update(
            { CalfRegistrationTable.animalId eq animalId }
        ) {
            it[CalfRegistrationTable.photoPath] = photoPath
        }
        updatedCount > 0
    }
}
