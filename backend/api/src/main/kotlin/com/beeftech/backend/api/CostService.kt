package com.beeftech.backend.api

class CostService(
    private val repository: CostRepository
) {

    /* One bad record reports ERROR for itself and does not stop the rest of the batch. */
    suspend fun sync(
        request: CostSyncRequest,
        submittedBy: String?,
        siteId: String?
    ): CostSyncResponse {

        val results = request.records.map { record ->

            try {

                val normalized =
                    if (record.deviceId.isBlank()) record.copy(deviceId = request.deviceId) else record

                val syncedAt = repository.upsert(normalized, submittedBy, siteId)

                CostSyncResult(
                    recordguid = record.recordguid,
                    animalId = record.animalId,
                    status = "SYNCED",
                    serverSyncedAt = syncedAt
                )

            } catch (exception: Exception) {

                CostSyncResult(
                    recordguid = record.recordguid,
                    animalId = record.animalId,
                    status = "ERROR",
                    message = exception.message ?: "Unable to sync cost."
                )
            }
        }

        return CostSyncResponse(results = results)
    }

    suspend fun list(scope: RecordScope): List<CostDto> =
        repository.list(scope)

    suspend fun listForAnimal(animalId: String, scope: RecordScope): List<CostDto> =
        repository.findByAnimalId(animalId, scope)
}
