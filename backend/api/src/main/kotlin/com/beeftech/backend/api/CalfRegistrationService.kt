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
                    animalId = persisted.animalId,
                    status = "SYNCED",
                    serverSyncedAt = persisted.syncedAt
                )

            } catch (e: Exception) {

                CalfRegistrationSyncResult(
                    recordguid = dto.recordguid,
                    animalId = dto.animalId,
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

    suspend fun findByAnimalId(animalId: String): CalfRegistrationDto? {
        return repository.findByAnimalId(animalId)
    }

    suspend fun updateMedia(animalId: String, photoPath: String): Boolean {
        return repository.updatePhotoPath(animalId, photoPath)
    }

    suspend fun generateCertificatePdf(animalId: String): ByteArray? {
        val record = repository.findByAnimalId(animalId) ?: return null
        return PdfGenerator.generateBirthCertificate(record)
    }
}
