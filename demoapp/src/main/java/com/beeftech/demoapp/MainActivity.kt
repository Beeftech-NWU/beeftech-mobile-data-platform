
package com.beeftech.demoapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.DatabaseResult
import com.beeftech.database.entity.CalfRegistration
import com.beeftech.demoapp.ui.theme.BeeftechTheme
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
                     * TEMPORARY DEMO DATA
                     *
                     * Create TEST-001 only if it does not
                     * already exist.
                     */
                    withContext(Dispatchers.IO) {

                        val calfRegistrationDao =
                            database.calfRegistrationDao()

                        val existingAnimal =
                            calfRegistrationDao.findByAnimalId(
                                "TEST-001"
                            )

                        if (existingAnimal == null) {

                            val demoAnimal =
                                CalfRegistration(
                                    animalId = "TEST-001",
                                    birthdate = 1725148800000L,
                                    breed = "Bonsmara",
                                    damId = "DAM-001",
                                    sireId = "SIRE-001",
                                    photoPath = null,
                                    videoPath = null,
                                    gpsLat = -26.2041,
                                    gpsLng = 28.0473,
                                    captureAt = 1725148800000L,
                                    deviceId = "DEMO-DEVICE",
                                    recordguid = "DEMO-GUID-001",
                                    syncStatus = "PENDING",
                                    syncedat = null
                                )

                            calfRegistrationDao.insert(
                                demoAnimal
                            )
                        }
                    }

                    /*
                     * Animal Movement setup
                     */
                    val animalMovementDao =
                        database.animalMovementDao()

                    val movementViewModelFactory =
                        AnimalMovementViewModelFactory(
                            animalMovementDao =
                                animalMovementDao
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

                    val treatmentViewModelFactory =
                        TreatmentViewModelFactory(
                            treatmentDao =
                                treatmentDao
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
                     * Location & Feed DAO
                     *
                     * Shared by Location & Feed and Cost Summary.
                     */
                    val locationFeedDao =
                        database.locationFeedDao()

                    /*
                     * Animal Cost DAO
                     *
                     * Used for Transport, Processing,
                     * Handling and Interest costs.
                     */
                    val animalCostDao =
                        database.animalCostDao()

                    /*
                     * Cost Summary setup
                     *
                     * Uses Treatment, Feed/Ration and
                     * additional Animal Cost records.
                     */
                    val costSummaryViewModelFactory =
                        CostSummaryViewModelFactory(
                            treatmentDao =
                                treatmentDao,

                            locationFeedDao =
                                locationFeedDao,

                            animalCostDao =
                                animalCostDao
                        )

                    val costSummaryViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            costSummaryViewModelFactory
                        )[CostSummaryViewModel::class.java]

                    /*
                     * Supplier setup
                     */
                    val supplierDao =
                        database.supplierDao()

                    val supplierViewModelFactory =
                        SupplierViewModelFactory(
                            supplierDao =
                                supplierDao
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
                            locationFeedDao =
                                locationFeedDao
                        )

                    val locationFeedViewModel =
                        ViewModelProvider(
                            this@MainActivity,
                            locationFeedViewModelFactory
                        )[LocationFeedViewModel::class.java]

                    /*
                     * Start Farm Traceability UI
                     */
                    setContent {

                        BeeftechTheme {

                            /*
                             * Animal Movement history
                             */
                            val movementRecords by
                            movementViewModel
                                .movements
                                .collectAsState()

                            /*
                             * Treatment history
                             */
                            val treatmentRecords by
                            treatmentViewModel
                                .treatments
                                .collectAsState()

                            /*
                             * Mortality history
                             */
                            val mortalityRecords by
                            mortalityViewModel
                                .mortalities
                                .collectAsState()

                            /*
                             * Cost Summary
                             */
                            val costSummaryState by
                            costSummaryViewModel
                                .uiState
                                .collectAsState()

                            /*
                             * Supplier history
                             */
                            val supplierRecords by
                            supplierViewModel
                                .suppliers
                                .collectAsState()

                            /*
                             * Location & Feed history
                             */
                            val locationFeedRecords by
                            locationFeedViewModel
                                .records
                                .collectAsState()

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

                                    FarmTraceabilityFlow(

                                        /*
                                         * Animal Movement
                                         */
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

                                        /*
                                         * Treatments
                                         */
                                        treatmentRecords =
                                            treatmentRecords,

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

                                        /*
                                         * Mortality
                                         */
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

                                        /*
                                         * Cost Summary
                                         */
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

                                        totalAnimalCost =
                                            costSummaryState.totalAnimalCost,

                                        onLoadCostSummary = {
                                                animalId: String ->

                                            costSummaryViewModel
                                                .loadCostSummary(
                                                    animalId
                                                )
                                        },

                                        /*
                                         * Supplier
                                         */
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

                                        /*
                                         * Location & Feed
                                         */
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
                                        }
                                    )
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

                                    Text(
                                        text =
                                            "Database error: " +
                                                    databaseResult.message
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

