package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.LocationFeedDao
import com.beeftech.database.entity.LocationFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationFeedViewModel(
    private val locationFeedDao: LocationFeedDao
) : ViewModel() {

    private val _records =
        MutableStateFlow<List<LocationFeed>>(
            emptyList()
        )

    val records: StateFlow<List<LocationFeed>> =
        _records.asStateFlow()

    fun loadRecords(
        animalId: String
    ) {

        if (animalId.isBlank()) {

            _records.value =
                emptyList()

            return
        }

        viewModelScope.launch {

            try {

                _records.value =
                    locationFeedDao
                        .getByAnimalId(
                            animalId
                        )

            } catch (exception: Exception) {

                _records.value =
                    emptyList()
            }
        }
    }

    fun saveRecord(
        animalId: String,
        destination: String,
        daysInDestinationText: String,
        rationName: String,
        rationDaysText: String,
        rationCostText: String,
        onResult: (
            Boolean,
            String
        ) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {

            onResult(
                false,
                "Please select an animal first."
            )

            return
        }

        if (destination.isBlank()) {

            onResult(
                false,
                "Please enter the destination."
            )

            return
        }

        val daysInDestination =
            daysInDestinationText
                .trim()
                .toIntOrNull()

        if (
            daysInDestination == null ||
            daysInDestination < 0
        ) {

            onResult(
                false,
                "Please enter valid days in destination."
            )

            return
        }

        if (rationName.isBlank()) {

            onResult(
                false,
                "Please enter the ration name."
            )

            return
        }

        val rationDays =
            rationDaysText
                .trim()
                .toIntOrNull()

        if (
            rationDays == null ||
            rationDays < 0
        ) {

            onResult(
                false,
                "Please enter valid ration days."
            )

            return
        }

        val cleanedCost =
            rationCostText
                .replace(
                    "R",
                    "",
                    ignoreCase = true
                )
                .replace(
                    " ",
                    ""
                )
                .replace(
                    ",",
                    "."
                )
                .trim()

        val rationCost =
            cleanedCost
                .toDoubleOrNull()

        if (
            rationCost == null ||
            rationCost < 0
        ) {

            onResult(
                false,
                "Please enter a valid ration cost."
            )

            return
        }

        viewModelScope.launch {

            try {

                val record =
                    LocationFeed(
                        animalId =
                            animalId,

                        destination =
                            destination.trim(),

                        daysInDestination =
                            daysInDestination,

                        rationName =
                            rationName.trim(),

                        rationDays =
                            rationDays,

                        rationCost =
                            rationCost,

                        timestamp =
                            System.currentTimeMillis()
                    )

                locationFeedDao.insert(
                    record
                )

                _records.value =
                    locationFeedDao
                        .getByAnimalId(
                            animalId
                        )

                onResult(
                    true,
                    "Location and feed record saved successfully."
                )

            } catch (exception: Exception) {

                onResult(
                    false,
                    "Unable to save location and feed record."
                )
            }
        }
    }
}