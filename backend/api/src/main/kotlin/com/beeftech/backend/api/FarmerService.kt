package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal

class FarmerService(
    private val repository: FarmerRepository,
    private val salesNotificationService: FarmerSalesNotificationService
) {

    fun syncRecords(
        request: FarmerSyncRequest,
        principal: AuthPrincipal
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

                    /*
                     * Farmer persistence has already succeeded.
                     *
                     * Notification failure must therefore never
                     * change the farmer back to FAILED/PENDING.
                     */
                    try {
                        salesNotificationService
                            .notifyRegistration(
                                FarmerSalesNotificationPayload(
                                    farmerId =
                                        farmer.farmerId,

                                    clientCode =
                                        farmer.clientCode,

                                    organisationName =
                                        farmer.organisationName,

                                    emailAddress =
                                        farmer.emailAddress,

                                    vatNumber =
                                        farmer.vatNumber,

                                    gpsLatitude =
                                        farmer.gpsLatitude,

                                    gpsLongitude =
                                        farmer.gpsLongitude,

                                    addresses =
                                        farmer.addresses,

                                    roles =
                                        farmer.roles,

                                    deviceId =
                                        request.deviceId,

                                    submittedByUserId =
                                        principal.userId,

                                    submittedByUsername =
                                        principal.username,

                                    submittedByRole =
                                        principal.role,

                                    registrationStatus =
                                        "REGISTERED",

                                    serverSyncedAt =
                                        serverSyncedAt
                                )
                            )
                    } catch (notificationException: Exception) {

                        System.err.println(
                            "Farmer ${farmer.farmerId} synchronized, " +
                                "but sales notification failed: " +
                                (
                                    notificationException.message
                                        ?: "Unknown notification error"
                                )
                        )
                    }

                    FarmerSyncResult(
                        farmerId = farmer.farmerId,
                        status = "SYNCED",
                        serverSyncedAt = serverSyncedAt,
                        message =
                            "Farmer registration synchronized successfully."
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