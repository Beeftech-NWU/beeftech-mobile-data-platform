package com.beeftech.demoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.beeftech.calfregistration.data.CalfCaptureContext
import com.beeftech.calfregistration.data.CalfRegistrationMappers
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.calfregistration.ui.CalfRegistrationFlow
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModelFactory
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.DatabaseResult
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.repository.SyncRepository
import com.beeftech.database.repository.SyncPolicyEnforcer
import com.beeftech.authentication.data.AuthApiClient
import com.beeftech.authentication.data.AuthRepository
import com.beeftech.authentication.data.EncryptedDeviceIdProvider
import com.beeftech.authentication.data.EncryptedSessionStore
import com.beeftech.authentication.viewmodel.LoginViewModelFactory
import com.beeftech.database.security.PinLockoutManager
import com.beeftech.authentication.domain.Role
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.ui.DashboardTab
import com.beeftech.management.ui.MyActivityScreen
import com.beeftech.management.ui.TeamTab
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
import com.beeftech.farmtraceability.data.MortalityApiClient
import com.beeftech.farmtraceability.data.MortalityRepository
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
                     * Seed one demo calf through the same transactional path
                     * real registrations use, only if its tag is not taken.
                     */
                    withContext(Dispatchers.IO) {

                        val calfRegistrationDao =
                            database.calfRegistrationDao()

                        if (calfRegistrationDao.findAnimalIdByTag(DEMO_TAG) == null) {

                            val seed =
                                CalfRegistrationMappers.toNewCalf(
                                    formData =
                                        CalfRegistrationData(
                                            tagNumber = DEMO_TAG,
                                            animalType = "BNM — Bonsmara"
                                        ),
                                    capture =
                                        CalfCaptureContext(
                                            deviceId = "demo-device",
                                            gpsLat = -26.0,
                                            gpsLng = 28.0
                                        ),
                                    damAnimalId = null,
                                    sireAnimalId = null
                                )

                            calfRegistrationDao.registerCalf(
                                seed.animal,
                                seed.identifiers,
                                seed.media,
                                seed.registration
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

                    val managementApiClient =
                        ManagementApiClient(
                            tokenProvider =
                                sessionStore
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
                    val mortalityRepository =
                        MortalityRepository(
                            mortalityDao =
                                database.mortalityDao(),
                            pendingSyncRepository =
                                pendingSyncRepository,
                            apiClient =
                                MortalityApiClient(
                                    tokenProvider =
                                        sessionStore
                                )
                        )

                    val mortalityViewModelFactory =
                        MortalityViewModelFactory(
                            repository =
                                mortalityRepository
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
                                sessionStore,
                            deviceIdProvider = {
                                EncryptedDeviceIdProvider(applicationContext).getDeviceId()
                            }
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
                     * Persistent Day-7 policy evaluator.
                     *
                     * This uses the same Room-backed policy state
                     * tested by SyncPolicyEnforcerTest.
                     */
                    val syncPolicyEnforcer =
                        SyncPolicyEnforcer(
                            pendingSyncDao =
                                database.pendingSyncDao(),

                            syncSecurityDao =
                                database.syncSecurityDao()
                        )

                    /*
                     * Start demo UI
                     */
                    setContent {

                        BeeftechTheme {

                            PolicyAwareAuthGate(
                                sessionStore =
                                    sessionStore,

                                viewModelFactory =
                                    loginViewModelFactory,

                                syncPolicyEnforcer =
                                    syncPolicyEnforcer
                            ) { loggedInUser, onLogout ->

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

                            val tabs =
                                remember(loggedInUser.role) {
                                    tabsFor(loggedInUser.roleEnum)
                                }

                            var showMyActivity by
                            remember {
                                mutableStateOf(false)
                            }

                            val pendingCount by
                            remember(loggedInUser.userId) {
                                pendingSyncRepository
                                    .observePendingCount(loggedInUser.userId)
                            }.collectAsState(initial = 0)

                            val oldestPendingAt by
                            remember(loggedInUser.userId) {
                                pendingSyncRepository
                                    .observeOldestPendingAt(loggedInUser.userId)
                            }.collectAsState(initial = null)

                            var showLogoutDialog by
                            remember {
                                mutableStateOf(false)
                            }

                            if (showLogoutDialog) {

                                AlertDialog(
                                    onDismissRequest = {
                                        showLogoutDialog =
                                            false
                                    },

                                    title = {
                                        Text(
                                            text = "Log out?"
                                        )
                                    },

                                    text = {
                                        Text(
                                            text = "You'll need your username and PIN to sign in again."
                                        )
                                    },

                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showLogoutDialog =
                                                    false
                                                onLogout()
                                            }
                                        ) {
                                            Text(
                                                text = "Log out"
                                            )
                                        }
                                    },

                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showLogoutDialog =
                                                    false
                                            }
                                        ) {
                                            Text(
                                                text = "Cancel"
                                            )
                                        }
                                    }
                                )
                            }

                            Scaffold(
                                modifier =
                                    Modifier.fillMaxSize(),

                                topBar = {

                                    Column(
                                        modifier =
                                            Modifier.statusBarsPadding()
                                    ) {

                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = 16.dp,
                                                        vertical = 4.dp
                                                    ),
                                            horizontalArrangement =
                                                Arrangement.SpaceBetween,
                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {

                                            Text(
                                                text = "Signed in as ${loggedInUser.username}"
                                            )

                                            Row {
                                                TextButton(
                                                    onClick = {
                                                        showMyActivity =
                                                            !showMyActivity
                                                    }
                                                ) {
                                                    Text(
                                                        text = "My activity"
                                                    )
                                                }

                                                TextButton(
                                                    onClick = {
                                                        showLogoutDialog =
                                                            true
                                                    }
                                                ) {
                                                    Text(
                                                        text = "Log out"
                                                    )
                                                }
                                            }
                                        }

                                        val tabContent: @Composable () -> Unit = {

                                            tabs.forEachIndexed { index, tab ->

                                                Tab(
                                                    selected =
                                                        selectedDemoTab == index,
                                                    onClick = {
                                                        selectedDemoTab = index
                                                    },
                                                    text = {
                                                        Text(
                                                            tab.label
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        /*
                                         * Workers keep the evenly spread three tabs. With five, the
                                         * labels no longer fit, so managers and admins scroll.
                                         */
                                        if (tabs.size > 3) {

                                            PrimaryScrollableTabRow(
                                                selectedTabIndex =
                                                    selectedDemoTab,
                                                edgePadding =
                                                    0.dp
                                            ) {
                                                tabContent()
                                            }

                                        } else {

                                            PrimaryTabRow(
                                                selectedTabIndex =
                                                    selectedDemoTab
                                            ) {
                                                tabContent()
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                ) {

                                    val currentTab =
                                        tabs[selectedDemoTab.coerceIn(tabs.indices)]

                                    if (showMyActivity) {

                                        Column {

                                            TextButton(
                                                onClick = {
                                                    showMyActivity =
                                                        false
                                                }
                                            ) {
                                                Text(
                                                    text = "Back"
                                                )
                                            }

                                            MyActivityScreen(
                                                username =
                                                    loggedInUser.username,
                                                role =
                                                    loggedInUser.role,
                                                siteId =
                                                    loggedInUser.siteId,
                                                pendingCount =
                                                    pendingCount,
                                                oldestPendingAt =
                                                    oldestPendingAt
                                            )
                                        }

                                    } else if (currentTab == AppTab.DASHBOARD) {

                                        DashboardTab(
                                            apiClient =
                                                managementApiClient,
                                            currentUserId =
                                                loggedInUser.userId
                                        )

                                    } else if (currentTab == AppTab.TEAM) {

                                        TeamTab(
                                            apiClient =
                                                managementApiClient,
                                            currentUserId =
                                                loggedInUser.userId,
                                            isAdmin =
                                                loggedInUser.roleEnum == Role.ADMIN
                                        )

                                    } else if (currentTab == AppTab.CALF_REGISTRATION) {

                                        CalfRegistrationFlow(
                                            viewModel =
                                                calfRegistrationViewModel
                                        )

                                    } else if (currentTab == AppTab.TRACEABILITY) {

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
                                                        "MORTALITY" in pendingTypes
                                                    ) {
                                                        mortalityViewModel
                                                            .retrySync {
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

private const val DEMO_TAG = "Blu0000001"
