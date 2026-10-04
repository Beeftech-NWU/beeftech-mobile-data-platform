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
import androidx.compose.material3.Button
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
import com.beeftech.management.data.REFERENCE_KINDS
import com.beeftech.management.data.ReferenceEntry
import com.beeftech.management.viewmodel.ReferenceDataViewModel
import com.beeftech.management.viewmodel.ReferenceDataViewModelFactory

const val MAX_REFERENCE_NAME_LENGTH = 100

/* The Treatment cost type is needed by every treatment, so the server won't let it be turned off. */
private const val PROTECTED_COST_CODE = "TREATMENT"

/* Capital letters, digits and underscores, starting with a letter: the same rule the server checks. */
fun isValidCostCode(code: String): Boolean =
    Regex("^[A-Z][A-Z0-9_]{1,63}$").matches(code)

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun ReferenceDataTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: ReferenceDataViewModel = viewModel(
        key = "reference-data-$currentUserId",
        factory = ReferenceDataViewModelFactory(apiClient)
    )

    ReferenceDataScreen(viewModel = viewModel, modifier = modifier)
}

/**
 * Online-only, admin only: the values the app's pickers offer. Add a value or turn one off; a
 * turned-off value is hidden from pickers but stays on every record that already uses it.
 */
@Composable
fun ReferenceDataScreen(
    viewModel: ReferenceDataViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showAdd) {
        AddReferenceDialog(
            isCostType = state.kind == "cost-types",
            kindLabel = REFERENCE_KINDS.firstOrNull { it.first == state.kind }?.second.orEmpty(),
            onDismiss = { showAdd = false },
            onAdd = { name, code -> viewModel.add(name, code) { showAdd = false } }
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
            Text("Reference data", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                Button(onClick = { showAdd = true }, enabled = !state.needsConnection) { Text("Add") }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            REFERENCE_KINDS.forEach { (slug, label) ->
                FilterChip(
                    selected = state.kind == slug,
                    onClick = { viewModel.selectKind(slug) },
                    label = { Text(label) }
                )
            }
        }

        Text(
            "Turning a value off hides it from pickers on every phone. Records that already use it keep it. " +
                "Values can't be renamed or deleted.",
            style = MaterialTheme.typography.bodySmall
        )

        if (state.needsConnection) {
            NeedsConnectionNotice("Reference data")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (state.loading && state.entries.isEmpty()) {
            CircularProgressIndicator()
        } else if (state.visibleEntries.isEmpty() && !state.needsConnection && state.error == null) {
            Text("Nothing here yet.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.visibleEntries, key = { "${it.kind}-${it.id}" }) { entry ->
                ReferenceCard(entry, onToggle = { viewModel.setActive(entry, !entry.active) })
            }
        }
    }
}

@Composable
private fun ReferenceCard(entry: ReferenceEntry, onToggle: () -> Unit) {
    val locked = entry.kind == "cost-types" && entry.id == PROTECTED_COST_CODE

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                if (entry.kind == "cost-types") {
                    Text(entry.id, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    if (locked) "Always on: every treatment records a cost with it"
                    else if (entry.active) "On" else "Off",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = entry.active, onCheckedChange = { onToggle() }, enabled = !locked)
        }
    }
}

@Composable
private fun AddReferenceDialog(
    isCostType: Boolean,
    kindLabel: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, code: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    val valid = name.trim().isNotEmpty() && (!isCostType || isValidCostCode(code.trim()))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to $kindLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_REFERENCE_NAME_LENGTH) name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                if (isCostType) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Code, e.g. VET_CALLOUT") },
                        supportingText = { Text("Capital letters, digits and underscores. Can't be changed later.") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, code.takeIf { isCostType }) }, enabled = valid) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
