package com.beeftech.database.repository

import com.beeftech.database.dao.FarmerDao
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerAddressEntity

class FarmerRepository(private val farmerDao: FarmerDao) {

    suspend fun addFarmer(farmer: FarmerEntity) {
        farmerDao.insertFarmer(farmer)
    }

    suspend fun addAddress(address: FarmerAddressEntity) {
        farmerDao.insertFarmerAddress(address)
    }

    suspend fun getFarmer(farmerId: String): FarmerEntity? {
        return farmerDao.getFarmerById(farmerId)
    }

    suspend fun getAllFarmers(): List<FarmerEntity> {
        return farmerDao.getAllFarmers()
    }
}