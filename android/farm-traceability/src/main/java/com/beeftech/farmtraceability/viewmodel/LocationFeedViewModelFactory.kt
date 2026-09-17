package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.LocationFeedDao

class LocationFeedViewModelFactory(
    private val locationFeedDao: LocationFeedDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                LocationFeedViewModel::class.java
            )
        ) {

            return LocationFeedViewModel(
                locationFeedDao =
                    locationFeedDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}