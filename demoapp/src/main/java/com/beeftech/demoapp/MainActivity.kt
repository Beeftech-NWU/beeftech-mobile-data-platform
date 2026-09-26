package com.beeftech.demoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.beeftech.calfregistration.ui.CalfRegistrationFlow
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModelFactory
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.DatabaseResult
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.CalfRegistrationEntity
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.repository.SyncRepository
import kotlinx.coroutines.flow.firstOrNull
import com.beeftech.authentication.data.AuthApiClient
import com.beeftech.authentication.data.AuthRepository
import com.beeftech.authentication.data.EncryptedDeviceIdProvider
import com.beeftech.authentication.data.EncryptedSessionStore
import com.beeftech.authentication.ui.AuthGate
import com.beeftech.authentication.viewmodel.LoginViewModelFactory
import com.beeftech.database.security.PinLockoutManager
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.demoapp.ui.theme.BeeftechTheme
import com.beeftech.farmerregistration.ClientDetailsScreen
import com.beeftech.farmerregistration.FarmerSyncScheduler
import com.beeftech.feedcrib.ui.FeedCribFlow
import com.beeftech.farmtraceability.data.TreatmentApiClient
import com.beeftech.farmtraceability.data.TreatmentRepository
import com.beeftech.farmtraceability.ui.FarmTraceabilityFlow
import com.beeftech.farmtraceability.viewmodel.AnimalMovementViewModel
import com.beeftech.farmtraceability.viewmodel.AnimalMovementViewModelFactory
import com.beeftech.farmtraceability.viewmodel.CostSummaryViewModel
import com.beeftech.farmtraceability.viewmodel.CostSummaryViewModelFactory
import com.beeftech.farmtraceability.viewmodel.LocationFeedViewModel
import com.beeftech.farmtraceability.viewmodel.LocationFeedViewModelFactory
import com.beeftech.farmtraceability.viewmodel.MortalityViewModel
import com.beeftech.farmtraceability.viewmodel.MortalityViewModelFactory
import com.beeftech.farmtraceability.viewmodel.SupplierViewModel
import com.beeftech.farmtraceability.viewmodel.SupplierViewModelFactory
import com.beeftech.farmtraceability.viewmodel.TreatmentViewModel
import com.beeftech.farmtraceability.viewmodel.TreatmentViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        /*
         * Temporary passphrase for demo/testing only.
         *
         * Later this should come from the BeefTech
         * Keystore / StrongBox implementation.
         */
        val passphrase =
            "beeftech-demo-passphrase"
                .toByteArray(Charsets.UTF_8)

        lifecycleScope.launch {

            /*
             * Initialise encrypted SQLCipher database
             * away from the main UI thread.
             */
            val databaseResult =
                withContext(Dispatchers.IO) {

                    DatabaseProvider.initialize(
                        context = applicationContext,
                        passphrase = passphrase
                    )
                }

            when (databaseResult) {

                is DatabaseResult.Success -> {

                    val database =
                        databaseResult.database

                    /*
                     * Authentication setup
                     */
                    val sessionStore =
                        EncryptedSessionStore(applicationContext)

                    TokenProviderRegistry.register(sessionStore)

                    val authRepository =
                        AuthRepository(
                            apiClient =
                                AuthApiClient(),
                            sessionStore =
                                sessionStore,
                            userDao =
                                database.userDao(),
                            lockoutManager =
                                PinLockoutManager(applicationContext),
                            deviceIdProvider =
                                EncryptedDeviceIdProvider(applicationContext)
                        )

                    val loginViewModelFactory =
                        LoginViewModelFactory(authRepository)

                    /*
                     * TEMPORARY DEMO DATA
                     *
                     * Create TEST-001 only if it does not
                     * already exist.
                     */
                    withContext(Dispatchers.IO) {

                        val animalDao =
                            database.animalDao()

                        val calfRegistrationDao =
                            database.calfRegistrationDao()

                        val existingAnimal =
                            animalDao.getById("TEST-001")
                                ?: calfRegistrationDao.getCalfRegistrationDetails(
                                    "TEST-001"
                                ).firstOrNull()

                        if (existingAnimal == null) {

                            val demoAnimalRecord =
                                Animal(
                                    animalId = "TEST-001",
                                    tagNumber = "TAG-001",
                                    birthdate = System.currentTimeMillis(),
                                    breed = "Bonsmara",
                                    gpsLat = -26.0,
                                    gpsLng = 28.0,
                                    captureAt = System.currentTimeMillis(),
                                    deviceId = "demo-device",
                                    recordguid = "guid-test-001"
                                )

                            animalDao.insert(demoAnimalRecord)

                            val demoAnimal =
                                CalfRegistrationEntity(
                                    registeredAnimalId = "TEST-001",
                                    damId = null,
                                    sireId = null,
                                    registrationDate = "2026-09-18"
                                )

                            calfRegistrationDao.insertCalfRegistration(
                                demoAnimal
                            )
                        }
                    }

                    /*
                     * Shared Pending Sync Repository
                     *
                     * Calf Registration and Animal Movement
                     * use the same encrypted pending-sync queue.
                     */
                    val pendingSyncRepository =
                        PendingSyncRepository(
                            database.pendingSyncDao()
                        )

                    /*
                     * Animal Movement setup
                     */
                    val animalMovementDao =
                        database.animalMovementDao()

                    val movementViewModelFactory =
                        AnimalMovementViewModelFactory(
                            animalMovementDao =
                                animalMovementDao,
                            context =
                                applicationContext
                        )

                    val movementViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            movementViewModelFactory
                        )[AnimalMovementViewModel::class.java]

                    /*
                     * Treatment setup
                     */
                    val treatmentDao =
                        database.treatmentDao()

                    val treatmentApiClient =
                        TreatmentApiClient(
                            tokenProvider =
                                sessionStore
                        )

                    val treatmentRepository =
                        TreatmentRepository(
                            treatmentDao =
                                treatmentDao,
                            pendingSyncRepository =
                                pendingSyncRepository,
                            apiClient =
                                treatmentApiClient
                        )

                    val treatmentViewModelFactory =
                        TreatmentViewModelFactory(
                            repository =
                                treatmentRepository,
                            applicationContext =
                                applicationContext
                        )

                    val treatmentViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            treatmentViewModelFactory
                        )[TreatmentViewModel::class.java]

                    /*
                     * Mortality setup
                     */
                    val mortalityDao =
                        database.mortalityDao()

                    val mortalityViewModelFactory =
                        MortalityViewModelFactory(
                            mortalityDao =
                                mortalityDao
                        )

                    val mortalityViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            mortalityViewModelFactory
                        )[MortalityViewModel::class.java]

                    /*
                     * Animal Cost DAO
                     */
                    val animalCostDao =
                        database.animalCostDao()

                    /*
                     * Cost Summary setup
                     *
                     * Uses Treatment and
                     * additional Animal Cost records.
                     */
                    val costSummaryViewModelFactory =
                        CostSummaryViewModelFactory(
                            animalCostDao =
                                animalCostDao,
                            costTypeDao =
                                database.costTypeDao()
                        )

                    val costSummaryViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            costSummaryViewModelFactory
                        )[CostSummaryViewModel::class.java]

                    /*
                     * Supplier setup
                     */
                    val animalPurchaseDao =
                        database.animalPurchaseDao()

                    val supplierViewModelFactory =
                        SupplierViewModelFactory(
                            animalPurchaseDao =
                                animalPurchaseDao
                        )

                    val supplierViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            supplierViewModelFactory
                        )[SupplierViewModel::class.java]

                    /*
                     * Location & Feed setup
                     */
                    val locationFeedViewModelFactory =
                        LocationFeedViewModelFactory(
                            animalMovementDao =
                                animalMovementDao
                        )

                    val locationFeedViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            locationFeedViewModelFactory
                        )[LocationFeedViewModel::class.java]

                    /*
                     * Calf Registration setup
                     */
                    val calfRegistrationViewModelFactory =
                        CalfRegistrationViewModelFactory(
                            context = applicationContext,
                            calfRegistrationDao =
                                database.calfRegistrationDao(),
                            pendingSyncRepository =
                                pendingSyncRepository,
                            tokenProvider =
                                sessionStore
                        )

                    val calfRegistrationViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            calfRegistrationViewModelFactory
                        )[CalfRegistrationViewModel::class.java]

                    val syncRepository =
                        SyncRepository(
                            pendingSyncDao =
                                database.pendingSyncDao(),
                            syncBatchDao =
                                database.syncBatchDao()
                        )

                    /*
                     * Start demo UI
                     */
                    setContent {

                        BeeftechTheme {

                            AuthGate(
                                sessionStore = sessionStore,
                                viewModelFactory = loginViewModelFactory
                            ) { loggedInUser ->

                                val movementRecords by
                                movementViewModel
                                    .movements
                                    .collectAsState()

                            val treatmentRecords by
                            treatmentViewModel
                                .treatments
                                .collectAsState()

                            val diseaseOptions by
                            treatmentViewModel
                                .diseaseOptions
                                .collectAsState()

                            val treatmentOptions by
                            treatmentViewModel
                                .treatmentOptions
                                .collectAsState()

                            val mortalityRecords by
                            mortalityViewModel
                                .mortalities
                                .collectAsState()

                            val costSummaryState by
                            costSummaryViewModel
                                .uiState
                                .collectAsState()

                            val supplierRecords by
                            supplierViewModel
                                .suppliers
                                .collectAsState()

                            val locationFeedRecords by
                            locationFeedViewModel
                                .records
                                .collectAsState()

                            var selectedDemoTab by
                            remember {
                                mutableIntStateOf(0)
                            }

                            Scaffold(
                                modifier =
                                    Modifier.fillMaxSize(),

                                topBar = {

                                    PrimaryTabRow(
                                        selectedTabIndex =
                                            selectedDemoTab,
                                        modifier =
                                            Modifier.statusBarsPadding()
                                    ) {

                                        Tab(
                                            selected =
                                                selectedDemoTab == 0,
                                            onClick = {
                                                selectedDemoTab = 0
                                            },
                                            text = {
                                                Text(
                                                    "Farm Traceability"
                                                )
                                            }
                                        )

                                        Tab(
                                            selected =
                                                selectedDemoTab == 1,
                                            onClick = {
                                                selectedDemoTab = 1
                                            },
                                            text = {
                                                Text(
                                                    "Calf Registration"
                                                )
                                            }
                                        )

                                        Tab(
                                            selected =
                                                selectedDemoTab == 2,
                                            onClick = {
                                                selectedDemoTab = 2
                                            },
                                            text = {
                                                Text(
                                                    "Feed Crib"
                                                )
                                            }
                                        )
                                    }
                                }
                            ) { innerPadding ->

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                ) {

                                    if (selectedDemoTab == 1) {

                                        CalfRegistrationFlow(
                                            viewModel =
                                                calfRegistrationViewModel
                                        )

                                    } else if (selectedDemoTab == 0) {

                                        FarmTraceabilityFlow(

                                            onFarmerRegistrationClick = {

                                                val intent =
                                                    Intent(
                                                        this@MainActivity,
                                                        ClientDetailsScreen::class.java
                                                    )

                                                startActivity(intent)
                                            },

                                            movementRecords =
                                                movementRecords,

                                            onLoadMovements = {
                                                    animalId: String ->

                                                movementViewModel
                                                    .loadMovements(
                                                        animalId
                                                    )
                                            },

                                            onSaveMovement = {
                                                    animalId,
                                                    movementInformation,
                                                    responsibleWorker ->

                                                movementViewModel
                                                    .saveMovement(
                                                        animalId =
                                                            animalId,
                                                        movementInformation =
                                                            movementInformation,
                                                        responsibleWorker =
                                                            responsibleWorker,
                                                        onResult = {
                                                                _,
                                                                message ->

                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                message,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                            },

                                            treatmentRecords =
                                                treatmentRecords,

                                            diseaseOptions =
                                                diseaseOptions,

                                            treatmentOptions =
                                                treatmentOptions,

                                            onLoadTreatments = {
                                                    animalId: String ->

                                                treatmentViewModel
                                                    .loadTreatments(
                                                        animalId
                                                    )
                                            },

                                            onSaveTreatment = {
                                                    animalId,
                                                    disease,
                                                    treatment,
                                                    batchNumber,
                                                    volumeUsed,
                                                    cost ->

                                                treatmentViewModel
                                                    .saveTreatment(
                                                        animalId =
                                                            animalId,
                                                        disease =
                                                            disease,
                                                        treatmentName =
                                                            treatment,
                                                        batchNumber =
                                                            batchNumber,
                                                        volumeUsed =
                                                            volumeUsed,
                                                        costText =
                                                            cost,
                                                        onResult = {
                                                                _,
                                                                message ->

                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                message,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                            },

                                            mortalityRecords =
                                                mortalityRecords,

                                            onLoadMortalities = {
                                                    animalId: String ->

                                                mortalityViewModel
                                                    .loadMortalities(
                                                        animalId
                                                    )
                                            },

                                            onSaveMortality = {
                                                    animalId,
                                                    mortalityReason,
                                                    responsibleWorker ->

                                                mortalityViewModel
                                                    .saveMortality(
                                                        animalId =
                                                            animalId,
                                                        mortalityReason =
                                                            mortalityReason,
                                                        responsibleWorker =
                                                            responsibleWorker,
                                                        onResult = {
                                                                _,
                                                                message ->

                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                message,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                            },

                                            transportCost =
                                                costSummaryState.transportCost,

                                            processingCost =
                                                costSummaryState.processingCost,

                                            treatmentCost =
                                                costSummaryState.treatmentCost,

                                            feedCost =
                                                costSummaryState.feedCost,

                                            handlingCost =
                                                costSummaryState.handlingCost,

                                            interestCost =
                                                costSummaryState.interestCost,

                                            otherCost =
                                                costSummaryState.otherCost,

                                            totalAnimalCost =
                                                costSummaryState.totalAnimalCost,

                                            onLoadCostSummary = {
                                                    animalId: String ->

                                                costSummaryViewModel
                                                    .loadCostSummary(
                                                        animalId
                                                    )
                                            },

                                            supplierRecords =
                                                supplierRecords,

                                            onLoadSuppliers = {
                                                    animalId: String ->

                                                supplierViewModel
                                                    .loadSuppliers(
                                                        animalId
                                                    )
                                            },

                                            onSaveSupplier = {
                                                    animalId,
                                                    supplierName,
                                                    glnNumber,
                                                    purchaseDate,
                                                    purchaseBatchNumber ->

                                                supplierViewModel
                                                    .saveSupplier(
                                                        animalId =
                                                            animalId,
                                                        supplierName =
                                                            supplierName,
                                                        glnNumber =
                                                            glnNumber,
                                                        purchaseDate =
                                                            purchaseDate,
                                                        purchaseBatchNumber =
                                                            purchaseBatchNumber,
                                                        onResult = {
                                                                _,
                                                                message ->

                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                message,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                            },

                                            locationFeedRecords =
                                                locationFeedRecords,

                                            onLoadLocationFeed = {
                                                    animalId: String ->

                                                locationFeedViewModel
                                                    .loadRecords(
                                                        animalId
                                                    )
                                            },

                                            onSaveLocationFeed = {
                                                    animalId,
                                                    destination,
                                                    daysInDestination,
                                                    rationName,
                                                    rationDays,
                                                    rationCost ->

                                                locationFeedViewModel
                                                    .saveRecord(
                                                        animalId =
                                                            animalId,
                                                        destination =
                                                            destination,
                                                        daysInDestinationText =
                                                            daysInDestination,
                                                        rationName =
                                                            rationName,
                                                        rationDaysText =
                                                            rationDays,
                                                        rationCostText =
                                                            rationCost,
                                                        onResult = {
                                                                _,
                                                                message ->

                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                message,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                            },

                                            onRetrySyncClick = {

                                                lifecycleScope.launch {

                                                    val pendingOperations =
                                                        withContext(Dispatchers.IO) {
                                                            pendingSyncRepository
                                                                .getAllPendingOperations()
                                                        }

                                                    if (pendingOperations.isEmpty()) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "There are no pending records to sync.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        return@launch
                                                    }

                                                    val pendingTypes =
                                                        pendingOperations
                                                            .map { it.entityType }
                                                            .toSet()

                                                    if (
                                                        "FARMER_REGISTRATION" in pendingTypes
                                                    ) {
                                                        FarmerSyncScheduler.enqueue(
                                                            applicationContext
                                                        )
                                                    }

                                                    if (
                                                        "CALF_REGISTRATION" in pendingTypes
                                                    ) {
                                                        calfRegistrationViewModel
                                                            .retrySync {
                                                                    success,
                                                                    message ->

                                                                if (
                                                                    success &&
                                                                    message.contains(
                                                                        "synced successfully",
                                                                        ignoreCase = true
                                                                    )
                                                                ) {
                                                                    lifecycleScope.launch {
                                                                        syncRepository
                                                                            .recordSuccessfulSync()
                                                                    }
                                                                }

                                                                Toast.makeText(
                                                                    this@MainActivity,
                                                                    message,
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                    }

                                                    if (
                                                        "ANIMAL_MOVEMENT" in pendingTypes
                                                    ) {
                                                        movementViewModel
                                                            .retrySync(
                                                                animalId = ""
                                                            ) {
                                                                    _,
                                                                    message ->

                                                                Toast.makeText(
                                                                    this@MainActivity,
                                                                    message,
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                    }

                                                    if (
                                                        "FARMER_REGISTRATION" in pendingTypes
                                                    ) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Farmer registration sync queued.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        )

                                    } else {

                                        FeedCribFlow(
                                            onBackToHome = {
                                                selectedDemoTab = 0
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }

                is DatabaseResult.Error -> {

                    setContent {

                        BeeftechTheme {

                            Scaffold(
                                modifier =
                                    Modifier.fillMaxSize()
                            ) { innerPadding ->

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                ) {

                                    val errorDetail = buildString {
                                        append("Database error: ")
                                        append(databaseResult.message)
                                        databaseResult.cause?.let { cause ->
                                            append("\n\nCause: ")
                                            append(cause.message ?: cause.toString())
                                            append("\n\n")
                                            append(cause.stackTraceToString())
                                        }
                                    }

                                    Text(
                                        text = errorDetail
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        DatabaseProvider.close()
    }
}
