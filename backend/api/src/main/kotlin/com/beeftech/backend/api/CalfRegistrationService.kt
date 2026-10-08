package com.beeftech.backend.api

sealed interface PhotoUploadOutcome {
    data class Stored(val path: String) : PhotoUploadOutcome
    data object NotFound : PhotoUploadOutcome
    data object NotAJpeg : PhotoUploadOutcome
    data object TooLarge : PhotoUploadOutcome
}

class CalfRegistrationService(
    private val repository: CalfRegistrationRepository,
    private val photoStore: CalfPhotoStore
) {

    suspend fun syncRecords(
        request: CalfRegistrationSyncRequest,
        submittedBy: String? = null,
        siteId: String? = null,
        scope: RecordScope = RecordScope.User(submittedBy.orEmpty())
    ): CalfRegistrationSyncResponse {

        val results = request.records.map { dto ->

            try {

                val persisted = repository.upsertByRecordGuid(
                    dto,
                    System.currentTimeMillis(),
                    submittedBy,
                    siteId,
                    scope
                )

                CalfRegistrationSyncResult(
                    recordguid = persisted.recordguid,
                    tagNumber = persisted.tagNumber,
                    status = "SYNCED",
                    serverSyncedAt = persisted.syncedAt
                )

            } catch (e: Exception) {

                CalfRegistrationSyncResult(
                    recordguid = dto.recordguid,
                    tagNumber = dto.tagNumber,
                    status = "ERROR",
                    message = e.message
                )
            }
        }

        return CalfRegistrationSyncResponse(results = results)
    }

    suspend fun listAll(scope: RecordScope = RecordScope.All): List<CalfRegistrationDto> {
        return repository.findAll(scope)
    }

    suspend fun findByTagNumber(
        tagNumber: String,
        scope: RecordScope = RecordScope.All
    ): CalfRegistrationDto? {
        return repository.findByTagNumber(tagNumber, scope)
    }

    /** Stores the calf's photo. The record must exist and be inside the caller's scope. */
    suspend fun storePhoto(tagNumber: String, bytes: ByteArray, scope: RecordScope): PhotoUploadOutcome {
        if (bytes.size > CalfPhotoStore.MAX_BYTES) return PhotoUploadOutcome.TooLarge
        if (!CalfPhotoStore.looksLikeJpeg(bytes)) return PhotoUploadOutcome.NotAJpeg

        val record = repository.findByTagNumber(tagNumber, scope) ?: return PhotoUploadOutcome.NotFound

        photoStore.save(record.recordguid, bytes)

        val path = "/api/calf-registrations/$tagNumber/photo"
        repository.updatePhotoPath(tagNumber, path, scope)
        return PhotoUploadOutcome.Stored(path)
    }

    suspend fun loadPhoto(tagNumber: String, scope: RecordScope): ByteArray? {
        val record = repository.findByTagNumber(tagNumber, scope) ?: return null
        return photoStore.read(record.recordguid)
    }

    suspend fun generateCertificatePdf(tagNumber: String, scope: RecordScope = RecordScope.All): ByteArray? {
        val record = repository.findByTagNumber(tagNumber, scope) ?: return null
        return PdfGenerator.generateBirthCertificate(record)
    }
}
