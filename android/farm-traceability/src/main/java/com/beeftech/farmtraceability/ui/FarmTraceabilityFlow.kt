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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.entity.AnimalPurchaseEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.Treatment
import com.beeftech.database.repository.SyncPolicyStore
import com.beeftech.database.repository.SyncRepository
import com.beeftech.farmtraceability.repository.AnimalRecordRepository
import com.beeftech.farmtraceability.repository.AnimalRecordSummary
import com.beeftech.farmtraceability.repository.FindAnimalRepository
import com.beeftech.farmtraceability.repository.TraceabilityLookupRepository
import com.beeftech.farmtraceability.viewmodel.FindAnimalUiState
import com.beeftech.farmtraceability.viewmodel.FindAnimalViewModel
import com.beeftech.farmtraceability.viewmodel.FindAnimalViewModelFactory
import com.beeftech.farmtraceability.viewmodel.SyncStatusViewModel
import com.beeftech.farmtraceability.viewmodel.SyncStatusViewModelFactory
import com.beeftech.farmtraceability.worker.TraceabilitySyncScheduler

private enum class TraceabilityScreen {
    HOME,
    REGISTERED_FARMERS,
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

    movementRecords: List<AnimalMovementEntity> = emptyList(),

    onLoadMovements: (String) -> Unit = {},

