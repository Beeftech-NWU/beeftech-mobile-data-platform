package com.beeftech.backend.api

import org.jetbrains.exposed.sql.ResultRow
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
                            row[FarmerAddressTable.gpsLongitude]
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

            addresses =
                addresses,

            roles =
                roles
        )
    }

    fun findAll(): List<FarmerDto> =
        transaction {

            FarmerTable
                .selectAll()
                .map {
                    it.toFarmerDto()
                }
        }

    fun findById(
        farmerId: String
    ): FarmerDto? =
        transaction {

            FarmerTable
                .selectAll()
                .where {
                    FarmerTable.farmerId eq farmerId
                }
                .singleOrNull()
                ?.toFarmerDto()
        }

    fun save(
        dto: FarmerDto,
        serverSyncedAt: Long
    ) {
        transaction {

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