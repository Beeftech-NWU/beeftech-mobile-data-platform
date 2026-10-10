package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.farmtraceability.data.AnimalMovementRepository
import com.beeftech.farmtraceability.worker.TraceabilitySyncScheduler
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnimalMovementViewModel(
    private val animalMovementDao:
        AnimalMovementDao
) : ViewModel() {

    private val _movements =
        MutableStateFlow<
            List<AnimalMovementEntity>
        >(
            emptyList()
        )

    val movements:
            StateFlow<
                List<AnimalMovementEntity>
            > =
        _movements.asStateFlow()


    fun loadMovements(
        animalId: String
    ) {

        val normalizedAnimalId =
            animalId.trim()


        if (
            normalizedAnimalId.isBlank()
        ) {

            _movements.value =
                emptyList()

            return
        }


        viewModelScope.launch {

            try {

                _movements.value =
                    getVisibleMovements(
                        normalizedAnimalId
                    )

            } catch (
                _: Exception
            ) {

                _movements.value =
                    emptyList()
            }
        }
    }


    fun saveMovement(
        animalId: String,
        movementInformation: String,
        responsibleWorker: String,
        movementDate: Long =
            System.currentTimeMillis(),
        onResult:
            (
                Boolean,
                String
            ) -> Unit =
            { _, _ -> }
    ) {

        val normalizedAnimalId =
            animalId.trim()

        val normalizedMovement =
            movementInformation.trim()

        val normalizedWorker =
            responsibleWorker.trim()


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
            normalizedMovement.isBlank()
        ) {

            onResult(
                false,
                "Please enter movement information."
            )

            return
        }


        if (
            normalizedWorker.isBlank()
        ) {

            onResult(
                false,
                "Please select the responsible worker."
            )

            return
        }


        viewModelScope.launch {

            try {

                /*
                 * Find the animal's latest REAL movement.
                 *
                 * Location & Feed rows deliberately share the
                 * animal_movements table, so exclude them here.
                 */
                val latestMovement =
                    animalMovementDao
                        .getByAnimalId(
                            normalizedAnimalId
                        )
                        .filter {
                            it.feedLocationType
                                .isNullOrBlank()
                        }
                        .maxByOrNull {
                            it.movementDate
                        }


                val requestedDestination =
                    destinationKey(
                        normalizedMovement
                    )


                val currentDestination =
                    latestMovement
                        ?.destinationFarmId
                        ?.let {
                            destinationKey(
                                it
                            )
                        }


                /*
                 * No time window.
                 *
                 * If the latest movement already leaves the animal at
                 * this destination, another movement there is invalid.
                 */
                if (
                    latestMovement != null &&
                    requestedDestination.isNotBlank() &&
                    requestedDestination ==
                    currentDestination
                ) {

                    _movements.value =
                        getVisibleMovements(
                            normalizedAnimalId
                        )


                    onResult(
                        false,
                        "Animal is already recorded at this destination."
                    )

                    return@launch
                }


                val effectiveMovementDate =
                    movementDate
                        .takeIf {
                            it > 0L
                        }
                        ?: System.currentTimeMillis()


                val movement =
                    AnimalMovementEntity(
                        animalId =
                            normalizedAnimalId,

                        destinationFarmId =
                            normalizedMovement,

                        destinationPenId =
                            "",

                        movementDate =
                            effectiveMovementDate,

                        notes =
                            normalizedWorker
                    )


                animalMovementDao
                    .insert(
                        movement
                    )


                val database =
                    DatabaseProvider
                        .getDatabase()
                        ?: throw IllegalStateException(
                            "The encrypted database is not available."
                        )


                PendingSyncRepository(
                    database.pendingSyncDao()
                )
                    .queueOperation(
                        entityType =
                            AnimalMovementRepository
                                .ENTITY_TYPE,

                        entityId =
                            movement.recordGuid,

                        operation =
                            "UPSERT",

                        payload =
                            movement.recordGuid
                    )


                TraceabilitySyncScheduler
                    .kick()


                _movements.value =
                    getVisibleMovements(
                        normalizedAnimalId
                    )


                onResult(
                    true,
                    "Movement record saved successfully."
                )

            } catch (
                exception: Exception
            ) {

                onResult(
                    false,
                    "Failed to save movement: " +
                        (
                            exception.message
                                ?: "Unknown error"
                        )
                )
            }
        }
    }


    /*
     * Display movement history as actual state transitions.
     *
     * Example stored history:
     *
     * 14:49 Pen A
     * 16:20 Pen A
     *
     * becomes:
     *
     * 14:49 Pen A
     *
     * But this remains valid:
     *
     * Pen A
     * Pen B
     * Pen A
     */
    private suspend fun getVisibleMovements(
        animalId: String
    ): List<AnimalMovementEntity> {

        val movementOnly =
            animalMovementDao
                .getByAnimalId(
                    animalId
                )
                .filter {
                    it.feedLocationType
                        .isNullOrBlank()
                }
                .sortedBy {
                    it.movementDate
                }


        val cleaned =
            mutableListOf<
                AnimalMovementEntity
            >()


        movementOnly.forEach {
                movement ->

            val previous =
                cleaned.lastOrNull()


            val isRepeatedDestination =
                previous != null &&
                    destinationKey(
                        previous.destinationFarmId
                    ) ==
                    destinationKey(
                        movement.destinationFarmId
                    )


            if (
                !isRepeatedDestination
            ) {

                cleaned +=
                    movement
            }
        }


        return cleaned
            .sortedByDescending {
                it.movementDate
            }
    }


    /*
     * Existing movement text may contain:
     *
     * "Moved from Khanyisa Livestock Farm to Pen A"
     *
     * while a user may type:
     *
     * "Pen A"
     *
     * Both resolve to "pen a".
     */
    private fun destinationKey(
        rawValue: String
    ): String {

        /*
         * Canonicalise legacy and current Movement text.
         *
         * Examples that must all resolve to "pen a":
         *
         * Pen A
         * Moved from Farm to Pen A
         * Moved from Farm to \nPen A
         * Moved from Farm to n Pen A
         */
        val normalized =
            rawValue
                .lowercase(
                    Locale.ROOT
                )
                .replace(
                    "\\n",
                    " "
                )
                .replace(
                    "\\r",
                    " "
                )
                .replace(
                    "\\t",
                    " "
                )
                .replace(
                    Regex("[\\r\\n\\t]+"),
                    " "
                )
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()


        if (
            normalized.isBlank()
        ) {

            return ""
        }


        /*
         * Pen is the strongest destination identifier currently
         * represented in Farm Traceability.
         *
         * Search the ENTIRE string instead of trusting old free-text
         * formatting around "to".
         */
        val penMatch =
            Regex(
                """\bpen\s*[-#:]?\s*([a-z0-9]+)\b"""
            )
                .findAll(
                    normalized
                )
                .lastOrNull()


        if (
            penMatch != null
        ) {

            return "pen " +
                penMatch
                    .groupValues[1]
                    .trim()
        }


        /*
         * Non-pen destinations retain the normal final "to ..."
         * behaviour.
         */
        val destination =
            if (
                normalized.contains(
                    " to "
                )
            ) {

                normalized
                    .substringAfterLast(
                        " to "
                    )

            } else {

                normalized
            }


        return destination
            .replace(
                Regex(
                    """^(?:\\[nrt]\s*)+"""
                ),
                ""
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    fun retrySync(
        animalId: String,
        onResult:
            (
                Boolean,
                String
            ) -> Unit =
            { _, _ -> }
    ) {

        TraceabilitySyncScheduler
            .kick()


        onResult(
            true,
            "Movement synchronization scheduled."
        )
    }
}
