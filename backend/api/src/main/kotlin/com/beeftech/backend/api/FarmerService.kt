package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal

class FarmerService(
    private val repository: FarmerRepository,
    private val salesNotificationService: FarmerSalesNotificationService
) {

    suspend fun syncRecords(
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
                        serverSyncedAt = serverSyncedAt,
                        submittedBy = principal.userId,
                        submitterSiteId = principal.siteId
                    )

                    /*
                     * Farmer persistence has already succeeded.
                     *
                     * Notification failure must therefore never
                     * change the farmer back to FAILED/PENDING.
                     */
                    try {
                        notifySalesOnce(
                            farmer = farmer,
                            deviceId = request.deviceId,
                            principal = principal,
                            serverSyncedAt = serverSyncedAt
                        )
                    } catch (notificationException: Exception) {

                        System.err.println(
                            "Farmer ${farmer.farmerId} synchronized, " +
                                "but the sales notification could not be recorded: " +
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

    /*
     * One email per farmer: claim first, so a re-sync, a retry or a concurrent sync of the same
     * farmer sends nothing. A send that fails or sends nothing releases the claim, and the next
     * sync of the farmer tries again.
     */
    private suspend fun notifySalesOnce(
        farmer: FarmerDto,
        deviceId: String,
        principal: AuthPrincipal,
        serverSyncedAt: Long
    ) {
        if (!repository.claimSalesNotification(farmer.farmerId, serverSyncedAt)) return

        val sent =
            try {
                salesNotificationService
                    .notifyRegistration(
                        FarmerSalesNotificationPayload(
                            farmerId = farmer.farmerId,
                            clientCode = farmer.clientCode,
                            organisationName = farmer.organisationName,
                            emailAddress = farmer.emailAddress,
                            vatNumber = farmer.vatNumber,
                            coRegIdNo = farmer.coRegIdNo,
                            landOwnership = farmer.landOwnership,
                            faCodeRmis = farmer.faCodeRmis,
                            glnNumber = farmer.glnNumber,
                            herdCapacity = farmer.herdCapacity,
                            interestStatus = farmer.interestStatus,
                            contactName = farmer.contactName,
                            contactNumber = farmer.contactNumber,
                            farmSizeHa = farmer.farmSizeHa,
                            headCount = farmer.headCount,
                            primaryBreed = farmer.primaryBreed,
                            gpsLatitude = farmer.gpsLatitude,
                            gpsLongitude = farmer.gpsLongitude,
                            addresses = farmer.addresses,
                            roles = farmer.roles,
                            deviceId = deviceId,
                            farmCode = principal.siteId?.let { farmCodeOfSite(it) },
                            assignedSalesmanEmail = principal.siteId?.let { salesRepEmailOfSite(it) },
                            submittedByUserId = principal.userId,
                            submittedByUsername = principal.username,
                            submittedByRole = principal.role,
                            registrationStatus = "REGISTERED",
                            serverSyncedAt = serverSyncedAt
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

                false
            }

        if (!sent) {
            repository.releaseSalesNotification(farmer.farmerId, serverSyncedAt)
        }
    }

    fun findAll(
        scope: RecordScope = RecordScope.All
    ): List<FarmerDto> =
        repository.findAll(scope)

    fun findById(
        farmerId: String,
        scope: RecordScope = RecordScope.All
    ): FarmerDto? =
        repository.findById(
            farmerId,
            scope
        )
}