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
import com.beeftech.management.data.AUDIT_ACTIONS
import com.beeftech.management.data.AuditLogEntry
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.auditActionLabel
import com.beeftech.management.data.roleLabel
import com.beeftech.management.viewmodel.AuditLogViewModel
import com.beeftech.management.viewmodel.AuditLogViewModelFactory
import com.beeftech.management.viewmodel.AuditRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Keyed by user, so a different user logging in on the same device never sees the previous log. */
@Composable
fun AuditLogTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: AuditLogViewModel = viewModel(
        key = "audit-log-$currentUserId",
        factory = AuditLogViewModelFactory(apiClient)
    )

    AuditLogScreen(viewModel = viewModel, modifier = modifier)
}

/**
 * Online-only, read-only audit log. An admin sees every site and a manager sees their own;
 * the server decides, so this screen has nothing to configure.
 */
@Composable
fun AuditLogScreen(
    viewModel: AuditLogViewModel,
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
            Text("Audit log", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.action == null,
                onClick = { viewModel.selectAction(null) },
                label = { Text("All actions") }
            )
            AUDIT_ACTIONS.forEach { (action, label) ->
                FilterChip(
                    selected = state.action == action,
                    onClick = { viewModel.selectAction(action) },
                    label = { Text(label) }
                )
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuditRange.entries.forEach { range ->
                FilterChip(
                    selected = state.range == range,
                    onClick = { viewModel.selectRange(range) },
                    label = { Text(range.label) }
                )
            }
        }

        if (state.needsConnection) {
            NeedsConnectionNotice("The audit log")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (state.loading && state.entries.isEmpty()) {
            CircularProgressIndicator()
        } else if (state.entries.isEmpty() && !state.needsConnection && state.error == null) {
            Text("Nothing recorded for this filter.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.entries, key = { it.id }) { entry -> AuditEntryCard(entry) }

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
private fun AuditEntryCard(entry: AuditLogEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(auditActionLabel(entry.action), style = MaterialTheme.typography.titleMedium)
            Text(
                "${entry.actorUsername} (${roleLabel(entry.actorRole)}) · ${formatAuditTime(entry.createdAt)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(auditTarget(entry), style = MaterialTheme.typography.bodyMedium)
            if (entry.reason.isNotBlank()) {
                Text("Reason: ${entry.reason}", style = MaterialTheme.typography.bodyMedium)
            }
            auditChanges(entry.details)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            entry.siteId?.let { Text("Site $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun auditTarget(entry: AuditLogEntry): String =
    when (entry.entityType) {
        "USER" -> "User ${entry.entityId}"
        "SITE" -> "Site ${entry.entityId}"
        "DEVICE" -> "Phone ${entry.entityId}"
        "SYNC_POLICY" -> "Sync policy"
        "DISEASE" -> "Disease ${entry.entityId}"
        "TREATMENT_TYPE" -> "Treatment type ${entry.entityId}"
        "COST_TYPE" -> "Cost type ${entry.entityId}"
        else -> "${entry.entityType} ${entry.entityId}"
    }

/*
 * The details are a flat JSON object of strings, e.g. {"role":"3->2"}. Showing it as
 * "role 3->2" keeps this free of a JSON parser; anything that doesn't look like that is shown as is.
 */
internal fun auditChanges(details: String?): String? {
    if (details.isNullOrBlank()) return null
    val body = details.trim().removePrefix("{").removeSuffix("}")
    if (body.isBlank()) return null
    return body.split("\",\"").joinToString(", ") { pair ->
        pair.replace("\"", "").replaceFirst(":", " ")
    }
}

private fun formatAuditTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
