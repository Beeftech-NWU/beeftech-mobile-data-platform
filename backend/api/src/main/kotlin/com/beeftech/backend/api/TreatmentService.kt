package com.beeftech.backend.api

class TreatmentService(
    private val repository: TreatmentRepository
) {

    suspend fun syncRecords(
        request: TreatmentSyncRequest,
        submittedBy: String? = null,
        siteId: String? = null
    ): TreatmentSyncResponse {

        val results =
            request.records.map { dto ->

                try {
                    val serverSyncedAt =
                        System.currentTimeMillis()

                    val persisted =
                        repository.upsertByRecordGuid(
                            dto = dto,
                            serverSyncedAt = serverSyncedAt,
                            submittedBy = submittedBy,
                            submitterSiteId = siteId
                        )

                    TreatmentSyncResult(
                        recordguid = persisted.recordguid,
                        animalId = persisted.animalId,
                        status = "SYNCED",
                        serverSyncedAt =
                            persisted.syncedAt
                                ?: serverSyncedAt
                    )

                } catch (exception: Exception) {

                    TreatmentSyncResult(
                        recordguid = dto.recordguid,
                        animalId = dto.animalId,
                        status = "ERROR",
                        message =
                            exception.message
                                ?: "Unable to synchronize treatment."
                    )
                }
            }

        return TreatmentSyncResponse(
            results = results
        )
    }

    suspend fun findAll(
        scope: RecordScope = RecordScope.All
    ): List<TreatmentDto> =
        repository.findAll(scope)

    suspend fun findByAnimalId(
        animalId: String,
        scope: RecordScope = RecordScope.All
    ): List<TreatmentDto> =
        repository.findByAnimalId(animalId, scope)
}
