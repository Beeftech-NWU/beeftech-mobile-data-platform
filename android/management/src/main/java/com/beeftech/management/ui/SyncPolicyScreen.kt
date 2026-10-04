package com.beeftech.management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.SyncPolicyRules
import com.beeftech.management.viewmodel.SyncPolicyViewModel
import com.beeftech.management.viewmodel.SyncPolicyViewModelFactory

/* Keyed by user, so a different user logging in on the same device never sees the previous form. */
@Composable
fun SyncPolicyTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: SyncPolicyViewModel = viewModel(
        key = "sync-policy-$currentUserId",
        factory = SyncPolicyViewModelFactory(apiClient)
    )

    SyncPolicyScreen(viewModel = viewModel, modifier = modifier)
}

/**
 * Online-only, admin only: when phones warn about data that hasn't synced, and when the dashboard
 * flags a worker. The day data is wiped is fixed at 7 and can't be changed here.
 */
@Composable
fun SyncPolicyScreen(
    viewModel: SyncPolicyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sync policy", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        if (state.needsConnection) {
            NeedsConnectionNotice("Sync policy")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (state.saved == null) {
            if (state.loading) CircularProgressIndicator()
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sync warnings", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A worker sees a warning when their oldest unsynced record reaches each of these days.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayField("First", state.day1, Modifier.weight(1f)) { viewModel.setDay(0, it) }
                        DayField("Second", state.day2, Modifier.weight(1f)) { viewModel.setDay(1, it) }
                        DayField("Final", state.day3, Modifier.weight(1f)) { viewModel.setDay(2, it) }
                    }
                    Text(
                        "Unsynced data is wiped on day ${SyncPolicyRules.WIPE_DAY}. That is fixed in the app and can't be changed here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dashboard alert", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.staleHours,
                        onValueChange = viewModel::setStaleHours,
                        label = { Text("Flag a worker after (hours without contact)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            state.validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Text(
                "Phones keep their current warning days until they connect again. Apps that haven't been " +
                    "updated keep the default 2, 4 and 6.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save, enabled = state.canSave && !state.needsConnection) {
                    Text(if (state.saving) "Saving…" else "Save")
                }
                TextButton(onClick = viewModel::reset, enabled = state.changed) { Text("Undo changes") }
            }
        }
    }
}

@Composable
private fun DayField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
