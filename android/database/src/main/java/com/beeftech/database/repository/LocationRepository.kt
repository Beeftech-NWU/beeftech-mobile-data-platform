package com.beeftech.database.repository

import com.beeftech.database.dao.LocationDao
import com.beeftech.database.entity.LocationEntity

class LocationRepository(private val locationDao: LocationDao) {

    suspend fun addLocation(location: LocationEntity) {
        locationDao.insertLocation(location)
    }

    suspend fun getLocation(locationId: String): LocationEntity? {
        return locationDao.getLocationById(locationId)
    }

    suspend fun getAllLocations(): List<LocationEntity> {
        return locationDao.getAllLocations()
    }
}