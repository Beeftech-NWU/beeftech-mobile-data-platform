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
/*
 * photo_path is set only by a real photo upload. The sync payload's photoPath is the
 * phone's local file path, which means nothing to the server, so it is ignored.
 */
data class CalfUpsertResult(val record: CalfRegistrationDto, val created: Boolean)

class CalfRegistrationRepository {

    private fun ResultRow.toDto(): CalfRegistrationDto {

        return CalfRegistrationDto(
            tagNumber = this[CalfRegistrationTable.tagNumber],
            animalUuid = this[CalfRegistrationTable.animalUuid],
            birthdate = this[CalfRegistrationTable.birthdate],
            breed = this[CalfRegistrationTable.breed],
            gender = this[CalfRegistrationTable.gender],
            hideColour = this[CalfRegistrationTable.hideColour],
            brandMark = this[CalfRegistrationTable.brandMark],
            birthWeightKg = this[CalfRegistrationTable.birthWeightKg],
            ageClass = this[CalfRegistrationTable.ageClass],
            bodyCondition = this[CalfRegistrationTable.bodyCondition],
            conformity = this[CalfRegistrationTable.conformity],
            oldTagNumber = this[CalfRegistrationTable.oldTagNumber],
            referenceNumber = this[CalfRegistrationTable.referenceNumber],
            processProof = this[CalfRegistrationTable.processProof],
            implantProof = this[CalfRegistrationTable.implantProof],
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
        submitterSiteId: String? = null,
        scope: RecordScope = RecordScope.User(submittedBy.orEmpty())
    ): CalfUpsertResult = newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

        val existing = CalfRegistrationTable
            .selectAll()
            .where { CalfRegistrationTable.recordguid eq dto.recordguid }
            .singleOrNull()

        if (existing != null) {

            /*
             * A caller may only update a record inside their own scope. A legacy row that
             * predates ownership (no submitter and no site) is claimed by the first caller
             * to sync it, so devices that registered calves before scoping keep syncing.
             */
            val unowned = existing[CalfRegistrationTable.submittedByUserId] == null &&
                existing[CalfRegistrationTable.siteId] == null

            val inScope = unowned || CalfRegistrationTable
                .selectAll()
                .where {
                    (CalfRegistrationTable.recordguid eq dto.recordguid) and
                        scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId)
                }
                .any()

            if (!inScope) {
                throw RecordOwnershipException(
                    "Record ${dto.recordguid} belongs to another user or site and cannot be overwritten."
                )
            }

            if (existing[CalfRegistrationTable.voidedAt] != null ||
                existing[CalfRegistrationTable.animalUuid] != dto.animalUuid ||
                (!unowned && existing[CalfRegistrationTable.siteId] != submitterSiteId)
            ) {
                throw RecordOwnershipException("Cannot overwrite a voided, reassigned, or mismatched calf record")
            }

            CalfRegistrationTable.update(
                { CalfRegistrationTable.recordguid eq dto.recordguid }
            ) {
                it[tagNumber] = dto.tagNumber
                it[animalUuid] = dto.animalUuid
                it[birthdate] = dto.birthdate
                it[breed] = dto.breed
                it[gender] = dto.gender
                it[hideColour] = dto.hideColour
                it[brandMark] = dto.brandMark
                it[birthWeightKg] = dto.birthWeightKg
                it[ageClass] = dto.ageClass
                it[bodyCondition] = dto.bodyCondition
                it[conformity] = dto.conformity
                it[oldTagNumber] = dto.oldTagNumber
                it[referenceNumber] = dto.referenceNumber
                it[processProof] = dto.processProof
                it[implantProof] = dto.implantProof
                it[damTagNumber] = dto.damTagNumber
                it[sireTagNumber] = dto.sireTagNumber
                it[damAnimalUuid] = dto.damAnimalUuid
                it[sireAnimalUuid] = dto.sireAnimalUuid
                it[videoPath] = dto.videoPath
                it[gpsLat] = dto.gpsLat
                it[gpsLng] = dto.gpsLng
                it[captureAt] = dto.captureAt
                it[deviceId] = dto.deviceId
                it[syncStatus] = "SYNCED"
                it[syncedAt] = serverSyncedAt
                if (unowned) {
                    it[submittedByUserId] = submittedBy
                    it[siteId] = submitterSiteId
                }
            }

        } else {

            CalfRegistrationTable.insert {
                it[tagNumber] = dto.tagNumber
                it[animalUuid] = dto.animalUuid
                it[birthdate] = dto.birthdate
                it[breed] = dto.breed
                it[gender] = dto.gender
                it[hideColour] = dto.hideColour
                it[brandMark] = dto.brandMark
                it[birthWeightKg] = dto.birthWeightKg
                it[ageClass] = dto.ageClass
                it[bodyCondition] = dto.bodyCondition
                it[conformity] = dto.conformity
                it[oldTagNumber] = dto.oldTagNumber
                it[referenceNumber] = dto.referenceNumber
                it[processProof] = dto.processProof
                it[implantProof] = dto.implantProof
                it[damTagNumber] = dto.damTagNumber
                it[sireTagNumber] = dto.sireTagNumber
                it[damAnimalUuid] = dto.damAnimalUuid
                it[sireAnimalUuid] = dto.sireAnimalUuid
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

        CalfUpsertResult(
            record = dto.copy(syncStatus = "SYNCED", syncedAt = serverSyncedAt),
            created = existing == null
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
        photoPath: String,
        scope: RecordScope = RecordScope.All
    ): Boolean = newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

        val updatedCount = CalfRegistrationTable.update(
            {
                (CalfRegistrationTable.tagNumber eq tagNumber) and
                    scope.predicate(CalfRegistrationTable.submittedByUserId, CalfRegistrationTable.siteId, CalfRegistrationTable.voidedAt)
            }
        ) {
            it[CalfRegistrationTable.photoPath] = photoPath
        }
        updatedCount > 0
    }
}

/** A sync tried to overwrite a record owned by another user or site. */
class RecordOwnershipException(message: String) : RuntimeException(message)
