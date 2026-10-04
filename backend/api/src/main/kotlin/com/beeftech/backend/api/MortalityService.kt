package com.beeftech.backend.api

class MortalityService(
    private val repository: MortalityRepository
) {

    /* One bad record reports ERROR for itself and does not stop the rest of the batch. */
    suspend fun sync(
        request: MortalitySyncRequest,
        submittedBy: String?,
        siteId: String?
    ): MortalitySyncResponse {

        val results = request.records.map { record ->

            try {

                val normalized =
                    if (record.deviceId.isBlank()) record.copy(deviceId = request.deviceId) else record

                val syncedAt = repository.upsert(normalized, submittedBy, siteId)

                MortalitySyncResult(
                    recordguid = record.recordguid,
                    animalId = record.animalId,
                    status = "SYNCED",
                    serverSyncedAt = syncedAt
                )

            } catch (exception: Exception) {

                MortalitySyncResult(
                    recordguid = record.recordguid,
                    animalId = record.animalId,
                    status = "ERROR",
                    message = exception.message ?: "Unable to sync mortality."
                )
            }
        }

        return MortalitySyncResponse(results = results)
    }

    suspend fun list(scope: RecordScope): List<MortalityDto> =
        repository.list(scope)

    suspend fun listForAnimal(animalId: String, scope: RecordScope): List<MortalityDto> =
        repository.findByAnimalId(animalId, scope)
}
