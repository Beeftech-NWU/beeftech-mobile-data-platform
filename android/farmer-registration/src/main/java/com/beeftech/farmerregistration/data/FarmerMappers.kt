package com.beeftech.farmerregistration.data

import com.beeftech.farmerregistration.data.FarmerEntity
import com.beeftech.farmerregistration.data.FarmerAddressEntity
import com.beeftech.farmerregistration.data.FarmerRoleEntity
import com.beeftech.farmerregistration.data.FarmerDto
import com.beeftech.farmerregistration.data.FarmerAddressDto
import com.beeftech.farmerregistration.data.FarmerRoleDto

fun FarmerEntity.toDto(
    addresses: List<FarmerAddressEntity> = emptyList(),
    roles: List<FarmerRoleEntity> = emptyList()
): FarmerDto {
    return FarmerDto(
        farmerId = this.farmerId,
        clientCode = this.clientCode,
        organisationName = this.organisationName,
        vatNumber = this.vatNumber,
        emailAddress = this.emailAddress,
        gpsLatitude = this.gpsLatitude,
        gpsLongitude = this.gpsLongitude,
        syncStatus = this.syncStatus,
        addresses = addresses.map { it.toDto() },
        roles = roles.map { it.toDto() }
    )
}

fun FarmerAddressEntity.toDto(): FarmerAddressDto {
    return FarmerAddressDto(
        addressId = this.addressId,
        farmerId = this.farmerId,
        addressType = this.addressType,
        addressLine1 = this.addressLine1,
        province = this.province,
        postalCode = this.postalCode,
        gpsLatitude = this.gpsLatitude,
        gpsLongitude = this.gpsLongitude
    )
}

fun FarmerRoleEntity.toDto(): FarmerRoleDto {
    return FarmerRoleDto(
        farmerRoleId = this.farmerRoleId,
        farmerId = this.farmerId,
        roleId = this.roleId
    )
}