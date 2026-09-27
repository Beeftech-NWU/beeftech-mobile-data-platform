package com.beeftech.backend.api

class CalfRegistrationService(private val repository: CalfRegistrationRepository) {

    suspend fun syncRecords(request: CalfRegistrationSyncRequest): CalfRegistrationSyncResponse {

        val results = request.records.map { dto ->

            try {

                val persisted = repository.upsertByRecordGuid(
                    dto,
                    System.currentTimeMillis()
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

    suspend fun listAll(): List<CalfRegistrationDto> {
        return repository.findAll()
    }

    suspend fun findByTagNumber(tagNumber: String): CalfRegistrationDto? {
        return repository.findByTagNumber(tagNumber)
    }

    suspend fun updateMedia(tagNumber: String, photoPath: String): Boolean {
        return repository.updatePhotoPath(tagNumber, photoPath)
    }

    suspend fun generateCertificatePdf(tagNumber: String): ByteArray? {
        val record = repository.findByTagNumber(tagNumber) ?: return null
        return PdfGenerator.generateBirthCertificate(record)
    }
}
