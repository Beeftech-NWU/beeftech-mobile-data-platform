package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CalfRegistration
import com.example.ui.components.BeeftechHeaderBar
import com.example.ui.components.UserRoleDialog
import com.example.ui.screens.CalfDetailScreen
import com.example.ui.screens.CalfRegistrationScreen
import com.example.ui.screens.CalfRegistryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OfficeReportsScreen
import com.example.ui.screens.SyncStatusScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CalfViewModel

enum class AppDestination(val title: String) {
    HOME("Home"),
    REGISTER("Register"),
    REGISTRY("Registry"),
    SYNC("Sync"),
    REPORTS("Reports")
}

class MainActivity : ComponentActivity() {
    private val calfViewModel: CalfViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BeeftechApp(
                    calfViewModel = calfViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

@Composable
fun BeeftechApp(
    calfViewModel: CalfViewModel,
    authViewModel: AuthViewModel
) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var showUserRoleDialog by remember { mutableStateOf(false) }

    val userSession by authViewModel.currentSession.collectAsStateWithLifecycle()
    val allCalves by calfViewModel.allCalves.collectAsStateWithLifecycle()
    val pendingCalves by calfViewModel.pendingCalves.collectAsStateWithLifecycle()
    val pendingCount by calfViewModel.pendingCount.collectAsStateWithLifecycle()
    val syncedCount by calfViewModel.syncedCount.collectAsStateWithLifecycle()
    val totalCount by calfViewModel.totalCount.collectAsStateWithLifecycle()
    val formState by calfViewModel.formState.collectAsStateWithLifecycle()
    val isSyncing by calfViewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncResult by calfViewModel.lastSyncResult.collectAsStateWithLifecycle()
    val syncLogs by calfViewModel.syncLogs.collectAsStateWithLifecycle()
    val backendEndpoint by calfViewModel.backendEndpoint.collectAsStateWithLifecycle()
    val filteredCalves by calfViewModel.filteredCalves.collectAsStateWithLifecycle()
    val searchQuery by calfViewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by calfViewModel.statusFilter.collectAsStateWithLifecycle()
    val selectedBreedFilter by calfViewModel.selectedBreedFilter.collectAsStateWithLifecycle()
    val selectedCalf by calfViewModel.selectedCalf.collectAsStateWithLifecycle()
    val knownDamIds by calfViewModel.knownDamIds.collectAsStateWithLifecycle()
    val knownSireIds by calfViewModel.knownSireIds.collectAsStateWithLifecycle()

    if (showUserRoleDialog) {
        UserRoleDialog(
            currentSession = userSession,
            onRoleSelected = { role ->
                authViewModel.switchRole(role)
            },
            onDismiss = { showUserRoleDialog = false }
        )
    }

    Scaffold(
        topBar = {
            BeeftechHeaderBar(
                userSession = userSession,
                pendingCount = pendingCount,
                onUserClick = { showUserRoleDialog = true },
                onSyncClick = { currentDestination = AppDestination.SYNC }
            )
        },
        bottomBar = {
            if (selectedCalf == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding().testTag("bottom_nav_bar")
                ) {
                    // Home
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.HOME,
                        onClick = { currentDestination = AppDestination.HOME },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )

                    // Register
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.REGISTER,
                        onClick = { currentDestination = AppDestination.REGISTER },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.REGISTER) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                                contentDescription = "Register",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Register", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_register")
                    )

