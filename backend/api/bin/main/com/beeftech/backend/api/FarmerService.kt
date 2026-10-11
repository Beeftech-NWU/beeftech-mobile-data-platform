package com.beeftech.backend.api

class FarmerService(
    private val repository: FarmerRepository
) {

    fun syncRecords(
        request: FarmerSyncRequest
    ): FarmerSyncResponse {

        val results =
            request.records.map { farmer ->

                try {

                    val serverSyncedAt =
                        System.currentTimeMillis()

                    repository.save(
                        dto = farmer,
                        serverSyncedAt = serverSyncedAt
                    )

                    FarmerSyncResult(
                        farmerId = farmer.farmerId,
                        status = "SYNCED",
                        serverSyncedAt = serverSyncedAt,
                        message = "Farmer registration synchronized successfully."
                    )

                } catch (exception: Exception) {

                    FarmerSyncResult(
                        farmerId = farmer.farmerId,
                        status = "FAILED",
                        serverSyncedAt = null,
                        message =
                            exception.message
                                ?: "Farmer registration synchronization failed."
                    )
                }
            }

        return FarmerSyncResponse(
            results = results
        )
    }

    fun findAll(): List<FarmerDto> =
        repository.findAll()

    fun findById(
        farmerId: String
    ): FarmerDto? =
        repository.findById(
            farmerId
        )
}