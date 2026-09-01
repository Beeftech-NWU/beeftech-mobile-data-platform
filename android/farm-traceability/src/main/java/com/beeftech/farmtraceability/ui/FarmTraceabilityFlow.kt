package com.beeftech.farmtraceability.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun FarmTraceabilityFlow() {

    var currentScreen by remember {
        mutableStateOf(TraceabilityScreen.HOME)
    }

    var selectedAnimalReference by remember {
        mutableStateOf("")
    }

    fun goBack() {
        currentScreen = when (currentScreen) {

            TraceabilityScreen.SUPPLIER,
            TraceabilityScreen.LOCATION_FEED,
            TraceabilityScreen.TREATMENTS,
            TraceabilityScreen.COST_SUMMARY -> {
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
                    currentScreen =
                        TraceabilityScreen.FIND_ANIMAL
                },

                onAnimalRecordClick = {
                    currentScreen =
                        TraceabilityScreen.ANIMAL_RECORD
                },

                onAnimalMovementClick = {
                    currentScreen =
                        TraceabilityScreen.ANIMAL_MOVEMENT
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
                },

                onMortalityClick = {
                    currentScreen =
                        TraceabilityScreen.MORTALITY
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
            FindAnimalScreen(
                onBackClick = {
                    goBack()
                },
                onFindAnimal = { reference ->
                    selectedAnimalReference = reference
                    currentScreen =
                        TraceabilityScreen.ANIMAL_RECORD
                }
            )
        }

        TraceabilityScreen.ANIMAL_RECORD -> {
            AnimalRecordScreen(
                animalReference = selectedAnimalReference,

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
            AnimalMovementScreen(
                animalReference = selectedAnimalReference,
                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.SUPPLIER -> {
            SupplierScreen(
                animalReference = selectedAnimalReference,
                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.LOCATION_FEED -> {
            LocationFeedScreen(
                animalReference = selectedAnimalReference,
                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.TREATMENTS -> {
            TreatmentsScreen(
                animalReference = selectedAnimalReference,
                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.COST_SUMMARY -> {
            CostSummaryScreen(
                animalReference = selectedAnimalReference,
                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.MORTALITY -> {
            MortalityScreen(
                animalReference = selectedAnimalReference,
                onBackClick = {
                    goBack()
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