                    // Registry
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.REGISTRY,
                        onClick = { currentDestination = AppDestination.REGISTRY },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.REGISTRY) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted,
                                contentDescription = "Registry"
                            )
                        },
                        label = { Text("Registry", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_registry")
                    )

                    // Sync
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.SYNC,
                        onClick = { currentDestination = AppDestination.SYNC },
                        icon = {
                            if (pendingCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.Black
                                        ) {
                                            Text("$pendingCount", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentDestination == AppDestination.SYNC) Icons.Filled.Sync else Icons.Outlined.Sync,
                                        contentDescription = "Sync"
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (currentDestination == AppDestination.SYNC) Icons.Filled.Sync else Icons.Outlined.Sync,
                                    contentDescription = "Sync"
                                )
                            }
                        },
                        label = { Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_sync")
                    )

                    // Reports
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.REPORTS,
                        onClick = { currentDestination = AppDestination.REPORTS },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.REPORTS) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = "Reports"
                            )
                        },
                        label = { Text("Reports", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_reports")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedCalf != null) {
                CalfDetailScreen(
                    calf = selectedCalf!!,
                    onNavigateBack = { calfViewModel.selectCalf(null) },
                    onMarkPending = { calfId ->
                        calfViewModel.reSyncRecord(calfId)
                        calfViewModel.selectCalf(null)
                    }
                )
            } else {
                when (currentDestination) {
                    AppDestination.HOME -> {
                        HomeScreen(
                            userSession = userSession,
                            pendingCount = pendingCount,
                            syncedCount = syncedCount,
                            totalCount = totalCount,
                            recentCalves = allCalves,
                            onNavigateToRegister = { currentDestination = AppDestination.REGISTER },
                            onNavigateToRegistry = { currentDestination = AppDestination.REGISTRY },
                            onNavigateToSync = { currentDestination = AppDestination.SYNC },
                            onNavigateToReports = { currentDestination = AppDestination.REPORTS },
                            onCalfSelected = { calf -> calfViewModel.selectCalf(calf) },
                            onQuickSync = { calfViewModel.triggerSync() }
                        )
                    }

                    AppDestination.REGISTER -> {
                        CalfRegistrationScreen(
                            formState = formState,
                            userSession = userSession,
                            knownDamIds = knownDamIds,
                            knownSireIds = knownSireIds,
                            onAnimalIdChanged = { calfViewModel.onAnimalIdChanged(it) },
                            onBirthDateChanged = { calfViewModel.onBirthDateChanged(it) },
                            onBreedChanged = { calfViewModel.onBreedChanged(it) },
                            onSexChanged = { calfViewModel.onSexChanged(it) },
                            onBirthWeightChanged = { calfViewModel.onBirthWeightChanged(it) },
                            onCalvingEaseChanged = { calfViewModel.onCalvingEaseChanged(it) },
                            onVigorChanged = { calfViewModel.onVigorChanged(it) },
                            onHornStatusChanged = { calfViewModel.onHornStatusChanged(it) },
                            onPastureLocationChanged = { calfViewModel.onPastureLocationChanged(it) },
                            onRfidTagChanged = { calfViewModel.onRfidTagChanged(it) },
                            onGenerateMockRfid = { calfViewModel.generateMockRfid() },
                            onDamIdChanged = { calfViewModel.onDamIdChanged(it) },
                            onSireIdChanged = { calfViewModel.onSireIdChanged(it) },
                            onPhotoSelected = { calfViewModel.onPhotoSelected(it) },
                            onSubmit = { calfViewModel.submitRegistration(userSession.deviceId) },
                            onClearSuccess = { calfViewModel.clearSubmissionSuccess() },
                            onNavigateBack = { currentDestination = AppDestination.HOME },
                            onNavigateToRegistry = {
                                calfViewModel.clearSubmissionSuccess()
                                currentDestination = AppDestination.REGISTRY
                            }
                        )
                    }

                    AppDestination.REGISTRY -> {
                        CalfRegistryScreen(
                            calves = filteredCalves,
                            searchQuery = searchQuery,
                            statusFilter = statusFilter,
                            selectedBreedFilter = selectedBreedFilter,
                            onSearchQueryChanged = { calfViewModel.setSearchQuery(it) },
                            onStatusFilterChanged = { calfViewModel.setStatusFilter(it) },
                            onBreedFilterChanged = { calfViewModel.setBreedFilter(it) },
                            onCalfSelected = { calf -> calfViewModel.selectCalf(calf) },
                            onNavigateToRegister = { currentDestination = AppDestination.REGISTER },
                            onNavigateBack = { currentDestination = AppDestination.HOME },
                            onExportCsv = { calfViewModel.getExportCsvString() }
                        )
                    }

                    AppDestination.SYNC -> {
                        SyncStatusScreen(
                            pendingCount = pendingCount,
                            syncedCount = syncedCount,
                            isSyncing = isSyncing,
                            lastSyncResult = lastSyncResult,
                            syncLogs = syncLogs,
                            backendEndpoint = backendEndpoint,
                            onEndpointChanged = { calfViewModel.setBackendEndpoint(it) },
                            onTriggerSync = { calfViewModel.triggerSync() },
                            onNavigateBack = { currentDestination = AppDestination.HOME }
                        )
                    }

                    AppDestination.REPORTS -> {
                        OfficeReportsScreen(
                            allCalves = allCalves,
                            userSession = userSession,
                            onExportCsv = { calfViewModel.getExportCsvString() },
                            onNavigateBack = { currentDestination = AppDestination.HOME }
                        )
                    }
                }
            }
        }
    }
}
