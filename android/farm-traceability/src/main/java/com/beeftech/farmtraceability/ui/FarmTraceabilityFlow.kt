package com.beeftech.farmtraceability.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

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
    onExitTraceability: () -> Unit = {}
) {
    var currentScreen by remember {
        mutableStateOf(TraceabilityScreen.HOME)
    }

    val navigationHistory = remember {
        mutableStateListOf<TraceabilityScreen>()
    }

    var selectedAnimalReference by remember {
        mutableStateOf("")
    }

    fun navigateTo(screen: TraceabilityScreen) {
        navigationHistory.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (navigationHistory.isNotEmpty()) {
            currentScreen =
                navigationHistory.removeAt(navigationHistory.lastIndex)
        } else if (currentScreen != TraceabilityScreen.HOME) {
            currentScreen = TraceabilityScreen.HOME
        } else {
            onExitTraceability()
        }
    }

    BackHandler {
        navigateBack()
    }

    when (currentScreen) {

        TraceabilityScreen.HOME -> {
            FarmTraceabilityScreen(
                onBackClick = {
                    navigateBack()
                },

                onFarmerFarmProfileClick = {
                    navigateTo(
                        TraceabilityScreen.FARMER_FARM_PROFILE
                    )
                },

                onFindAnimalClick = {
                    navigateTo(
                        TraceabilityScreen.FIND_ANIMAL
                    )
                },

                onAnimalRecordClick = {
                    navigateTo(
                        TraceabilityScreen.ANIMAL_RECORD
                    )
                },

                onAnimalMovementClick = {
                    navigateTo(
                        TraceabilityScreen.ANIMAL_MOVEMENT
                    )
                },

                onSupplierClick = {
                    navigateTo(
                        TraceabilityScreen.SUPPLIER
                    )
                },

                onLocationFeedClick = {
                    navigateTo(
                        TraceabilityScreen.LOCATION_FEED
                    )
                },

                onTreatmentsClick = {
                    navigateTo(
                        TraceabilityScreen.TREATMENTS
                    )
                },

                onCostSummaryClick = {
                    navigateTo(
                        TraceabilityScreen.COST_SUMMARY
                    )
                },

                onMortalityClick = {
                    navigateTo(
                        TraceabilityScreen.MORTALITY
                    )
                }
            )
        }

        TraceabilityScreen.FARMER_FARM_PROFILE -> {
            FarmerFarmProfileScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen.FIND_ANIMAL -> {
            FindAnimalScreen(
                onBackClick = {
                    navigateBack()
                },

                onFindAnimal = { reference ->
                    selectedAnimalReference = reference

                    navigateTo(
                        TraceabilityScreen.ANIMAL_RECORD
                    )
                }
            )
        }

        TraceabilityScreen.ANIMAL_RECORD -> {
            AnimalRecordScreen(
                animalReference = selectedAnimalReference,

                onBackClick = {
                    navigateBack()
                },

                onSupplierClick = {
                    navigateTo(
                        TraceabilityScreen.SUPPLIER
                    )
                },

                onLocationFeedClick = {
                    navigateTo(
                        TraceabilityScreen.LOCATION_FEED
                    )
                },

                onTreatmentsClick = {
                    navigateTo(
                        TraceabilityScreen.TREATMENTS
                    )
                },

                onAnimalMovementClick = {
                    navigateTo(
                        TraceabilityScreen.ANIMAL_MOVEMENT
                    )
                },

                onCostSummaryClick = {
                    navigateTo(
                        TraceabilityScreen.COST_SUMMARY
                    )
                },

                onMortalityClick = {
                    navigateTo(
                        TraceabilityScreen.MORTALITY
                    )
                }
            )
        }

        TraceabilityScreen.ANIMAL_MOVEMENT -> {
            AnimalMovementScreen(
                animalReference = selectedAnimalReference,

                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen.SUPPLIER -> {
            SupplierScreen(
                animalReference = selectedAnimalReference,

                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen.LOCATION_FEED -> {
            LocationFeedScreen(
                animalReference = selectedAnimalReference,

                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen.TREATMENTS -> {
            TreatmentsScreen(
                animalReference = selectedAnimalReference,

                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen.COST_SUMMARY -> {
            CostSummaryScreen(
                animalReference = selectedAnimalReference,

                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen.MORTALITY -> {
            MortalityScreen(
                animalReference = selectedAnimalReference,

                onBackClick = {
                    navigateBack()
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