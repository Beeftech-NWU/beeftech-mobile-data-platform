package com.beeftech.management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.Site
import com.beeftech.management.viewmodel.SitesViewModel
import com.beeftech.management.viewmodel.SitesViewModelFactory
import com.beeftech.management.viewmodel.isValidFarmCode
import com.beeftech.management.viewmodel.normaliseFarmCode

const val MAX_SITE_NAME_LENGTH = 100

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun SitesTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: SitesViewModel = viewModel(
        key = "sites-$currentUserId",
        factory = SitesViewModelFactory(apiClient)
    )

    SitesScreen(viewModel = viewModel, modifier = modifier)
}

/** Online-only site management for admins: list, add, rename, deactivate and reactivate. */
@Composable
fun SitesScreen(
    viewModel: SitesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Site?>(null) }
    var changingCode by remember { mutableStateOf<Site?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showCreate) {
        NewSiteDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name, farmCode -> viewModel.createSite(name, farmCode) { showCreate = false } }
        )
    }

    changingCode?.let { site ->
        FarmCodeDialog(
            title = "Farm code for ${site.name}",
            initial = site.farmCode.orEmpty(),
            onDismiss = { changingCode = null },
            onConfirm = { code -> viewModel.changeFarmCode(site, code) { changingCode = null } }
        )
    }

    renaming?.let { site ->
        SiteNameDialog(
            title = "Rename ${site.name}",
            initial = site.name,
            confirmLabel = "Rename",
            onDismiss = { renaming = null },
            onConfirm = { name -> viewModel.rename(site, name) { renaming = null } }
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
            Text("Sites", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                Button(onClick = { showCreate = true }, enabled = !state.needsConnection) { Text("Add site") }
            }
        }

        if (state.needsConnection) {
            NeedsConnectionNotice("Site management")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (state.loading && state.sites.isEmpty()) {
            CircularProgressIndicator()
        } else if (state.sites.isEmpty() && !state.needsConnection && state.error == null) {
            Text("No sites yet. Create a site before assigning managers and workers.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.sites, key = { it.siteId }) { site ->
                SiteCard(
                    site = site,
                    onRename = { renaming = site },
                    onChangeFarmCode = { changingCode = site },
                    onToggleActive = { viewModel.setActive(site, !site.active) }
                )
            }
        }
    }
}

@Composable
private fun SiteCard(
    site: Site,
    onRename: () -> Unit,
    onChangeFarmCode: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(site.name, style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    append(site.farmCode ?: "No farm code")
                    append(" · ${site.siteId}")
                    append(" · ${site.activeUserCount} active user${if (site.activeUserCount == 1L) "" else "s"}")
                    append(if (site.active) " · Active" else " · Inactive")
                },
                style = MaterialTheme.typography.bodySmall
            )
            Row {
                TextButton(onClick = onRename) { Text("Rename") }
                TextButton(onClick = onChangeFarmCode) { Text("Farm code") }
                TextButton(onClick = onToggleActive) { Text(if (site.active) "Deactivate" else "Reactivate") }
            }
        }
    }
}

@Composable
private fun SiteNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    val valid = name.trim().isNotEmpty() && name.trim() != initial.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_SITE_NAME_LENGTH) name = it },
                label = { Text("Site name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FarmCodeField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { entered ->
            onValueChange(entered.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(FARM_CODE_LENGTH))
        },
        label = { Text("Farm code") },
        supportingText = { Text("4 characters, A-Z and 0-9. Starts every file name from this farm.") },
        singleLine = true
    )
}

@Composable
private fun NewSiteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, farmCode: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var farmCode by remember { mutableStateOf("") }
    val valid = name.trim().isNotEmpty() && isValidFarmCode(farmCode)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add site") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_SITE_NAME_LENGTH) name = it },
                    label = { Text("Site name") },
                    singleLine = true
                )
                FarmCodeField(farmCode) { farmCode = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, farmCode) }, enabled = valid) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FarmCodeDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var farmCode by remember { mutableStateOf(initial) }
    val valid = isValidFarmCode(farmCode) && normaliseFarmCode(farmCode) != initial

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { FarmCodeField(farmCode) { farmCode = it } },
        confirmButton = {
            TextButton(onClick = { onConfirm(farmCode) }, enabled = valid) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private const val FARM_CODE_LENGTH = 4
