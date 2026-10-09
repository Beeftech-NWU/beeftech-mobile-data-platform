package com.beeftech.backend.api

class CalfRegistrationService(
    private val repository: CalfRegistrationRepository,
    private val notificationService: FarmerSalesNotificationService? = null
) {

    suspend fun syncRecords(
        request: CalfRegistrationSyncRequest,
        submittedBy: String? = null,
        siteId: String? = null
    ): CalfRegistrationSyncResponse {

        val results = request.records.map { dto ->

            try {

                val saved = repository.upsertByRecordGuid(
                    dto,
                    System.currentTimeMillis(),
                    submittedBy,
                    siteId
                )

                val persisted = saved.record
                if (saved.created) {
                    try {
                        notificationService?.notifyCalfRegistration(
                            CalfRegistrationNotificationPayload(
                                recordGuid = persisted.recordguid,
                                animalUuid = persisted.animalUuid,
                                tagNumber = persisted.tagNumber,
                                breed = persisted.breed,
                                birthdate = persisted.birthdate,
                                captureAt = persisted.captureAt,
                                deviceId = persisted.deviceId,
                                siteId = siteId,
                                submittedByUserId = submittedBy,
                                serverSyncedAt = persisted.syncedAt ?: System.currentTimeMillis()
                            )
                        )
                    } catch (_: Exception) {
                        System.err.println("Calf synchronized, but JSON email delivery failed (check SMTP configuration).")
                    }
                }
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

    suspend fun updateMedia(tagNumber: String, photoPath: String, scope: RecordScope): Boolean {
        return repository.updatePhotoPath(tagNumber, photoPath, scope)
    }

    suspend fun generateCertificatePdf(tagNumber: String, scope: RecordScope): ByteArray? {
        val record = repository.findByTagNumber(tagNumber, scope) ?: return null
        return PdfGenerator.generateBirthCertificate(record)
    }
}
