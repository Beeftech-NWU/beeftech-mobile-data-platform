package com.beeftech.farmtraceability.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.beeftech.database.repository.SyncRepository
import com.beeftech.farmtraceability.repository.FindAnimalRepository
import com.beeftech.farmtraceability.viewmodel.FindAnimalUiState
import com.beeftech.farmtraceability.viewmodel.FindAnimalViewModel
import com.beeftech.farmtraceability.viewmodel.FindAnimalViewModelFactory
import com.beeftech.farmtraceability.viewmodel.SyncStatusViewModel
import com.beeftech.farmtraceability.viewmodel.SyncStatusViewModelFactory

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
    onExitTraceability: () -> Unit = {},

    onFarmerRegistrationClick: () -> Unit = {},

    movementRecords: List<AnimalMovement> = emptyList(),

    onLoadMovements: (String) -> Unit = {},

    onSaveMovement: (
        animalId: String,
        movementInformation: String,
        responsibleWorker: String
    ) -> Unit = { _, _, _ -> },

    treatmentRecords: List<Treatment> = emptyList(),

    diseaseOptions: List<String> = emptyList(),

    treatmentOptions: List<String> = emptyList(),

    onLoadTreatments: (String) -> Unit = {},

    onSaveTreatment: (
        animalId: String,
        disease: String,
        treatment: String,
        batchNumber: String,
        volumeUsed: String,
        cost: String
    ) -> Unit = { _, _, _, _, _, _ -> },

    mortalityRecords: List<Mortality> = emptyList(),

    onLoadMortalities: (String) -> Unit = {},

    onSaveMortality: (
        animalId: String,
        mortalityReason: String,
        responsibleWorker: String
    ) -> Unit = { _, _, _ -> },

    transportCost: Double = 0.0,
    processingCost: Double = 0.0,
    treatmentCost: Double = 0.0,
    feedCost: Double = 0.0,
    handlingCost: Double = 0.0,
    interestCost: Double = 0.0,
    totalAnimalCost: Double = 0.0,

    onLoadCostSummary: (String) -> Unit = {},

    supplierRecords: List<Supplier> = emptyList(),

    onLoadSuppliers: (String) -> Unit = {},

    onSaveSupplier: (
        animalId: String,
        supplierName: String,
        glnNumber: String,
        purchaseDate: String,
        purchaseBatchNumber: String
    ) -> Unit = { _, _, _, _, _ -> },

    locationFeedRecords: List<LocationFeed> = emptyList(),

    onLoadLocationFeed: (String) -> Unit = {},

    onSaveLocationFeed: (
        animalId: String,
        destination: String,
        daysInDestination: String,
        rationName: String,
        rationDays: String,
        rationCost: String
    ) -> Unit = { _, _, _, _, _, _ -> },

    onRetrySyncClick: () -> Unit = {}
) {

    var currentScreen by remember {
        mutableStateOf(
            TraceabilityScreen.HOME
        )
    }

    val navigationHistory =
        remember {
            mutableStateListOf<
                    TraceabilityScreen
                    >()
        }

    var selectedAnimalReference by remember {
        mutableStateOf("")
    }

    var findAnimalDestination by remember {
        mutableStateOf(
            TraceabilityScreen.ANIMAL_RECORD
        )
    }

    fun navigateTo(
        screen: TraceabilityScreen
    ) {
        navigationHistory.add(
            currentScreen
        )

        currentScreen = screen
    }

    fun navigateBack() {
        if (
            navigationHistory.isNotEmpty()
        ) {
            currentScreen =
                navigationHistory.removeAt(
                    navigationHistory.lastIndex
                )
        } else if (
            currentScreen !=
            TraceabilityScreen.HOME
        ) {
            currentScreen =
                TraceabilityScreen.HOME
        } else {
            onExitTraceability()
        }
    }

    BackHandler {
        navigateBack()
    }

    when (currentScreen) {

        TraceabilityScreen.HOME -> {

            val database =
                DatabaseProvider
                    .getDatabase()

            if (database == null) {

                FarmTraceabilityScreen(
                    pendingRecordCount = null,
                    lastSync = "",
                    syncStatus = "Database unavailable",
                    syncWarningLevel = 0,
                    scheduledSync = "",
                    retrySyncAvailable = false,

                    onBackClick = {
                        navigateBack()
                    },

                    onFarmerFarmProfileClick = {
                        navigateTo(
                            TraceabilityScreen
                                .FARMER_FARM_PROFILE
                        )
                    },

                    onFarmerRegistrationClick =
                        onFarmerRegistrationClick,

                    onFindAnimalClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .ANIMAL_RECORD

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onAnimalRecordClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .ANIMAL_RECORD

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onAnimalMovementClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .ANIMAL_MOVEMENT

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onSupplierClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .SUPPLIER

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onLocationFeedClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .LOCATION_FEED

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onTreatmentsClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .TREATMENTS

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onCostSummaryClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .COST_SUMMARY

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onMortalityClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .MORTALITY

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    }
                )

            } else {

                val syncRepository =
                    remember(database) {
                        SyncRepository(
                            pendingSyncDao =
                                database.pendingSyncDao(),

                            syncBatchDao =
                                database.syncBatchDao()
                        )
                    }

                val syncFactory =
                    remember(syncRepository) {
                        SyncStatusViewModelFactory(
                            syncRepository
                        )
                    }

                val syncStatusViewModel:
                        SyncStatusViewModel =
                    viewModel(
                        factory = syncFactory
                    )

                val syncState by
                syncStatusViewModel
                    .uiState
                    .collectAsState()

                FarmTraceabilityScreen(
                    pendingRecordCount =
                        syncState.pendingRecordCount,

                    lastSync =
                        syncState.lastSync,

                    syncStatus =
                        syncState.syncStatus,

                    syncWarningLevel =
                        syncState.syncWarningLevel,

                    scheduledSync = "Every 15 minutes when connected",

                    retrySyncAvailable =
                        (syncState.pendingRecordCount ?: 0) > 0,

                    onRetrySyncClick =
                        onRetrySyncClick,

                    onBackClick = {
                        navigateBack()
                    },

                    onFarmerFarmProfileClick = {
                        navigateTo(
                            TraceabilityScreen
                                .FARMER_FARM_PROFILE
                        )
                    },

                    onFarmerRegistrationClick =
                        onFarmerRegistrationClick,

                    onFindAnimalClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .ANIMAL_RECORD

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onAnimalRecordClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .ANIMAL_RECORD

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onAnimalMovementClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .ANIMAL_MOVEMENT

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onSupplierClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .SUPPLIER

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onLocationFeedClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .LOCATION_FEED

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onTreatmentsClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .TREATMENTS

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onCostSummaryClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .COST_SUMMARY

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    },

                    onMortalityClick = {
                        findAnimalDestination =
                            TraceabilityScreen
                                .MORTALITY

                        navigateTo(
                            TraceabilityScreen
                                .FIND_ANIMAL
                        )
                    }
                )
            }
        }

        TraceabilityScreen
            .FARMER_FARM_PROFILE -> {

            FarmerFarmProfileScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen
            .FIND_ANIMAL -> {

            val database =
                DatabaseProvider
                    .getDatabase()

            if (database == null) {

                FindAnimalScreen(
                    errorMessage =
                        "The encrypted database has not been initialised yet.",

                    onBackClick = {
                        navigateBack()
                    }
                )

            } else {

                val repository =
                    remember(database) {
                        FindAnimalRepository(
                            calfRegistrationDao =
                                database
                                    .calfRegistrationDao()
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

                LaunchedEffect(
                    uiState
                ) {
                    val state =
                        uiState

                    if (
                        state is
                                FindAnimalUiState
                                .Found
                    ) {
                        selectedAnimalReference =
                            state
                                .animal
                                .animalId

                        findAnimalViewModel
                            .resetState()

                        if (
                            findAnimalDestination !=
                            TraceabilityScreen
                                .ANIMAL_RECORD
                        ) {
                            navigationHistory.add(
                                TraceabilityScreen
                                    .ANIMAL_RECORD
                            )
                        }

                        currentScreen =
                            findAnimalDestination
                    }
                }

                val errorMessage =
                    when (
                        val state =
                            uiState
                    ) {
                        is FindAnimalUiState
                        .NotFound -> {

                            "Animal ${state.animalReference} was not found."
                        }

                        is FindAnimalUiState
                        .Error -> {

                            state.message
                        }

                        else -> {
                            null
                        }
                    }

                FindAnimalScreen(
                    isLoading =
                        uiState is
                                FindAnimalUiState
                                .Loading,

                    errorMessage =
                        errorMessage,

                    onBackClick = {
                        findAnimalViewModel
                            .resetState()

                        navigateBack()
                    },

                    onFindAnimal = {
                            reference ->

                        findAnimalViewModel
                            .findAnimal(
                                reference
                            )
                    }
                )
            }
        }

        TraceabilityScreen
            .ANIMAL_RECORD -> {

            AnimalRecordScreen(
                animalReference =
                    selectedAnimalReference,

                onBackClick = {
                    navigateBack()
                },

                onSupplierClick = {
                    navigateTo(
                        TraceabilityScreen
                            .SUPPLIER
                    )
                },

                onLocationFeedClick = {
                    navigateTo(
                        TraceabilityScreen
                            .LOCATION_FEED
                    )
                },

                onTreatmentsClick = {
                    navigateTo(
                        TraceabilityScreen
                            .TREATMENTS
                    )
                },

                onAnimalMovementClick = {
                    navigateTo(
                        TraceabilityScreen
                            .ANIMAL_MOVEMENT
                    )
                },

                onCostSummaryClick = {
                    navigateTo(
                        TraceabilityScreen
                            .COST_SUMMARY
                    )
                },

                onMortalityClick = {
                    navigateTo(
                        TraceabilityScreen
                            .MORTALITY
                    )
                }
            )
        }

        TraceabilityScreen
            .ANIMAL_MOVEMENT -> {

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
                    navigateBack()
                },

                onAddMovementClick = {
                        _,
                        movementInformation,
                        responsibleWorker ->

                    onSaveMovement(
                        selectedAnimalReference,
                        movementInformation,
                        responsibleWorker
                    )
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

        TraceabilityScreen
            .SUPPLIER -> {

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
                    navigateBack()
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

        TraceabilityScreen
            .LOCATION_FEED -> {

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
                    navigateBack()
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

        TraceabilityScreen
            .TREATMENTS -> {

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

                diseaseOptions =
                    diseaseOptions,

                treatmentOptions =
                    treatmentOptions,

                treatmentRecords =
                    treatmentRecords,

                onBackClick = {
                    navigateBack()
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

        TraceabilityScreen
            .COST_SUMMARY -> {

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
                    navigateBack()
                }
            )
        }

        TraceabilityScreen
            .MORTALITY -> {

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

                mortalityCount =
                    mortalityRecords.size,

                onBackClick = {
                    navigateBack()
                },

                onAddMortalityClick = {
                        _,
                        mortalityReason,
                        responsibleWorker ->

                    onSaveMortality(
                        selectedAnimalReference,
                        mortalityReason,
                        responsibleWorker
                    )
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

