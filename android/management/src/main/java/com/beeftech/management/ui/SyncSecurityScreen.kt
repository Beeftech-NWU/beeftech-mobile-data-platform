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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.LockedAccount
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.SECURITY_EVENT_TYPES
import com.beeftech.management.data.SecurityEventRow
import com.beeftech.management.data.securityEventLabel
import com.beeftech.management.viewmodel.SyncSecurityViewModel
import com.beeftech.management.viewmodel.SyncSecurityViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun SyncSecurityTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: SyncSecurityViewModel = viewModel(
        key = "sync-security-$currentUserId",
        factory = SyncSecurityViewModelFactory(apiClient)
    )

    SyncSecurityScreen(viewModel = viewModel, modifier = modifier)
}

/** Online-only, admin only: accounts locked by the 7-day rule, and what the phones reported. */
@Composable
fun SyncSecurityScreen(
    viewModel: SyncSecurityViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var confirming by remember { mutableStateOf<LockedAccount?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    confirming?.let { account ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text("Clear the lock for ${account.username}?") },
            text = {
                Text(
                    "They can sign in and capture again once their phone connects. " +
                        "Data wiped on day 7 does not come back."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLock(account)
                    confirming = null
                }) { Text("Clear lock") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text("Cancel") }
            }
        )
    }

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
            Text("Sync security", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        if (state.needsConnection) {
            NeedsConnectionNotice("Sync security")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.eventType == null,
                onClick = { viewModel.selectEventType(null) },
                label = { Text("All events") }
            )
            SECURITY_EVENT_TYPES.forEach { (type, label) ->
                FilterChip(
                    selected = state.eventType == type,
                    onClick = { viewModel.selectEventType(type) },
                    label = { Text(label) }
                )
            }
        }

        if (state.loading && state.events.isEmpty() && state.locked.isEmpty()) {
            CircularProgressIndicator()
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Locked accounts", style = MaterialTheme.typography.titleMedium)
            }
            if (state.locked.isEmpty() && !state.loading) {
                item { Text("No account is locked.") }
            }
            items(state.locked, key = { "locked-${it.userId}" }) { account ->
                LockedCard(account, onClear = { confirming = account })
            }

            item {
                Text("Reported by phones", style = MaterialTheme.typography.titleMedium)
            }
            if (state.events.isEmpty() && !state.loading && !state.needsConnection && state.error == null) {
                item { Text("Nothing reported for this filter.") }
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
private fun LockedCard(account: LockedAccount, onClear: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(account.username, style = MaterialTheme.typography.titleMedium)
            Text(
                "Locked ${formatSecurityTime(account.lockedAt)}" + (account.siteId?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            account.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = onClear) { Text("Clear lock") }
        }
    }
}

@Composable
private fun EventCard(event: SecurityEventRow) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(securityEventLabel(event.eventType), style = MaterialTheme.typography.titleSmall)
            Text(
                "${event.username} · ${formatSecurityTime(event.eventTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                buildString {
                    append("${event.pendingCount} unsynced")
                    event.warningDay?.let { append(" · day $it") }
                    append(" · ${event.deviceId}")
                    event.siteId?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatSecurityTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
