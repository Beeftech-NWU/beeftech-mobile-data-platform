package com.beeftech.feedcrib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
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
                errorMessage = openError,
                onOpenCrib = { number ->
                    openError = null
                    viewModel.openCrib(number) { found ->
                        if (found) screen = FeedCribScreen.DETAIL
                        else openError = "No crib ${number.trim().uppercase()} on this phone."
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

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
