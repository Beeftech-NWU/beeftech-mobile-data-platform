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

                /*
                 * When an animal is successfully found,
                 * save its reference and navigate to
                 * the Animal Record screen.
                 */
                LaunchedEffect(uiState) {

                    val state = uiState

                    if (state is FindAnimalUiState.Found) {

                        selectedAnimalReference =
                            state.animal.animalId

                        findAnimalViewModel
                            .resetState()

                        currentScreen =
                            TraceabilityScreen.ANIMAL_RECORD
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

            AnimalMovementScreen(

                animalReference =
                    selectedAnimalReference,

                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.SUPPLIER -> {

            SupplierScreen(

                animalReference =
                    selectedAnimalReference,

                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.LOCATION_FEED -> {

            LocationFeedScreen(

                animalReference =
                    selectedAnimalReference,

                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.TREATMENTS -> {

            TreatmentsScreen(

                animalReference =
                    selectedAnimalReference,

                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.COST_SUMMARY -> {

            CostSummaryScreen(

                animalReference =
                    selectedAnimalReference,

                onBackClick = {
                    goBack()
                }
            )
        }

        TraceabilityScreen.MORTALITY -> {

            MortalityScreen(

                animalReference =
                    selectedAnimalReference,

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