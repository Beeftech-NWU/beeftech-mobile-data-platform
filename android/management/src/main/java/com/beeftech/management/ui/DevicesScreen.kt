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
import com.beeftech.management.data.Device
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.DevicesViewModel
import com.beeftech.management.viewmodel.DevicesViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val MAX_DEVICE_REASON_LENGTH = 500

/* Keyed by user, so a different user logging in on the same device never sees the previous list. */
@Composable
fun DevicesTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    canManage: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: DevicesViewModel = viewModel(
        key = "devices-$currentUserId",
        factory = DevicesViewModelFactory(apiClient, canManage)
    )

    DevicesScreen(viewModel = viewModel, modifier = modifier)
}

/**
 * Online-only list of the phones that have signed in. An admin can block and unblock a phone;
 * a manager sees their site's phones read-only.
 */
@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var blocking by remember { mutableStateOf<Device?>(null) }
    var unblocking by remember { mutableStateOf<Device?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    blocking?.let { device ->
        DeviceReasonDialog(
            title = "Block ${device.model ?: device.deviceId}?",
            warning = "A blocked phone can't sign in or sync. Records still waiting on it can't upload " +
                "until you unblock it, and are wiped after 7 days offline. Only block a phone that is lost or stolen.",
            confirmLabel = "Block phone",
            onDismiss = { blocking = null },
            onConfirm = { reason -> viewModel.block(device, reason) { blocking = null } }
        )
    }

    unblocking?.let { device ->
        DeviceReasonDialog(
            title = "Unblock ${device.model ?: device.deviceId}?",
            warning = "The phone can sign in and sync again, and anything still waiting on it can upload.",
            confirmLabel = "Unblock phone",
            onDismiss = { unblocking = null },
            onConfirm = { reason -> viewModel.unblock(device, reason) { unblocking = null } }
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
            Text("Phones", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf<Pair<String?, String>>(null to "All", "ACTIVE" to "Active", "REVOKED" to "Blocked").forEach { (value, label) ->
                FilterChip(
                    selected = state.status == value,
                    onClick = { viewModel.selectStatus(value) },
                    label = { Text(label) }
                )
            }
        }

        if (state.needsConnection) {
            NeedsConnectionNotice("The phone list")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (state.loading && state.devices.isEmpty()) {
            CircularProgressIndicator()
        } else if (state.devices.isEmpty() && !state.needsConnection && state.error == null) {
            Text("No phones linked yet. Linked worker devices will appear here.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.devices, key = { it.deviceId }) { device ->
                DeviceCard(
                    device = device,
                    canManage = viewModel.canManage,
                    onBlock = { blocking = device },
                    onUnblock = { unblocking = device }
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: Device,
    canManage: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(device.model ?: "Unknown model", style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    append(device.deviceId)
                    device.appVersion?.let { append(" · v$it") }
                    append(if (device.isRevoked) " · Blocked" else " · Active")
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Last seen ${formatDeviceTime(device.lastSeenAt)}" +
                    (device.lastUsername?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            if (device.boundUsernames.isNotEmpty()) {
                Text("Assigned to ${device.boundUsernames.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            }
            device.siteId?.let { Text("Site $it", style = MaterialTheme.typography.bodySmall) }
            if (device.isRevoked) {
                Text(
                    "Blocked" + (device.revokedAt?.let { " ${formatDeviceTime(it)}" } ?: "") +
                        (device.revokeReason?.let { ": $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (canManage) {
                Row {
                    if (device.isRevoked) {
                        TextButton(onClick = onUnblock) { Text("Unblock") }
                    } else {
                        TextButton(onClick = onBlock) { Text("Block") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceReasonDialog(
    title: String,
    warning: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(warning)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= MAX_DEVICE_REASON_LENGTH) reason = it },
                    label = { Text("Reason") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = reason.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatDeviceTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
