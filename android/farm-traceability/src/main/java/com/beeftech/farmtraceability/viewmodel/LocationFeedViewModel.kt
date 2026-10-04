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
import java.util.Locale
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

        val normalizedAnimalId =
            animalId.trim()


        if (
            normalizedAnimalId.isBlank()
        ) {

            _records.value =
                emptyList()

            return
        }


        viewModelScope.launch {

            try {

                _records.value =
                    getVisibleRecords(
                        normalizedAnimalId
                    )

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

        val normalizedAnimalId =
            animalId.trim()

        val normalizedDestination =
            destination.trim()

        val normalizedRation =
            rationName.trim()

        val normalizedDays =
            daysInDestinationText.trim()

        val normalizedRationDays =
            rationDaysText.trim()

        val normalizedCost =
            rationCostText.trim()


        if (
            normalizedAnimalId.isBlank()
        ) {

            onResult(
                false,
                "Please select an animal first."
            )

            return
        }


        if (
            normalizedDestination.isBlank()
        ) {

            onResult(
                false,
                "Please select or enter the destination."
            )

            return
        }


        val daysInDestination =
            normalizedDays
                .toIntOrNull()


        if (
            daysInDestination == null ||
            daysInDestination <= 0
        ) {

            onResult(
                false,
                "Please enter the number of days at this location."
            )

            return
        }


        if (
            normalizedRation.isBlank()
        ) {

            onResult(
                false,
                "Please enter or select the ration."
            )

            return
        }


        val rationDays =
            normalizedRationDays
                .toIntOrNull()


        if (
            rationDays == null ||
            rationDays <= 0
        ) {

            onResult(
                false,
                "Please enter the number of ration days."
            )

            return
        }


        if (
            rationDays >
            daysInDestination
        ) {

            onResult(
                false,
                "Ration days cannot exceed days at the location."
            )

            return
        }


        /*
         * Previously a blank Cost automatically became 0.0.
         *
         * That is removed.
         *
         * If the real cost is zero, the user must deliberately type 0.
         */
        if (
            normalizedCost.isBlank()
        ) {

            onResult(
                false,
                "Please enter the ration cost. Enter 0 only if the cost is genuinely zero."
            )

            return
        }


        val rationCost =
            parseMoney(
                normalizedCost
            )


        if (
            rationCost == null ||
            rationCost < 0.0
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


                val existingFeedRecords =
                    animalMovementDao
                        .getByAnimalId(
                            normalizedAnimalId
                        )
                        .filter {
                            !it.feedLocationType
                                .isNullOrBlank()
                        }
                        .sortedBy {
                            it.movementDate
                        }


                /*
                 * Only the latest VALID feed state is relevant for
                 * duplicate prevention.
                 *
                 * Same pen but different ration is valid.
                 */
                val latestValidRecord =
                    existingFeedRecords
                        .lastOrNull {
                            parseStoredDetails(
                                it.notes
                            ) != null
                        }


                val requestedState =
                    feedStateKey(
                        destination =
                            normalizedDestination,

                        ration =
                            normalizedRation,

                        days =
                            daysInDestination,

                        rationDays =
                            rationDays,

                        cost =
                            rationCost
                    )


                val currentState =
                    latestValidRecord
                        ?.let {
                                record ->

                            val details =
                                parseStoredDetails(
                                    record.notes
                                )
                                    ?: return@let null


                            feedStateKey(
                                destination =
                                    record.destinationFarmId,

                                ration =
                                    record.feedLocationType
                                        .orEmpty(),

                                days =
                                    details.days,

                                rationDays =
                                    details.rationDays,

                                cost =
                                    details.cost
                            )
                        }


                /*
                 * Prevent Save being pressed repeatedly with exactly
                 * the same current location/feed information.
                 */
                if (
                    currentState != null &&
                    currentState ==
                    requestedState
                ) {

                    _records.value =
                        getVisibleRecords(
                            normalizedAnimalId
                        )


                    onResult(
                        false,
                        "This location and feed record is already current."
                    )

                    return@launch
                }


                val now =
                    System.currentTimeMillis()


                val record =
                    AnimalMovementEntity(
                        animalId =
                            normalizedAnimalId,

                        destinationFarmId =
                            normalizedDestination,

                        destinationPenId =
                            "",

                        movementDate =
                            now,

                        feedLocationType =
                            normalizedRation,

                        notes =
                            "Days: $daysInDestination, " +
                                "Ration days: $rationDays, " +
                                "Cost: $rationCost",

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
                 * Cost is an EXPLICIT total cost entered for this
                 * Location & Feed record.
                 *
                 * It is not automatically calculated from duration
                 * because there is currently no stored cost-per-day
                 * or cost-per-feed-unit rate.
                 */
                if (
                    rationCost >
                    0.0
                ) {

                    val feedCost =
                        AnimalCost(
                            animalId =
                                normalizedAnimalId,

                            costType =
                                CostSummaryViewModel
                                    .COST_FEED,

                            amount =
                                rationCost,

                            description =
                                normalizedRation,

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
                    getVisibleRecords(
                        normalizedAnimalId
                    )


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


    /*
     * Old versions allowed:
     *
     * Days:
     * Ration days:
     * Cost: 0.0
     *
     * Those malformed rows are hidden from the user.
     *
     * Exact consecutive duplicates are also collapsed.
     */
    private suspend fun getVisibleRecords(
        animalId: String
    ): List<AnimalMovementEntity> {

        val rawRecords =
            animalMovementDao
                .getByAnimalId(
                    animalId
                )
                .filter {
                    !it.feedLocationType
                        .isNullOrBlank()
                }
                .sortedBy {
                    it.movementDate
                }


        val cleaned =
            mutableListOf<
                AnimalMovementEntity
            >()


        rawRecords.forEach {
                record ->

            val details =
                parseStoredDetails(
                    record.notes
                )


            /*
             * Do not display malformed historical entries.
             */
            if (
                details != null &&
                record.destinationFarmId
                    .isNotBlank() &&
                !record.feedLocationType
                    .isNullOrBlank()
            ) {

                val key =
                    feedStateKey(
                        destination =
                            record.destinationFarmId,

                        ration =
                            record.feedLocationType
                                .orEmpty(),

                        days =
                            details.days,

                        rationDays =
                            details.rationDays,

                        cost =
                            details.cost
                    )


                val previous =
                    cleaned
                        .lastOrNull()


                val previousDetails =
                    previous
                        ?.let {
                            parseStoredDetails(
                                it.notes
                            )
                        }


                val previousKey =
                    if (
                        previous != null &&
                        previousDetails != null
                    ) {

                        feedStateKey(
                            destination =
                                previous.destinationFarmId,

                            ration =
                                previous.feedLocationType
                                    .orEmpty(),

                            days =
                                previousDetails.days,

                            rationDays =
                                previousDetails.rationDays,

                            cost =
                                previousDetails.cost
                        )

                    } else {

                        null
                    }


                if (
                    key !=
                    previousKey
                ) {

                    cleaned +=
                        record
                }
            }
        }


        return cleaned
            .sortedByDescending {
                it.movementDate
            }
    }


    private fun feedStateKey(
        destination: String,
        ration: String,
        days: Int,
        rationDays: Int,
        cost: Double
    ): String {

        return listOf(
            normalize(
                destination
            ),

            normalize(
                ration
            ),

            days.toString(),

            rationDays.toString(),

            String.format(
                Locale.US,
                "%.2f",
                cost
            )
        )
            .joinToString(
                separator = "|"
            )
    }


    private fun normalize(
        value: String
    ): String {

        return value
            .trim()
            .lowercase(
                Locale.ROOT
            )
            .replace(
                Regex("\\s+"),
                " "
            )
    }


    private fun parseStoredDetails(
        notes: String?
    ): FeedDetails? {

        val source =
            notes
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null


        val match =
            Regex(
                pattern =
                    """(?i)Days:\s*(\d+)\s*,\s*Ration days:\s*(\d+)\s*,\s*Cost:\s*(-?\d+(?:\.\d+)?)"""
            )
                .find(
                    source
                )
                ?: return null


        val days =
            match
                .groupValues
                .getOrNull(1)
                ?.toIntOrNull()
                ?: return null


        val rationDays =
            match
                .groupValues
                .getOrNull(2)
                ?.toIntOrNull()
                ?: return null


        val cost =
            match
                .groupValues
                .getOrNull(3)
                ?.toDoubleOrNull()
                ?: return null


        if (
            days <= 0 ||
            rationDays <= 0 ||
            rationDays > days ||
            cost < 0.0
        ) {

            return null
        }


        return FeedDetails(
            days =
                days,

            rationDays =
                rationDays,

            cost =
                cost
        )
    }


    private fun parseMoney(
        rawValue: String
    ): Double? {

        /*
         * IMPORTANT:
         *
         * Blank is invalid.
         * It must NOT silently become zero.
         */
        if (
            rawValue.isBlank()
        ) {

            return null
        }


        return rawValue
            .replace(
                "R",
                "",
                ignoreCase =
                    true
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


    private data class FeedDetails(
        val days: Int,
        val rationDays: Int,
        val cost: Double
    )
}
