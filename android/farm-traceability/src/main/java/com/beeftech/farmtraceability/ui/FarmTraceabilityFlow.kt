package com.beeftech.farmtraceability.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.AnimalMovement
import com.beeftech.database.entity.LocationFeed
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.Supplier
import com.beeftech.database.entity.Treatment
import com.beeftech.farmtraceability.repository.FindAnimalRepository
import com.beeftech.farmtraceability.viewmodel.FindAnimalUiState
import com.beeftech.farmtraceability.viewmodel.FindAnimalViewModel
import com.beeftech.farmtraceability.viewmodel.FindAnimalViewModelFactory

private enum class TraceabilityScreen {
    HOME,
    FARMER_FARM_PROFILE,
    FIND_ANIMAL,
    ANIMAL_RECORD,
    ANIMAL_MOVEMENT,
    SUPPLIER,
    LOCATION_FEED,
    TREATMENTS,
    COST_SUMMARY,
    MORTALITY
}

@Composable
fun FarmTraceabilityFlow(

    // Animal Movement
    movementRecords: List<AnimalMovement> = emptyList(),

    onLoadMovements: (String) -> Unit = {},

    onSaveMovement: (
        animalId: String,
        movementInformation: String,
        responsibleWorker: String
    ) -> Unit = { _, _, _ -> },

    // Treatments
    treatmentRecords: List<Treatment> = emptyList(),

    onLoadTreatments: (String) -> Unit = {},

    onSaveTreatment: (
        animalId: String,
        disease: String,
        treatment: String,
        batchNumber: String,
        volumeUsed: String,
        cost: String
    ) -> Unit = { _, _, _, _, _, _ -> },

    // Mortality
    mortalityRecords: List<Mortality> = emptyList(),

    onLoadMortalities: (String) -> Unit = {},

    onSaveMortality: (
        animalId: String,
        mortalityReason: String,
        responsibleWorker: String
    ) -> Unit = { _, _, _ -> },

    // Cost Summary
    transportCost: Double = 0.0,

    processingCost: Double = 0.0,

    treatmentCost: Double = 0.0,

    feedCost: Double = 0.0,

    handlingCost: Double = 0.0,

    interestCost: Double = 0.0,

    totalAnimalCost: Double = 0.0,

    onLoadCostSummary: (String) -> Unit = {},

    // Supplier
    supplierRecords: List<Supplier> = emptyList(),

    onLoadSuppliers: (String) -> Unit = {},

    onSaveSupplier: (
        animalId: String,
        supplierName: String,
        glnNumber: String,
        purchaseDate: String,
        purchaseBatchNumber: String
    ) -> Unit = { _, _, _, _, _ -> },

    // Location & Feed
    locationFeedRecords: List<LocationFeed> = emptyList(),

    onLoadLocationFeed: (String) -> Unit = {},

    onSaveLocationFeed: (
        animalId: String,
        destination: String,
        daysInDestination: String,
        rationName: String,
        rationDays: String,
        rationCost: String
    ) -> Unit = { _, _, _, _, _, _ -> }
) {

    var currentScreen by remember {
        mutableStateOf(TraceabilityScreen.HOME)
    }

    var selectedAnimalReference by remember {
        mutableStateOf("")
    }

    /*
     * Determines which screen should open
     * after an animal has successfully been found.
     */
    var findAnimalDestination by remember {
        mutableStateOf(TraceabilityScreen.ANIMAL_RECORD)
    }

    fun goBack() {

        currentScreen =
            when (currentScreen) {

                TraceabilityScreen.SUPPLIER,
                TraceabilityScreen.LOCATION_FEED,
                TraceabilityScreen.TREATMENTS,
                TraceabilityScreen.COST_SUMMARY,
                TraceabilityScreen.MORTALITY -> {

                    TraceabilityScreen.ANIMAL_RECORD
                }

                else -> {

                    TraceabilityScreen.HOME
                }
            }
    }

    if (currentScreen != TraceabilityScreen.HOME) {

        BackHandler {
            goBack()
        }
    }

    when (currentScreen) {

        TraceabilityScreen.HOME -> {

            FarmTraceabilityScreen(

                onFarmerFarmProfileClick = {

                    currentScreen =
                        TraceabilityScreen.FARMER_FARM_PROFILE
                },

                onFindAnimalClick = {

                    findAnimalDestination =
                        TraceabilityScreen.ANIMAL_RECORD

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onAnimalRecordClick = {

                    findAnimalDestination =
                        TraceabilityScreen.ANIMAL_RECORD

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onAnimalMovementClick = {

                    findAnimalDestination =
                        TraceabilityScreen.ANIMAL_MOVEMENT

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onSupplierClick = {

                    findAnimalDestination =
                        TraceabilityScreen.SUPPLIER

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onLocationFeedClick = {

                    findAnimalDestination =
                        TraceabilityScreen.LOCATION_FEED

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onTreatmentsClick = {

                    findAnimalDestination =
                        TraceabilityScreen.TREATMENTS

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onCostSummaryClick = {

                    findAnimalDestination =
                        TraceabilityScreen.COST_SUMMARY

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onMortalityClick = {

                    findAnimalDestination =
                        TraceabilityScreen.MORTALITY

                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                }
            )
        }

        TraceabilityScreen.FARMER_FARM_PROFILE -> {

            FarmerFarmProfileScreen(

                onBackClick = {

                    goBack()
                }
            )
        }

        TraceabilityScreen.FIND_ANIMAL -> {

            val database =
                DatabaseProvider.getDatabase()

            if (database == null) {

                FindAnimalScreen(

                    errorMessage =
                        "The encrypted database has not been initialised yet.",

                    onBackClick = {

                        goBack()
                    }
                )

            } else {

                val repository =
                    remember(database) {

                        FindAnimalRepository(
                            calfRegistrationDao =
                                database.calfRegistrationDao()
                        )
                    }

                val factory =
                    remember(repository) {

                        FindAnimalViewModelFactory(
                            repository
                        )
                    }

                val findAnimalViewModel:
                        FindAnimalViewModel =
                    viewModel(
                        factory = factory
                    )

                val uiState by
                findAnimalViewModel
                    .uiState
                    .collectAsState()

                LaunchedEffect(uiState) {

                    val state =
                        uiState

                    if (state is FindAnimalUiState.Found) {

                        selectedAnimalReference =
                            state.animal.animalId

                        findAnimalViewModel
                            .resetState()

                        currentScreen =
                            findAnimalDestination
                    }
                }

                val errorMessage =
                    when (val state = uiState) {

                        is FindAnimalUiState.NotFound -> {

                            "Animal ${state.animalReference} was not found."
                        }

                        is FindAnimalUiState.Error -> {

                            state.message
                        }

                        else -> {

                            null
                        }
                    }

                FindAnimalScreen(

                    isLoading =
                        uiState is FindAnimalUiState.Loading,

                    errorMessage =
                        errorMessage,

                    onBackClick = {

                        findAnimalViewModel
                            .resetState()

                        goBack()
                    },

                    onFindAnimal = { reference ->

                        findAnimalViewModel
                            .findAnimal(reference)
                    }
                )
            }
        }

        TraceabilityScreen.ANIMAL_RECORD -> {

            AnimalRecordScreen(

                animalReference =
                    selectedAnimalReference,

                onBackClick = {

                    goBack()
                },

                onSupplierClick = {

                    currentScreen =
                        TraceabilityScreen.SUPPLIER
                },

                onLocationFeedClick = {

                    currentScreen =
                        TraceabilityScreen.LOCATION_FEED
                },

                onTreatmentsClick = {

                    currentScreen =
                        TraceabilityScreen.TREATMENTS
                },

                onCostSummaryClick = {

                    currentScreen =
                        TraceabilityScreen.COST_SUMMARY
                }
            )
        }

        TraceabilityScreen.ANIMAL_MOVEMENT -> {

            LaunchedEffect(
                selectedAnimalReference
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadMovements(
                        selectedAnimalReference
                    )
                }
            }

            AnimalMovementScreen(

                animalReference =
                    selectedAnimalReference,

                movementRecords =
                    movementRecords,

                onBackClick = {

                    goBack()
                },

                onSaveClick = {
                        movementInformation,
                        responsibleWorker ->

                    onSaveMovement(
                        selectedAnimalReference,
                        movementInformation,
                        responsibleWorker
                    )
                }
            )
        }

        TraceabilityScreen.SUPPLIER -> {

            LaunchedEffect(
                selectedAnimalReference
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadSuppliers(
                        selectedAnimalReference
                    )
                }
            }

            SupplierScreen(

                animalReference =
                    selectedAnimalReference,

                supplierRecords =
                    supplierRecords,

                onBackClick = {

                    goBack()
                },

                onSaveClick = {
                        supplierName,
                        glnNumber,
                        purchaseDate,
                        purchaseBatchNumber ->

                    onSaveSupplier(
                        selectedAnimalReference,
                        supplierName,
                        glnNumber,
                        purchaseDate,
                        purchaseBatchNumber
                    )
                }
            )
        }

        TraceabilityScreen.LOCATION_FEED -> {

            /*
             * Load saved Location & Feed records
             * for the selected animal.
             */
            LaunchedEffect(
                selectedAnimalReference
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadLocationFeed(
                        selectedAnimalReference
                    )
                }
            }

            LocationFeedScreen(

                animalReference =
                    selectedAnimalReference,

                locationFeedRecords =
                    locationFeedRecords,

                onBackClick = {

                    goBack()
                },

                onSaveClick = {
                        destination,
                        daysInDestination,
                        rationName,
                        rationDays,
                        rationCost ->

                    onSaveLocationFeed(
                        selectedAnimalReference,
                        destination,
                        daysInDestination,
                        rationName,
                        rationDays,
                        rationCost
                    )
                }
            )
        }

        TraceabilityScreen.TREATMENTS -> {

            LaunchedEffect(
                selectedAnimalReference
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadTreatments(
                        selectedAnimalReference
                    )
                }
            }

            TreatmentsScreen(

                animalReference =
                    selectedAnimalReference,

                treatmentRecords =
                    treatmentRecords,

                onBackClick = {

                    goBack()
                },

                onSaveClick = {
                        disease,
                        treatment,
                        batchNumber,
                        volumeUsed,
                        cost ->

                    onSaveTreatment(
                        selectedAnimalReference,
                        disease,
                        treatment,
                        batchNumber,
                        volumeUsed,
                        cost
                    )
                }
            )
        }

        TraceabilityScreen.COST_SUMMARY -> {

            LaunchedEffect(
                selectedAnimalReference
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadCostSummary(
                        selectedAnimalReference
                    )
                }
            }

            CostSummaryScreen(

                animalReference =
                    selectedAnimalReference,

                transportCost =
                    "%.2f".format(
                        transportCost
                    ),

                processingCost =
                    "%.2f".format(
                        processingCost
                    ),

                treatmentCost =
                    "%.2f".format(
                        treatmentCost
                    ),

                feedCost =
                    "%.2f".format(
                        feedCost
                    ),

                handlingCost =
                    "%.2f".format(
                        handlingCost
                    ),

                interestCost =
                    "%.2f".format(
                        interestCost
                    ),

                totalAnimalCost =
                    "%.2f".format(
                        totalAnimalCost
                    ),

                onBackClick = {

                    goBack()
                }
            )
        }

        TraceabilityScreen.MORTALITY -> {

            LaunchedEffect(
                selectedAnimalReference
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadMortalities(
                        selectedAnimalReference
                    )
                }
            }

            MortalityScreen(

                animalReference =
                    selectedAnimalReference,

                mortalityRecords =
                    mortalityRecords,

                onBackClick = {

                    goBack()
                },

                onSaveClick = {
                        mortalityReason,
                        responsibleWorker ->

                    onSaveMortality(
                        selectedAnimalReference,
                        mortalityReason,
                        responsibleWorker
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FarmTraceabilityFlowPreview() {

    FarmTraceabilityFlow()
}
