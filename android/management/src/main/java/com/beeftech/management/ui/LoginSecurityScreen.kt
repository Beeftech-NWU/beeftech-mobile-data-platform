package com.beeftech.management.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.LOGIN_OUTCOMES
import com.beeftech.management.data.Lockout
import com.beeftech.management.data.LoginEvent
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.loginOutcomeLabel
import com.beeftech.management.viewmodel.LoginSecurityViewModel
import com.beeftech.management.viewmodel.LoginSecurityViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun LoginSecurityTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: LoginSecurityViewModel = viewModel(
        key = "login-security-$currentUserId",
        factory = LoginSecurityViewModelFactory(apiClient)
    )

    LoginSecurityScreen(viewModel = viewModel, modifier = modifier)
}

/** Online-only, admin only: who is locked out of signing in right now, and every sign-in attempt. */
@Composable
fun LoginSecurityScreen(
    viewModel: LoginSecurityViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Login security", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        if (state.needsConnection) {
            NeedsConnectionNotice("Login security")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.outcome == null,
                onClick = { viewModel.selectOutcome(null) },
                label = { Text("All attempts") }
            )
            LOGIN_OUTCOMES.forEach { (outcome, label) ->
                FilterChip(
                    selected = state.outcome == outcome,
                    onClick = { viewModel.selectOutcome(outcome) },
                    label = { Text(label) }
                )
            }
        }

        if (state.loading && state.events.isEmpty() && state.lockouts.isEmpty()) {
            CircularProgressIndicator()
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Locked out now", style = MaterialTheme.typography.titleMedium)
            }
            if (state.lockouts.isEmpty() && !state.loading) {
                item { Text("Nobody is locked out.") }
            }
            items(state.lockouts, key = { "lock-${it.username}" }) { lockout ->
                LockoutCard(lockout, onUnlock = { viewModel.unlock(lockout) })
            }

            item {
                Text("Sign-in attempts", style = MaterialTheme.typography.titleMedium)
            }
            if (state.events.isEmpty() && !state.loading && !state.needsConnection && state.error == null) {
                item { Text("Nothing recorded for this filter.") }
            }
            items(state.events, key = { "event-${it.id}" }) { event -> EventCard(event) }

            if (state.canLoadMore) {
                item {
                    OutlinedButton(
                        onClick = viewModel::loadMore,
                        enabled = !state.loadingMore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.loadingMore) "Loading…" else "Load more")
                    }
                }
            }
        }
    }
}

@Composable
private fun LockoutCard(lockout: Lockout, onUnlock: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(lockout.username, style = MaterialTheme.typography.titleMedium)
            Text(
                "${lockout.failedAttempts} wrong PINs · locked until ${formatSecurityTime(lockout.lockedUntil)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (lockout.userId != null) {
                TextButton(onClick = onUnlock) { Text("Unlock") }
            } else {
                Text("Not a real user, so nothing to unlock.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EventCard(event: LoginEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(loginOutcomeLabel(event.outcome), style = MaterialTheme.typography.titleSmall)
            Text(
                "${event.usernameAttempted} · ${formatSecurityTime(event.createdAt)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                buildString {
                    append(event.deviceId)
                    event.appVersion?.let { append(" · v$it") }
                    event.siteId?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatSecurityTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
