package com.beeftech.management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.TeamMember
import com.beeftech.management.data.roleLabel
import com.beeftech.management.viewmodel.TeamViewModel
import com.beeftech.management.viewmodel.TeamViewModelFactory

const val PIN_LENGTH = 5

/* A pin is exactly PIN_LENGTH digits, matching the login screen and the backend. */
fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun TeamTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: TeamViewModel = viewModel(
        key = "team-$currentUserId",
        factory = TeamViewModelFactory(apiClient)
    )

    TeamScreen(
        viewModel = viewModel,
        currentUserId = currentUserId,
        isAdmin = isAdmin,
        modifier = modifier
    )
}

/**
 * Online-only team management. A manager sees the workers on their site;
 * an admin sees everyone, and also has to name a site when creating a user.
 */
@Composable
fun TeamScreen(
    viewModel: TeamViewModel,
    currentUserId: String,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showCreate) {
        CreateUserDialog(
            askForSite = isAdmin,
            onDismiss = { showCreate = false },
            onCreate = { username, pin, siteId ->
                viewModel.createWorker(username, pin, siteId) { showCreate = false }
            }
        )
    }

    state.issuedPin?.let { issued ->
        AlertDialog(
            onDismissRequest = viewModel::dismissIssuedPin,
            title = { Text("New PIN for ${issued.username}") },
            text = {
                Column {
                    Text(issued.pin, style = MaterialTheme.typography.headlineMedium)
                    Text("Give this to the worker now. It won't be shown again.")
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissIssuedPin) { Text("Done") }
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
            Text("Team", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                Button(onClick = { showCreate = true }, enabled = !state.needsConnection) {
                    Text(if (isAdmin) "Add user" else "Add worker")
                }
            }
        }

        if (state.needsConnection) {
            Text(
                "Team management needs a connection. Check your signal and tap Refresh.",
                color = MaterialTheme.colorScheme.error
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (state.loading && state.members.isEmpty()) {
            CircularProgressIndicator()
        } else if (state.members.isEmpty() && !state.needsConnection) {
            Text("No team members yet.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.members, key = { it.userId }) { member ->
                MemberCard(
                    member = member,
                    isSelf = member.userId == currentUserId,
                    onToggleActive = { viewModel.setActive(member, !member.active) },
                    onResetPin = { viewModel.resetPin(member) },
                    onUnbind = { viewModel.unbindDevice(member) }
                )
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: TeamMember,
    isSelf: Boolean,
    onToggleActive: () -> Unit,
    onResetPin: () -> Unit,
    onUnbind: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                member.username + if (isSelf) " (you)" else "",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                buildString {
                    append(roleLabel(member.role))
                    member.siteId?.let { append(" · $it") }
                    append(if (member.active) " · Active" else " · Deactivated")
                    append(if (member.deviceAssignedId != null) " · Phone linked" else " · No phone linked")
                },
                style = MaterialTheme.typography.bodySmall
            )
            Row {
                if (!isSelf) {
                    TextButton(onClick = onToggleActive) {
                        Text(if (member.active) "Deactivate" else "Reactivate")
                    }
                }
                TextButton(onClick = onResetPin) { Text("Reset PIN") }
                if (member.deviceAssignedId != null) {
                    TextButton(onClick = onUnbind) { Text("Unlink phone") }
                }
            }
        }
    }
}

@Composable
private fun CreateUserDialog(
    askForSite: Boolean,
    onDismiss: () -> Unit,
    onCreate: (username: String, pin: String, siteId: String?) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var siteId by remember { mutableStateOf("") }

    val valid = username.trim().length >= 3 &&
        isValidPin(pin) &&
        (!askForSite || siteId.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (askForSite) "Add user" else "Add worker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= PIN_LENGTH) pin = it },
                    label = { Text("$PIN_LENGTH-digit PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                if (askForSite) {
                    OutlinedTextField(
                        value = siteId,
                        onValueChange = { siteId = it },
                        label = { Text("Site ID") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onCreate(username, pin, siteId.takeIf { askForSite }) }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