    onSaveMovement: (
        animalId: String,
        movementInformation: String,
        responsibleWorker: String,
        onCompleted: (
            Boolean,
            String
        ) -> Unit
    ) -> Unit = { _, _, _, onCompleted ->
        onCompleted(
            false,
            "Movement save is unavailable."
        )
    },

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
        cost: String,
        onCompleted: (
            Boolean,
            String
        ) -> Unit
    ) -> Unit = { _, _, _, _, _, _, onCompleted ->
        onCompleted(
            false,
            "Treatment save is unavailable."
        )
    },

    mortalityRecords: List<Mortality> = emptyList(),

    onLoadMortalities: (String) -> Unit = {},

    onSaveMortality: (
        animalId: String,
        mortalityReason: String,
        responsibleWorker: String,
        onCompleted: (
            Boolean,
            String
        ) -> Unit
    ) -> Unit = { _, _, _, onCompleted ->
        onCompleted(
            false,
            "Mortality save is unavailable."
        )
    },

    transportCost: Double = 0.0,
    processingCost: Double = 0.0,
    treatmentCost: Double = 0.0,
    feedCost: Double = 0.0,
    handlingCost: Double = 0.0,
    interestCost: Double = 0.0,
    otherCost: Double = 0.0,
    totalAnimalCost: Double = 0.0,

    onLoadCostSummary: (String) -> Unit = {},

    supplierRecords: List<AnimalPurchaseEntity> = emptyList(),

    onLoadSuppliers: (String) -> Unit = {},

    onSaveSupplier: (
        animalId: String,
        supplierName: String,
        glnNumber: String,
        purchaseDate: String,
        purchaseBatchNumber: String,
        onCompleted: (
            Boolean,
            String
        ) -> Unit
    ) -> Unit = { _, _, _, _, _, onCompleted ->
        onCompleted(
            false,
            "Supplier save is unavailable."
        )
    },

    locationFeedRecords: List<AnimalMovementEntity> = emptyList(),

    onLoadLocationFeed: (String) -> Unit = {},

    onSaveLocationFeed: (
        animalId: String,
        destination: String,
        daysInDestination: String,
        rationName: String,
        rationDays: String,
        rationCost: String,
        onCompleted: (
            Boolean,
            String
        ) -> Unit
    ) -> Unit = { _, _, _, _, _, _, onCompleted ->
        onCompleted(
            false,
            "Location and feed save is unavailable."
        )
    },

    onRetrySyncClick: () -> Unit = {}
) {

    val traceabilityContext =
        LocalContext.current

    LaunchedEffect(
        traceabilityContext
    ) {

        TraceabilitySyncScheduler
            .initialize(
                traceabilityContext
            )
    }

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

    // animals.animalId (UUID): the key movements, treatments and costs use.
    var selectedAnimalReference by remember {
        mutableStateOf("")
    }

    // Human-readable tag for display only.
    var selectedTagNumber by remember {
        mutableStateOf("")
    }

    var selectedFarmerId by remember {
        mutableStateOf<String?>(null)
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

    /*
     * Successful saves return to the selected Animal Record.
     * Failed validation stays on the current form.
     */
    fun returnToAnimalRecordAfterSave() {

        if (
            navigationHistory.isNotEmpty() &&
            navigationHistory.last() ==
            TraceabilityScreen.ANIMAL_RECORD
        ) {

            navigationHistory.removeAt(
                navigationHistory.lastIndex
            )
        }

        currentScreen =
            TraceabilityScreen.ANIMAL_RECORD
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
                        selectedFarmerId = null

                        navigateTo(
                            TraceabilityScreen
                                .REGISTERED_FARMERS
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
                            syncRepository,
                            policyProvider = {
                                SyncPolicyStore(database.referenceDataDao()).current()
                            }
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

                    scheduledSync = "05:00-06:00 morning | 18:00-19:00 evening",

                    retrySyncAvailable =
                        (syncState.pendingRecordCount ?: 0) > 0,

                    onRetrySyncClick = {

                        TraceabilitySyncScheduler
                            .kick()

                        onRetrySyncClick()
                    },

                    onBackClick = {
                        navigateBack()
                    },

                    onFarmerFarmProfileClick = {
                        selectedFarmerId = null

                        navigateTo(
                            TraceabilityScreen
                                .REGISTERED_FARMERS
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
            .REGISTERED_FARMERS -> {

            val database =
                DatabaseProvider
                    .getDatabase()

            var farmers by
                remember(database) {
                    mutableStateOf(
                        emptyList<
                                com.beeftech.database.entity.FarmerEntity
                                >()
                    )
                }

            var isLoadingFarmers by
                remember(database) {
                    mutableStateOf(true)
                }

            var farmerListError by
                remember(database) {
                    mutableStateOf("")
                }

            LaunchedEffect(database) {

                isLoadingFarmers = true
                farmerListError = ""

                if (database == null) {

                    farmers = emptyList()

                    farmerListError =
                        "The encrypted database has not been initialised yet."

                    isLoadingFarmers = false

                } else {

                    try {

                        val repository =
                            com.beeftech.database.repository
                                .FarmerRepository(
                                    database.farmerDao()
                                )

                        farmers =
                            repository
                                .getAllFarmers()

                    } catch (
                        exception: Exception
                    ) {

                        farmers = emptyList()

                        farmerListError =
                            exception.message
                                ?: "Unable to load registered farmers."

                    } finally {

                        isLoadingFarmers = false
                    }
                }
            }

            RegisteredFarmersScreen(
                farmers = farmers,

                isLoading =
                    isLoadingFarmers,

                errorMessage =
                    farmerListError,

                onFarmerClick = {
                        farmerId ->

                    selectedFarmerId =
                        farmerId

                    navigateTo(
                        TraceabilityScreen
                            .FARMER_FARM_PROFILE
                    )
                },

                onBackClick = {
                    navigateBack()
                }
            )
        }

        TraceabilityScreen
            .FARMER_FARM_PROFILE -> {

            val database =
                DatabaseProvider
                    .getDatabase()

            val farmerId =
                selectedFarmerId

            var farmer by
                remember(
                    database,
                    farmerId
                ) {
                    mutableStateOf<
                            com.beeftech.database.entity.FarmerEntity?
                            >(null)
                }

            var address by
                remember(
                    database,
                    farmerId
                ) {
                    mutableStateOf<
                            com.beeftech.database.entity.FarmerAddressEntity?
                            >(null)
                }

            var businessRoleNames by
                remember(
                    database,
                    farmerId
                ) {
                    mutableStateOf(
                        emptyList<String>()
                    )
                }

            var isLoadingProfile by
                remember(
                    database,
                    farmerId
                ) {
                    mutableStateOf(true)
                }

            var profileError by
                remember(
                    database,
                    farmerId
                ) {
                    mutableStateOf("")
                }

            LaunchedEffect(
                database,
                farmerId
            ) {

                isLoadingProfile = true
                profileError = ""

                farmer = null
                address = null
                businessRoleNames =
                    emptyList()

                when {

                    database == null -> {

                        profileError =
                            "The encrypted database has not been initialised yet."
                    }

                    farmerId.isNullOrBlank() -> {

                        profileError =
                            "No farmer has been selected."
                    }

                    else -> {

                        try {

                            val repository =
                                com.beeftech.database.repository
                                    .FarmerRepository(
                                        database.farmerDao()
                                    )

                            val loadedFarmer =
                                repository
                                    .getFarmer(
                                        farmerId
                                    )

                            if (
                                loadedFarmer == null
                            ) {

                                profileError =
                                    "The selected farmer could not be found."

                            } else {

                                farmer =
                                    loadedFarmer

                                val addresses =
                                    repository
                                        .getAddressesForFarmer(
                                            farmerId
                                        )

                                address =
                                    addresses
                                        .firstOrNull {
                                            it.address_type ==
                                                "PRIMARY"
                                        }
                                        ?: addresses.firstOrNull()

                                businessRoleNames =
                                    repository
                                        .getRolesForFarmer(
                                            farmerId
                                        )
                                        .map {
                                                role ->

                                            when (
                                                role.role_id
                                            ) {
                                                1L -> "Agent"
                                                2L -> "Buyer"
                                                3L -> "Client"
                                                4L -> "Location"
                                                5L -> "Feedlot"
                                                6L -> "Owner"
                                                7L -> "Supplier"
                                                8L -> "Transporter"

                                                else ->
                                                    "Role ${role.role_id}"
                                            }
                                        }
                                        .distinct()
                            }

                        } catch (
                            exception: Exception
                        ) {

                            profileError =
                                exception.message
                                    ?: "Unable to load farmer profile."
                        }
                    }
                }

                isLoadingProfile = false
            }

            val loadedFarmer =
                farmer

            val loadedAddress =
                address

            val formattedAddress =
                listOfNotNull(
                    loadedAddress
                        ?.address_line_1
                        ?.takeIf {
                            it.isNotBlank()
                        },

                    loadedAddress
                        ?.province
                        ?.takeIf {
                            it.isNotBlank()
                        },

                    loadedAddress
                        ?.postal_code
                        ?.takeIf {
                            it.isNotBlank()
                        }
                )
                    .joinToString(", ")

            val latitude =
                loadedAddress
                    ?.gps_latitude
                    ?: loadedFarmer
                        ?.gps_latitude

            val longitude =
                loadedAddress
                    ?.gps_longitude
                    ?: loadedFarmer
                        ?.gps_longitude

            val coordinates =
                if (
                    latitude != null &&
                    longitude != null
                ) {
                    "$latitude, $longitude"
                } else {
                    ""
                }

            FarmerFarmProfileScreen(
                organisationName =
                    loadedFarmer
                        ?.organisation_name
                        .orEmpty(),

                clientCode =
                    loadedFarmer
                        ?.client_code
                        .orEmpty(),

                emailAddress =
                    loadedFarmer
                        ?.email_address
                        .orEmpty(),

                vatNumber =
                    loadedFarmer
                        ?.vat_number
                        .orEmpty(),

                coRegIdNo =
                    loadedFarmer
                        ?.co_reg_id_no
                        .orEmpty(),

                landOwnership =
                    loadedFarmer
                        ?.land_ownership
                        .orEmpty(),

                faCodeRmis =
                    loadedFarmer
                        ?.fa_code_rmis
                        .orEmpty(),

                glnNumber =
                    loadedFarmer
                        ?.gln_number
                        .orEmpty(),

                businessRoles =
                    businessRoleNames
                        .joinToString(", "),

                farmAddress =
                    formattedAddress,

                streetCode =
                    loadedAddress
                        ?.street_code
                        .orEmpty(),

                postalAddress =
                    loadedAddress
                        ?.postal_address
                        .orEmpty(),

                country =
                    loadedAddress
                        ?.country
                        .orEmpty(),

                gpsCoordinates =
                    coordinates,

                syncStatus =
                    loadedFarmer
                        ?.sync_status
                        .orEmpty(),

                isLoading =
                    isLoadingProfile,

                errorMessage =
                    profileError,

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

                        selectedTagNumber =
                            state
                                .animal
                                .tagNumber

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

            val database =
                DatabaseProvider
                    .getDatabase()


            var summary by
                remember(
                    database,
                    selectedAnimalReference,
                    selectedTagNumber
                ) {

                    mutableStateOf(
                        AnimalRecordSummary()
                    )
                }


            LaunchedEffect(
                database,
                selectedAnimalReference,
                selectedTagNumber
            ) {

                summary =
                    if (
                        database != null &&
                        selectedAnimalReference
                            .isNotBlank()
                    ) {

                        try {

                            AnimalRecordRepository(
                                database
                            )
                                .load(
                                    animalId =
                                        selectedAnimalReference,

                                    tagNumber =
                                        selectedTagNumber
                                )

                        } catch (
                            _: Exception
                        ) {

                            AnimalRecordSummary()
                        }

                    } else {

                        AnimalRecordSummary()
                    }
            }


            AnimalRecordScreen(
                tagNumber =
                    selectedTagNumber,

                breed =
                    summary.breed,

                gender =
                    summary.gender,

                entryMass =
                    summary.entryMass,

                lastMass =
                    summary.lastMass,

                daysAtFacility =
                    summary.daysAtFacility,

                averageDailyGain =
                    summary.averageDailyGain,

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

            val database =
                DatabaseProvider
                    .getDatabase()


            var workerOptions by
                remember(
                    database,
                    selectedAnimalReference
                ) {

                    mutableStateOf(
                        emptyList<String>()
                    )
                }


            LaunchedEffect(
                database,
                selectedAnimalReference,
                movementRecords
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadMovements(
                        selectedAnimalReference
                    )
                }


                val localUsers =
                    try {

                        database
                            ?.userDao()
                            ?.getAllUsers()
                            .orEmpty()
                            .map {
                                it.username
                                    .trim()
                            }
                            .filter {
                                it.isNotBlank()
                            }

                    } catch (
                        _: Exception
                    ) {

                        emptyList()
                    }


                /*
                 * Keep workers from movement history available
                 * offline as well.
                 */
                val historicalWorkers =
                    movementRecords
                        .filter {
                            it.feedLocationType
                                .isNullOrBlank()
                        }
                        .mapNotNull {
                            it.notes
                                ?.trim()
                                ?.takeIf {
                                        worker ->

                                    worker.isNotBlank() &&
                                        !worker.startsWith(
                                            "Days:",
                                            ignoreCase =
                                                true
                                        )
                                }
                        }


                workerOptions =
                    (
                        localUsers +
                            historicalWorkers
                    )
                        .distinctBy {
                            it.lowercase()
                        }
                        .sortedBy {
                            it.lowercase()
                        }
            }


            /*
             * Movement History displays actual LOCATION
             * TRANSITIONS, not repeated Save presses.
             *
             * We process oldest -> newest so the first valid
             * transition is preserved.
             *
             * Example:
             *
             * 14:49 Pen A
             * 16:20 Pen A
             *
             * Result:
             *
             * 14:49 Pen A
             *
             * But:
             *
             * Pen A -> Pen B -> Pen A
             *
             * keeps all three.
             */
            val visibleMovementRecords =
                remember(
                    movementRecords
                ) {

                    fun destinationKey(
                        rawValue: String
                    ): String {

                        val normalized =
                            rawValue
                                .lowercase()
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
                         * Resolve every variation containing a Pen
                         * to one stable destination key.
                         *
                         * This fixes old data such as:
                         *
                         * "... to Pen A"
                         * "... to n Pen A"
                         * "... to \nPen A"
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

                    val chronological =
                        movementRecords
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


                    chronological.forEach {
                            movement ->

                        val previous =
                            cleaned
                                .lastOrNull()


                        val repeatedDestination =
                            previous != null &&
                                destinationKey(
                                    previous.destinationFarmId
                                ) ==
                                destinationKey(
                                    movement.destinationFarmId
                                )


                        if (
                            !repeatedDestination
                        ) {

                            cleaned +=
                                movement
                        }
                    }


                    cleaned
                        .sortedByDescending {
                            it.movementDate
                        }
                }


            AnimalMovementScreen(
                animalReference =
                    selectedTagNumber
                        .ifBlank {
                            selectedAnimalReference
                        },

                workerOptions =
                    workerOptions,

                movementRecords =
                    visibleMovementRecords,

                onBackClick = {
                    navigateBack()
                },

                onSaveClick = {
                        movementInformation,
                        responsibleWorker ->

                    onSaveMovement(
                        selectedAnimalReference,
                        movementInformation,
                        responsibleWorker
                    ) {
                            success,
                            _ ->

                        if (
                            success
                        ) {

                            returnToAnimalRecordAfterSave()
                        }
                    }
                }
            )
        }

        TraceabilityScreen
            .SUPPLIER -> {

            val database =
                DatabaseProvider.getDatabase()

            var registeredSuppliers by
                remember(
                    database,
                    selectedAnimalReference
                ) {

                    mutableStateOf(
                        emptyList<FarmerEntity>()
                    )
                }

            var selectedSupplierName by
                remember(
                    selectedAnimalReference
                ) {

                    mutableStateOf("")
                }


            LaunchedEffect(
                database,
                selectedAnimalReference
            ) {

                registeredSuppliers =
                    try {

                        database
                            ?.farmerDao()
                            ?.getSupplierFarmers()
                            .orEmpty()

                    } catch (
                        _: Exception
                    ) {

                        emptyList()
                    }
            }


            val selectedSupplier =
                registeredSuppliers
                    .firstOrNull {
                            farmer ->

                        val displayName =
                            farmer
                                .organisation_name
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: farmer
                                    .client_code
                                    .orEmpty()

                        displayName.equals(
                            selectedSupplierName,
                            ignoreCase = true
                        )
                    }


            val supplierOptions =
                registeredSuppliers
                    .mapNotNull {
                            farmer ->

                        farmer
                            .organisation_name
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: farmer
                                .client_code
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                    }
                    .distinct()


            val linkedFarm =
                selectedSupplier
                    ?.let {
                            farmer ->

                        val organisation =
                            farmer
                                .organisation_name
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: farmer
                                    .client_code
                                    .orEmpty()

                        val code =
                            farmer
                                .client_code
                                ?.takeIf {
                                    it.isNotBlank() &&
                                        it != organisation
                                }

                        if (
                            code == null
                        ) {
                            organisation
                        } else {
                            "$organisation ? $code"
                        }
                    }
                    .orEmpty()


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
                    selectedTagNumber
                        .ifBlank {
                            selectedAnimalReference
                        },

                supplierName =
                    selectedSupplierName,

                glnNumber =
                    selectedSupplier
                        ?.gln_number
                        .orEmpty(),

                linkedFarm =
                    linkedFarm,

                supplierOptions =
                    supplierOptions,

                supplierRecords =
                    supplierRecords,

                onSupplierNameChange = {
                        value ->

                    selectedSupplierName =
                        value
                },

                onViewFarmClick = {

                    val supplierFarmerId =
                        selectedSupplier
                            ?.farmer_id

                    if (
                        !supplierFarmerId
                            .isNullOrBlank()
                    ) {

                        selectedFarmerId =
                            supplierFarmerId

                        navigateTo(
                            TraceabilityScreen
                                .FARMER_FARM_PROFILE
                        )
                    }
                },

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
                    ) {
                            success,
                            _ ->

                        if (
                            success
                        ) {

                            /*
                             * Clear Flow-owned supplier state.
                             *
                             * SupplierScreen leaves composition after
                             * this and therefore all its local text
                             * fields are recreated blank next time.
                             */
                            selectedSupplierName =
                                ""

                            returnToAnimalRecordAfterSave()
                        }
                    }
                }
            )
        }


        TraceabilityScreen
            .LOCATION_FEED -> {

            val database =
                DatabaseProvider
                    .getDatabase()


            var destinationOptions by
                remember(
                    database
                ) {

                    mutableStateOf(
                        emptyList<String>()
                    )
                }


            var rationOptions by
                remember(
                    database
                ) {

                    mutableStateOf(
                        emptyList<String>()
                    )
                }


            LaunchedEffect(
                selectedAnimalReference,
                database
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {

                    onLoadLocationFeed(
                        selectedAnimalReference
                    )
                }


                if (
                    database != null
                ) {

                    val lookupRepository =
                        TraceabilityLookupRepository(
                            database
                        )


                    destinationOptions =
                        lookupRepository
                            .getDestinationOptions()


                    rationOptions =
                        lookupRepository
                            .getRationOptions()

                } else {

                    destinationOptions =
                        emptyList()

                    rationOptions =
                        emptyList()
                }
            }


            LocationFeedScreen(
                animalReference =
                    selectedTagNumber
                        .ifBlank {
                            selectedAnimalReference
                        },

                destinationOptions =
                    destinationOptions,

                rationOptions =
                    rationOptions,

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
                    ) {
                            success,
                            _ ->

                        if (
                            success
                        ) {
                            returnToAnimalRecordAfterSave()
                        }
                    }
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
                    selectedTagNumber
                        .ifBlank {
                            selectedAnimalReference
                        },

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
                    ) {
                            success,
                            _ ->

                        if (
                            success
                        ) {
                            returnToAnimalRecordAfterSave()
                        }
                    }
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
                    selectedTagNumber
                        .ifBlank {
                            selectedAnimalReference
                        },

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

                otherCost =
                    "%.2f".format(
                        otherCost
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

            val database =
                DatabaseProvider
                    .getDatabase()

            var mortalityWorkerOptions by
                remember(
                    database,
                    selectedAnimalReference
                ) {
                    mutableStateOf(
                        emptyList<String>()
                    )
                }

            LaunchedEffect(
                database,
                selectedAnimalReference,
                mortalityRecords
            ) {

                if (
                    selectedAnimalReference
                        .isNotBlank()
                ) {
                    onLoadMortalities(
                        selectedAnimalReference
                    )
                }

                val localUsers =
                    try {

                        database
                            ?.userDao()
                            ?.getAllUsers()
                            .orEmpty()
                            .map {
                                it.username.trim()
                            }
                            .filter {
                                it.isNotBlank()
                            }

                    } catch (
                        _: Exception
                    ) {

                        emptyList()
                    }

                val previousWorkers =
                    mortalityRecords
                        .map {
                            it.responsibleWorker
                                .trim()
                        }
                        .filter {
                            it.isNotBlank()
                        }

                mortalityWorkerOptions =
                    (
                        localUsers +
                            previousWorkers
                    )
                        .distinctBy {
                            it.lowercase()
                        }
                        .sortedBy {
                            it.lowercase()
                        }
            }

            MortalityScreen(
                animalReference =
                    selectedTagNumber
                        .ifBlank {
                            selectedAnimalReference
                        },

                workerOptions =
                    mortalityWorkerOptions,

                mortalityRecords =
                    mortalityRecords,

                mortalityCount =
                    mortalityRecords.size,

                onBackClick = {
                    navigateBack()
                },

                onSaveClick = {
                        mortalityReason,
                        responsibleWorker ->

                    onSaveMortality(
                        selectedAnimalReference,
                        mortalityReason,
                        responsibleWorker
                    ) {
                            success,
                            _ ->

                        if (
                            success
                        ) {
                            returnToAnimalRecordAfterSave()
                        }
                    }
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
