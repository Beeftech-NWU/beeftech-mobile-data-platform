package com.beeftech.database.repository

import com.beeftech.database.dao.FarmerDao
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerRoleEntity

class FarmerRepository(
    private val farmerDao: FarmerDao
) {

    suspend fun addFarmer(
        farmer: FarmerEntity
    ) {
        farmerDao.insertFarmer(farmer)
    }

    suspend fun addAddress(
        address: FarmerAddressEntity
    ) {
        farmerDao.insertFarmerAddress(address)
    }

    suspend fun addRole(
        role: FarmerRoleEntity
    ) {
        farmerDao.insertFarmerRole(role)
    }

    suspend fun getFarmer(
        farmerId: String
    ): FarmerEntity? {
        return farmerDao.getFarmerById(
            farmerId
        )
    }

    suspend fun getAllFarmers():
            List<FarmerEntity> {

        return farmerDao.getAllFarmers()
    }

    suspend fun getPendingFarmers():
            List<FarmerEntity> {

        return farmerDao.getPendingFarmers()
    }

    suspend fun getAddressesForFarmer(
        farmerId: String
    ): List<FarmerAddressEntity> {

        return farmerDao.getAddressesForFarmer(
            farmerId
        )
    }

    suspend fun getRolesForFarmer(
        farmerId: String
    ): List<FarmerRoleEntity> {

        return farmerDao.getRolesForFarmer(
            farmerId
        )
    }

    suspend fun markAsSynced(
        farmerId: String
    ) {
        farmerDao.updateSyncStatus(
            farmerId = farmerId,
            status = "SYNCED"
        )
    }

    suspend fun markAsPending(
        farmerId: String
    ) {
        farmerDao.updateSyncStatus(
            farmerId = farmerId,
            status = "PENDING"
        )
    }
}