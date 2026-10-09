package com.beeftech.backend.api

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class FarmerRepository {

    private fun ResultRow.toFarmerDto(): FarmerDto {

        val farmerIdValue =
            this[FarmerTable.farmerId]

        val addresses =
            FarmerAddressTable
                .selectAll()
                .where {
                    FarmerAddressTable.farmerId eq farmerIdValue
                }
                .map { row ->

                    FarmerAddressDto(
                        addressId =
                            row[FarmerAddressTable.addressId],

                        farmerId =
                            row[FarmerAddressTable.farmerId],

                        addressType =
                            row[FarmerAddressTable.addressType],

                        addressLine1 =
                            row[FarmerAddressTable.addressLine1],

                        province =
                            row[FarmerAddressTable.province],

                        postalCode =
                            row[FarmerAddressTable.postalCode],

                        gpsLatitude =
                            row[FarmerAddressTable.gpsLatitude],

                        gpsLongitude =
                            row[FarmerAddressTable.gpsLongitude],

                        streetCode =
                            row[FarmerAddressTable.streetCode],

                        postalAddress =
                            row[FarmerAddressTable.postalAddress],

                        country =
                            row[FarmerAddressTable.country]
                    )
                }

        val roles =
            FarmerRoleTable
                .selectAll()
                .where {
                    FarmerRoleTable.farmerId eq farmerIdValue
                }
                .map { row ->

                    FarmerRoleDto(
                        farmerRoleId =
                            row[FarmerRoleTable.farmerRoleId],

                        farmerId =
                            row[FarmerRoleTable.farmerId],

                        roleId =
                            row[FarmerRoleTable.roleId]
                    )
                }

        return FarmerDto(
            farmerId =
                farmerIdValue,

            clientCode =
                this[FarmerTable.clientCode],

            organisationName =
                this[FarmerTable.organisationName],

            vatNumber =
                this[FarmerTable.vatNumber],

            emailAddress =
                this[FarmerTable.emailAddress],

            gpsLatitude =
                this[FarmerTable.gpsLatitude],

            gpsLongitude =
                this[FarmerTable.gpsLongitude],

            syncStatus =
                this[FarmerTable.syncStatus],

            coRegIdNo =
                this[FarmerTable.coRegIdNo],

            landOwnership =
                this[FarmerTable.landOwnership],

            faCodeRmis =
                this[FarmerTable.faCodeRmis],

            glnNumber =
                this[FarmerTable.glnNumber],

            herdCapacity =
                this[FarmerTable.herdCapacity],

            interestStatus =
                this[FarmerTable.interestStatus],

            contactName =
                this[FarmerTable.contactName],

            contactNumber =
                this[FarmerTable.contactNumber],

            farmSizeHa =
                this[FarmerTable.farmSizeHa],

            headCount =
                this[FarmerTable.headCount],

            primaryBreed =
                this[FarmerTable.primaryBreed],

            addresses =
                addresses,

            roles =
                roles
        )
    }

    fun findAll(
        scope: RecordScope = RecordScope.All
    ): List<FarmerDto> =
        transaction(DatabaseFactory.getDatabase()) {

            FarmerTable
                .selectAll()
                .where {
                    scope.predicate(
                        FarmerTable.submittedByUserId,
                        FarmerTable.siteId,
                        FarmerTable.voidedAt
                    )
                }
                .map {
                    it.toFarmerDto()
                }
        }

    fun findById(
        farmerId: String,
        scope: RecordScope = RecordScope.All
    ): FarmerDto? =
        transaction(DatabaseFactory.getDatabase()) {

            FarmerTable
                .selectAll()
                .where {
                    (FarmerTable.farmerId eq farmerId) and
                        scope.predicate(
                            FarmerTable.submittedByUserId,
                            FarmerTable.siteId,
                            FarmerTable.voidedAt
                        )
                }
                .singleOrNull()
                ?.toFarmerDto()
        }

    fun save(
        dto: FarmerDto,
        serverSyncedAt: Long,
        submittedBy: String? = null,
        submitterSiteId: String? = null
    ) {
        transaction(DatabaseFactory.getDatabase()) {

            val exists =
                FarmerTable
                    .selectAll()
                    .where {
                        FarmerTable.farmerId eq dto.farmerId
                    }
                    .any()

            if (exists) {

                FarmerTable.update(
                    {
                        FarmerTable.farmerId eq dto.farmerId
                    }
                ) {

                    it[clientCode] =
                        dto.clientCode

                    it[organisationName] =
                        dto.organisationName

                    it[vatNumber] =
                        dto.vatNumber

                    it[emailAddress] =
                        dto.emailAddress

                    it[gpsLatitude] =
                        dto.gpsLatitude

                    it[gpsLongitude] =
                        dto.gpsLongitude

                    it[syncStatus] =
                        "SYNCED"

                    it[syncedAt] =
                        serverSyncedAt

                    it[submittedByUserId] =
                        submittedBy

                    it[siteId] =
                        submitterSiteId

                    it[coRegIdNo] =
                        dto.coRegIdNo

                    it[landOwnership] =
                        dto.landOwnership

                    it[faCodeRmis] =
                        dto.faCodeRmis

                    it[glnNumber] =
                        dto.glnNumber

                    it[herdCapacity] =
                        dto.herdCapacity

                    it[interestStatus] =
                        dto.interestStatus

                    it[contactName] =
                        dto.contactName

                    it[contactNumber] =
                        dto.contactNumber

                    it[farmSizeHa] =
                        dto.farmSizeHa

                    it[headCount] =
                        dto.headCount

                    it[primaryBreed] =
                        dto.primaryBreed
                }

            } else {

                FarmerTable.insert {

                    it[farmerId] =
                        dto.farmerId

                    it[clientCode] =
                        dto.clientCode

                    it[organisationName] =
                        dto.organisationName

                    it[vatNumber] =
                        dto.vatNumber

                    it[emailAddress] =
                        dto.emailAddress

                    it[gpsLatitude] =
                        dto.gpsLatitude

                    it[gpsLongitude] =
                        dto.gpsLongitude

                    it[syncStatus] =
                        "SYNCED"

                    it[syncedAt] =
                        serverSyncedAt

                    it[submittedByUserId] =
                        submittedBy

                    it[siteId] =
                        submitterSiteId

                    it[coRegIdNo] =
                        dto.coRegIdNo

                    it[landOwnership] =
                        dto.landOwnership

                    it[faCodeRmis] =
                        dto.faCodeRmis

                    it[glnNumber] =
                        dto.glnNumber

                    it[herdCapacity] =
                        dto.herdCapacity

                    it[interestStatus] =
                        dto.interestStatus

                    it[contactName] =
                        dto.contactName

                    it[contactNumber] =
                        dto.contactNumber

                    it[farmSizeHa] =
                        dto.farmSizeHa

                    it[headCount] =
                        dto.headCount

                    it[primaryBreed] =
                        dto.primaryBreed
                }
            }

            /*
             * Replace child records so a retry of the same
             * registration cannot create duplicate addresses
             * or roles.
             */
            FarmerAddressTable.deleteWhere {
                FarmerAddressTable.farmerId eq dto.farmerId
            }

            FarmerRoleTable.deleteWhere {
                FarmerRoleTable.farmerId eq dto.farmerId
            }

            dto.addresses.forEach { address ->

                FarmerAddressTable.insert {

                    it[addressId] =
                        address.addressId

                    it[farmerId] =
                        dto.farmerId

                    it[addressType] =
                        address.addressType

                    it[addressLine1] =
                        address.addressLine1

                    it[province] =
                        address.province

                    it[postalCode] =
                        address.postalCode

                    it[gpsLatitude] =
                        address.gpsLatitude

                    it[gpsLongitude] =
                        address.gpsLongitude

                    it[streetCode] =
                        address.streetCode

                    it[postalAddress] =
                        address.postalAddress

                    it[country] =
                        address.country
                }
            }

            dto.roles.forEach { role ->

                FarmerRoleTable.insert {

                    it[farmerRoleId] =
                        role.farmerRoleId

                    it[farmerId] =
                        dto.farmerId

                    it[roleId] =
                        role.roleId
                }
            }
        }
    }
}