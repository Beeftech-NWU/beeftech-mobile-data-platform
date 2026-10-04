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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.REVIEW_TYPES
import com.beeftech.management.data.ReviewRecord
import com.beeftech.management.viewmodel.RecordsReviewViewModel
import com.beeftech.management.viewmodel.RecordsReviewViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val MAX_VOID_REASON_LENGTH = 500

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun RecordsReviewTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: RecordsReviewViewModel = viewModel(
        key = "records-review-$currentUserId",
        factory = RecordsReviewViewModelFactory(apiClient)
    )

    RecordsReviewScreen(viewModel = viewModel, modifier = modifier)
}

/**
 * Online-only records review. A manager sees their site's records and an admin sees all of
 * them. Void is the only correction: the worker re-captures the record.
 */
@Composable
fun RecordsReviewScreen(
    viewModel: RecordsReviewViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var voidTarget by remember { mutableStateOf<ReviewRecord?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    voidTarget?.let { record ->
        VoidDialog(
            record = record,
            onDismiss = { voidTarget = null },
            onVoid = { reason ->
                viewModel.voidRecord(record, reason)
                voidTarget = null
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
            Text("Records", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            REVIEW_TYPES.forEach { (slug, label) ->
                FilterChip(
                    selected = state.type == slug,
                    onClick = { viewModel.selectType(slug) },
                    label = { Text(label) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = state.showVoided, onCheckedChange = viewModel::setShowVoided)
            Text("  Show voided", style = MaterialTheme.typography.bodyMedium)
        }

        if (state.needsConnection) {
            Text(
                "Records review needs a connection. Check your signal and tap Refresh.",
                color = MaterialTheme.colorScheme.error
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (state.loading && state.records.isEmpty()) {
            CircularProgressIndicator()
        } else if (state.visibleRecords.isEmpty() && !state.needsConnection && state.error == null) {
            Text("No records.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.visibleRecords, key = { "${it.type}-${it.id}" }) { record ->
                RecordCard(record = record, onVoid = { voidTarget = record })
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: ReviewRecord,
    onVoid: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(record.label, style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    record.submittedByUsername?.let { append(it) }
                    record.siteId?.let { append(if (isEmpty()) it else " · $it") }
                    record.capturedAt?.let { append(if (isEmpty()) formatTime(it) else " · ${formatTime(it)}") }
                },
                style = MaterialTheme.typography.bodySmall
            )
            if (record.isVoided) {
                Text(
                    "Voided: ${record.voidReason.orEmpty()}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                TextButton(onClick = onVoid) { Text("Void") }
            }
        }
    }
}

@Composable
private fun VoidDialog(
    record: ReviewRecord,
    onDismiss: () -> Unit,
    onVoid: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Void ${record.label}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The record is kept but hidden. The worker will need to capture it again.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= MAX_VOID_REASON_LENGTH) reason = it },
                    label = { Text("Reason") }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = reason.isNotBlank(), onClick = { onVoid(reason) }) { Text("Void") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(Date(millis))
