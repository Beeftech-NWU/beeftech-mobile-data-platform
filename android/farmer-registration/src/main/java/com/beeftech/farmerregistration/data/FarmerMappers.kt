package com.beeftech.farmerregistration.data

import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerRoleEntity

fun FarmerEntity.toDto(
    addresses: List<FarmerAddressEntity> = emptyList(),
    roles: List<FarmerRoleEntity> = emptyList()
): FarmerPayload {
    return FarmerPayload(
        farmerId = farmer_id,
        clientCode = client_code,
        organisationName = organisation_name,
        vatNumber = vat_number,
        emailAddress = email_address,
        gpsLatitude = gps_latitude,
        gpsLongitude = gps_longitude,
        syncStatus = sync_status,
        addresses = addresses.map { it.toDto() },
        roles = roles.map { it.toDto() }
    )
}

fun FarmerAddressEntity.toDto(): FarmerAddressPayload {
    return FarmerAddressPayload(
        addressId = address_id,
        farmerId = farmer_id,
        addressType = address_type,
        addressLine1 = address_line_1,
        province = province,
        postalCode = postal_code,
        gpsLatitude = gps_latitude,
        gpsLongitude = gps_longitude
    )
}

fun FarmerRoleEntity.toDto(): FarmerRolePayload {
    return FarmerRolePayload(
        farmerRoleId = farmer_role_id,
        farmerId = farmer_id,
        roleId = role_id.toString()
    )
}
