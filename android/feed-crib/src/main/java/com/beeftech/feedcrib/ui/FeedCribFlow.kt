package com.beeftech.feedcrib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.beeftech.feedcrib.viewmodel.FeedCribViewModel
import kotlinx.coroutines.launch

private enum class FeedCribScreen {
    HOME,
    DETAIL,
    SESSIONS,
    SESSION_DETAIL
}

@Composable
fun FeedCribFlow(
    viewModel: FeedCribViewModel,
    onBackToHome: () -> Unit = {}
) {
    val cribs by viewModel.cribs.collectAsState()
    val codes by viewModel.codes.collectAsState()
    val lastDownloadedAt by viewModel.lastDownloadedAt.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val setup by viewModel.cribSetup.collectAsState()
    val addingCrib by viewModel.addingCrib.collectAsState()
    val cribErrors by viewModel.cribErrors.collectAsState()
    val setupMessage by viewModel.setupMessage.collectAsState()
    var showAddCrib by rememberSaveable { mutableStateOf(false) }
    var conflictUuid by rememberSaveable { mutableStateOf<String?>(null) }

    var screen by remember { mutableStateOf(FeedCribScreen.HOME) }
    var openError by remember { mutableStateOf<String?>(null) }
    var sessionCrib by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun say(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Opening the Feed tab downloads the cribs. Offline, the phone keeps the ones it has.
    LaunchedEffect(Unit) { viewModel.refresh() }

    BackHandler {
        when (screen) {
            FeedCribScreen.HOME -> onBackToHome()
            FeedCribScreen.DETAIL -> Unit // the detail screen handles Back itself, asking first if it changed
            FeedCribScreen.SESSIONS -> screen = FeedCribScreen.HOME
            FeedCribScreen.SESSION_DETAIL -> screen = FeedCribScreen.SESSIONS
        }
    }

    Box {
        when (screen) {
            FeedCribScreen.HOME -> FeedCribHomeScreen(
                cribs = cribs,
                lastDownloadedAt = lastDownloadedAt,
                refreshing = refreshing,
                syncing = syncing,
                syncMessage = syncMessage,
                onResolveConflict = if (viewModel.canAddCrib) { uuid ->
                    viewModel.prepareAddCrib()
                    conflictUuid = uuid
                } else null,
                errorMessage = openError,
                onOpenCrib = { number ->
                    openError = null
                    viewModel.openCrib(number) { found ->
                        if (found) screen = FeedCribScreen.DETAIL
                        else openError = "Crib not found, or the code matches more than one site. Select it from the list."
                    }
                },
                onSessions = {
                    viewModel.loadSessions()
                    screen = FeedCribScreen.SESSIONS
                },
                onSync = { viewModel.retrySync { _, message -> say(message) } },
                onRefresh = { viewModel.refresh { _, message -> say(message) } }
            )

            FeedCribScreen.DETAIL -> detail?.let { state ->
                CribDetailScreen(
                    detail = state,
                    codes = codes,
                    nowMillis = System.currentTimeMillis(),
                    saving = saving,
                    onSelectCode = viewModel::selectCode,
                    onAdjustAdi = viewModel::adjustAdi,
                    onDiscard = {
                        viewModel.discard()
                        screen = FeedCribScreen.HOME
                    },
                    onSave = {
                        viewModel.save { saved, message ->
                            if (saved) screen = FeedCribScreen.HOME
                            say(message)
                        }
                    }
                )
            }

            FeedCribScreen.SESSIONS -> SessionListScreen(
                sessions = sessions,
                onOpen = {
                    sessionCrib = it
                    screen = FeedCribScreen.SESSION_DETAIL
                },
                onBack = { screen = FeedCribScreen.HOME }
            )

            FeedCribScreen.SESSION_DETAIL -> {
                val entries by viewModel.entriesToday(sessionCrib).collectAsState(initial = emptyList())
                SessionDetailScreen(
                    cribNumber = sessionCrib,
                    entries = entries,
                    onBack = { screen = FeedCribScreen.SESSIONS }
                )
            }
        }

        if (screen == FeedCribScreen.HOME && viewModel.canAddCrib) {
            FeedCribTooltip("Add a new crib to the list. This does not create a reading.", modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                FloatingActionButton(
                    onClick = { viewModel.prepareAddCrib(); showAddCrib = true },
                    containerColor = FeedCribColors.Rust
                ) { Icon(Icons.Default.Add, contentDescription = "Add crib") }
            }
        }
        if (showAddCrib) AddCribDialog(
            setup = setup, saving = addingCrib, errors = cribErrors, setupMessage = setupMessage,
            onDismiss = { showAddCrib = false },
            onSave = { form -> viewModel.addCrib(form) {
                showAddCrib = false
                say("Crib added successfully")
            } }
        )
        conflictUuid?.let { uuid ->
            cribs.firstOrNull { it.cribUuid == uuid }?.let { crib ->
                CribConflictDialog(crib, addingCrib, cribErrors,
                    onDismiss = { conflictUuid = null },
                    onSave = { code, tag -> viewModel.resolveCribConflict(uuid, code, tag) {
                        conflictUuid = null
                        say("Conflict updated. Crib and readings are queued for sync.")
                    } })
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
