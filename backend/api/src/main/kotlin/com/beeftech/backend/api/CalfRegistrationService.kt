package com.beeftech.backend.api

class CalfRegistrationService(private val repository: CalfRegistrationRepository) {

    suspend fun syncRecords(
        request: CalfRegistrationSyncRequest,
        submittedBy: String? = null,
        siteId: String? = null
    ): CalfRegistrationSyncResponse {

        val results = request.records.map { dto ->

            try {

                val persisted = repository.upsertByRecordGuid(
                    dto,
                    System.currentTimeMillis(),
                    submittedBy,
                    siteId
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

    suspend fun updateMedia(tagNumber: String, photoPath: String): Boolean {
        return repository.updatePhotoPath(tagNumber, photoPath)
    }

    suspend fun generateCertificatePdf(tagNumber: String): ByteArray? {
        val record = repository.findByTagNumber(tagNumber) ?: return null
        return PdfGenerator.generateBirthCertificate(record)
    }
}
