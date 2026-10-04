package com.beeftech.backend.api

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
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
            tagNumber = this[CalfRegistrationTable.tagNumber],
            animalUuid = this[CalfRegistrationTable.animalUuid],
            birthdate = this[CalfRegistrationTable.birthdate],
            breed = this[CalfRegistrationTable.breed],
            damTagNumber = this[CalfRegistrationTable.damTagNumber],
            sireTagNumber = this[CalfRegistrationTable.sireTagNumber],
            damAnimalUuid = this[CalfRegistrationTable.damAnimalUuid],
            sireAnimalUuid = this[CalfRegistrationTable.sireAnimalUuid],
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
        serverSyncedAt: Long,
        submittedBy: String? = null,
        submitterSiteId: String? = null
    ): CalfRegistrationDto = newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

        val existing = CalfRegistrationTable
            .selectAll()
            .where { CalfRegistrationTable.recordguid eq dto.recordguid }
            .singleOrNull()

        if (existing != null) {

            CalfRegistrationTable.update(
                { CalfRegistrationTable.recordguid eq dto.recordguid }
            ) {
                it[tagNumber] = dto.tagNumber
                it[animalUuid] = dto.animalUuid
                it[birthdate] = dto.birthdate
                it[breed] = dto.breed
                it[damTagNumber] = dto.damTagNumber
                it[sireTagNumber] = dto.sireTagNumber
                it[damAnimalUuid] = dto.damAnimalUuid
                it[sireAnimalUuid] = dto.sireAnimalUuid
                it[photoPath] = dto.photoPath
                it[videoPath] = dto.videoPath
                it[gpsLat] = dto.gpsLat
                it[gpsLng] = dto.gpsLng
                it[captureAt] = dto.captureAt
                it[deviceId] = dto.deviceId
                it[syncStatus] = "SYNCED"
                it[syncedAt] = serverSyncedAt
                it[submittedByUserId] = submittedBy
                it[siteId] = submitterSiteId
            }

        } else {

            CalfRegistrationTable.insert {
                it[tagNumber] = dto.tagNumber
                it[animalUuid] = dto.animalUuid
                it[birthdate] = dto.birthdate
                it[breed] = dto.breed
                it[damTagNumber] = dto.damTagNumber
                it[sireTagNumber] = dto.sireTagNumber
                it[damAnimalUuid] = dto.damAnimalUuid
                it[sireAnimalUuid] = dto.sireAnimalUuid
                it[photoPath] = dto.photoPath
                it[videoPath] = dto.videoPath
                it[gpsLat] = dto.gpsLat
                it[gpsLng] = dto.gpsLng
                it[captureAt] = dto.captureAt
                it[deviceId] = dto.deviceId
                it[recordguid] = dto.recordguid
                it[syncStatus] = "SYNCED"
                it[syncedAt] = serverSyncedAt
                it[submittedByUserId] = submittedBy
                it[siteId] = submitterSiteId
            }
        }

        dto.copy(
            syncStatus = "SYNCED",
            syncedAt = serverSyncedAt
        )
    }

    suspend fun findAll(scope: RecordScope = RecordScope.All): List<CalfRegistrationDto> = newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

        CalfRegistrationTable
            .selectAll()
            .where { scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt) }
            .map { it.toDto() }
    }

    suspend fun findByTagNumber(
        tagNumber: String,
        scope: RecordScope = RecordScope.All
    ): CalfRegistrationDto? = newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

        CalfRegistrationTable
            .selectAll()
            .where {
                (CalfRegistrationTable.tagNumber eq tagNumber) and
                    scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt)
            }
            .map { it.toDto() }
            .singleOrNull()
    }

    suspend fun updatePhotoPath(
        tagNumber: String,
        photoPath: String
    ): Boolean = newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

        val updatedCount = CalfRegistrationTable.update(
            { CalfRegistrationTable.tagNumber eq tagNumber }
        ) {
            it[CalfRegistrationTable.photoPath] = photoPath
        }
        updatedCount > 0
    }
}
