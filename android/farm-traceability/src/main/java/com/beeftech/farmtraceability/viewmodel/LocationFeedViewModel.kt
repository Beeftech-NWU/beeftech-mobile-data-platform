package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.farmtraceability.worker.TraceabilityOutboxWorker
import com.beeftech.farmtraceability.worker.TraceabilitySyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationFeedViewModel(
    private val animalMovementDao:
            AnimalMovementDao
) : ViewModel() {

    private val _records =
        MutableStateFlow<
            List<AnimalMovementEntity>
            >(
            emptyList()
        )

    val records:
            StateFlow<
                List<AnimalMovementEntity>
                > =
        _records.asStateFlow()


    fun loadRecords(
        animalId: String
    ) {

        if (
            animalId.isBlank()
        ) {

            _records.value =
                emptyList()

            return
        }

        viewModelScope.launch {

            try {

                _records.value =
                    animalMovementDao
                        .getByAnimalId(
                            animalId
                        )
                        .filter {
                            !it.feedLocationType
                                .isNullOrBlank()
                        }

            } catch (
                _: Exception
            ) {

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

        if (
            animalId.isBlank()
        ) {

            onResult(
                false,
                "Please select an animal first."
            )

            return
        }

        if (
            destination.isBlank()
        ) {

            onResult(
                false,
                "Please enter the destination."
            )

            return
        }

        if (
            rationName.isBlank()
        ) {

            onResult(
                false,
                "Please enter or select the ration."
            )

            return
        }


        val rationCost =
            parseMoney(
                rationCostText
            )


        if (
            rationCostText.isNotBlank() &&
            rationCost == null
        ) {

            onResult(
                false,
                "Please enter a valid ration cost."
            )

            return
        }


        viewModelScope.launch {

            try {

                val database =
                    DatabaseProvider
                        .getDatabase()
                        ?: throw IllegalStateException(
                            "The encrypted database is not available."
                        )


                val now =
                    System.currentTimeMillis()


                val record =
                    AnimalMovementEntity(
                        animalId =
                            animalId.trim(),

                        destinationFarmId =
                            destination.trim(),

                        destinationPenId =
                            "",

                        movementDate =
                            now,

                        feedLocationType =
                            rationName.trim(),

                        notes =
                            "Days: ${daysInDestinationText.trim()}, " +
                                "Ration days: ${rationDaysText.trim()}, " +
                                "Cost: ${rationCost ?: 0.0}",

                        capturedAt =
                            now,

                        syncStatus =
                            "PENDING"
                    )


                animalMovementDao
                    .insert(
                        record
                    )


                val pendingRepository =
                    PendingSyncRepository(
                        database.pendingSyncDao()
                    )


                pendingRepository
                    .queueOperation(
                        entityType =
                            TraceabilityOutboxWorker
                                .ENTITY_LOCATION_FEED,

                        entityId =
                            record.recordGuid,

                        operation =
                            "UPSERT",

                        payload =
                            record.recordGuid
                    )


                /*
                 * Cost Summary now receives FEED cost automatically.
                 */
                if (
                    rationCost != null &&
                    rationCost > 0.0
                ) {

                    val feedCost =
                        AnimalCost(
                            animalId =
                                animalId.trim(),

                            costType =
                                CostSummaryViewModel
                                    .COST_FEED,

                            amount =
                                rationCost,

                            description =
                                rationName.trim(),

                            gpsLat =
                                record.gpsLat,

                            gpsLng =
                                record.gpsLng,

                            timestamp =
                                now,

                            sourceEntity =
                                TraceabilityOutboxWorker
                                    .ENTITY_LOCATION_FEED,

                            sourceRecordId =
                                record.recordGuid
                        )


                    database
                        .animalCostDao()
                        .insert(
                            feedCost
                        )


                    pendingRepository
                        .queueOperation(
                            entityType =
                                TraceabilityOutboxWorker
                                    .ENTITY_ANIMAL_COST,

                            entityId =
                                feedCost.recordGuid,

                            operation =
                                "UPSERT",

                            payload =
                                feedCost.recordGuid
                        )
                }


                _records.value =
                    animalMovementDao
                        .getByAnimalId(
                            animalId
                        )
                        .filter {
                            !it.feedLocationType
                                .isNullOrBlank()
                        }


                TraceabilitySyncScheduler
                    .kick()


                onResult(
                    true,
                    "Location and feed saved offline and queued for sync."
                )

            } catch (
                exception: Exception
            ) {

                onResult(
                    false,
                    exception.message
                        ?: "Unable to save location and feed record."
                )
            }
        }
    }


    private fun parseMoney(
        rawValue: String
    ): Double? {

        if (
            rawValue.isBlank()
        ) {
            return 0.0
        }

        return rawValue
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
            .toDoubleOrNull()
    }
}
