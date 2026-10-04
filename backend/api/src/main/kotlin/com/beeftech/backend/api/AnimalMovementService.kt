package com.beeftech.backend.api

class AnimalMovementService(
    private val repository:
    AnimalMovementRepository
) {

    fun sync(
        request: AnimalMovementSyncRequest,
        submittedBy: String? = null,
        siteId: String? = null
    ): AnimalMovementSyncResponse {

        val results =
            request.records.map { record ->

                try {

                    val normalizedRecord =
                        if (
                            record.deviceId.isBlank()
                        ) {
                            record.copy(
                                deviceId =
                                    request.deviceId
                            )
                        } else {
                            record
                        }

                    val syncedAt =
                        repository.upsert(
                            normalizedRecord,
                            submittedBy,
                            siteId
                        )

                    AnimalMovementSyncResult(
                        recordguid =
                            record.recordguid,
                        animalId =
                            record.animalId,
                        status =
                            "SYNCED",
                        serverSyncedAt =
                            syncedAt,
                        message =
                            null
                    )

                } catch (
                    exception: Exception
                ) {

                    AnimalMovementSyncResult(
                        recordguid =
                            record.recordguid,
                        animalId =
                            record.animalId,
                        status =
                            "ERROR",
                        serverSyncedAt =
                            null,
                        message =
                            exception.message
                                ?: "Unable to sync movement."
                    )
                }
            }

        return AnimalMovementSyncResponse(
            results = results
        )
    }

    suspend fun list(scope: RecordScope): List<AnimalMovementDto> =
        repository.list(scope)

    suspend fun listForAnimal(animalId: String, scope: RecordScope): List<AnimalMovementDto> =
        repository.findByAnimalId(animalId, scope)
}
