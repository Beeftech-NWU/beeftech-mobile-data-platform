package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FarmTraceabilityScreen(
    pendingRecordCount: Int? = null,
    lastSync: String = "",
    syncStatus: String = "",
    syncWarningLevel: Int = 0,
    scheduledSync: String = "",
    retrySyncAvailable: Boolean = false,
    onBackClick: () -> Unit = {},
    onRetrySyncClick: () -> Unit = {},
    onFarmerFarmProfileClick: () -> Unit = {},
    onAssignAnimalClick: () -> Unit = {},
    onFarmerRegistrationClick: () -> Unit = {},
    onFindAnimalClick: () -> Unit = {},
    onAnimalRecordClick: () -> Unit = {},
    onAnimalMovementClick: () -> Unit = {},
    onSupplierClick: () -> Unit = {},
    onLocationFeedClick: () -> Unit = {},
    onTreatmentsClick: () -> Unit = {},
    onCostSummaryClick: () -> Unit = {},
    onMortalityClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {

        // ---------------------------------------------------------
        // HEADER
        // ---------------------------------------------------------

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BeeftechPrimaryDeep)
                .padding(
                    start = 14.dp,
                    end = 22.dp,
                    top = 10.dp,
                    bottom = 12.dp
                )
        ) {

            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to main Beeftech app",
                    tint = BeeftechWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "BEEFTECH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = BeeftechPrimary
                )

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Agriculture,
                        contentDescription =
                            "Farm Traceability",
                        tint = BeeftechWhite,
                        modifier =
                            Modifier.size(27.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.size(10.dp)
                    )

                    Text(
                        text = "Farm Traceability",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechWhite
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "View and manage livestock traceability information",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = BeeftechSoftAccent
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
        ) {

            // ---------------------------------------------------------
            // UNSYNCED DATA WARNING
            // ---------------------------------------------------------

            if (syncWarningLevel in 1..3) {

                TraceabilitySyncWarning(
                    warningLevel = syncWarningLevel,
                    onSyncClick = onRetrySyncClick
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ---------------------------------------------------------
            // COMPACT SYNC SUMMARY
            // Full sync details remain on the main dashboard.
            // Keep the warning and actionable retry here for offline work.
            // ---------------------------------------------------------
            TraceabilityCard {
                // Status comes from the existing sync ViewModel, not an optimistic UI guess.
                val failed = syncStatus.contains("fail", ignoreCase = true) ||
                    syncStatus.contains("error", ignoreCase = true)
                val pending = (pendingRecordCount ?: 0) > 0
                // Never infer server acknowledgement from an empty local queue.
                val acknowledged = syncStatus.trim().equals("Synced", ignoreCase = true) &&
                    pendingRecordCount == 0
                val label = when {
                    failed -> "Sync: Failed"
                    pending -> "Sync: Pending"
                    acknowledged -> "Sync: Synced"
                    pendingRecordCount == 0 -> "Sync queue clear"
                    else -> "Sync status"
                }
                val detail = when {
                    failed -> syncStatus
                    pending -> "${pendingRecordCount} record(s) waiting to sync"
                    acknowledged -> "Server synchronization confirmed"
                    pendingRecordCount == 0 -> "No pending local records. Check dashboard for server confirmation"
                    syncStatus.isNotBlank() -> syncStatus
                    else -> "Open the dashboard to check synchronization"
                }
                TraceabilityInfoRow(
                    icon = Icons.Outlined.CloudSync,
                    title = label,
                    subtitle = detail
                )

                // A warning already contains the single contextual retry action.
                if (retrySyncAvailable && syncWarningLevel !in 1..3 && pending) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TraceabilitySecondaryButton(
                        text = "Retry Sync",
                        icon = Icons.Outlined.Refresh,
                        onClick = onRetrySyncClick
                    )
                }
            }

            // Design A: two-column tiles. All 11 original destinations stay unchanged.
            // The header, warning and existing sync summary above this line are untouched.
            Spacer(modifier = Modifier.height(16.dp))
            TraceabilityMenuGroupHeading(
                title = "Farmer & Animal Management",
                description = "Profiles, assignments and animal lookup"
            )
            Spacer(modifier = Modifier.height(8.dp))
            TraceabilityMenuGrid(
                items = listOf(
                    TraceabilityMenuAction("Farmer & Farm Profile", "Farmer and herd overview", Icons.Outlined.HomeWork, onFarmerFarmProfileClick),
                    TraceabilityMenuAction("Farmer Registration", "Register a farmer or client", Icons.Outlined.Description, onFarmerRegistrationClick),
                    TraceabilityMenuAction("Assign Animal to Farmer", "Link a registered calf", Icons.AutoMirrored.Outlined.Assignment, onAssignAnimalClick),
                    TraceabilityMenuAction("Find Animal", "Search by animal tag", Icons.Outlined.Search, onFindAnimalClick),
                    TraceabilityMenuAction("Animal Record", "Animal details and history", Icons.Outlined.Description, onAnimalRecordClick)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            TraceabilityMenuGroupHeading(
                title = "Farm Operations",
                description = "Movements, health, feeding and suppliers"
            )
            Spacer(modifier = Modifier.height(8.dp))
            TraceabilityMenuGrid(
                items = listOf(
                    TraceabilityMenuAction("Animal Movement", "Movements and history", Icons.Outlined.Route, onAnimalMovementClick),
                    TraceabilityMenuAction("Location & Feed", "Locations and feeding", Icons.Outlined.LocationOn, onLocationFeedClick),
                    TraceabilityMenuAction("Treatments", "Medicines and treatments", Icons.Outlined.Medication, onTreatmentsClick),
                    TraceabilityMenuAction("Supplier", "Supplier information", Icons.Outlined.LocalShipping, onSupplierClick)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            TraceabilityMenuGroupHeading(
                title = "Records & Costs",
                description = "Financial and mortality records"
            )
            Spacer(modifier = Modifier.height(8.dp))
            TraceabilityMenuGrid(
                items = listOf(
                    TraceabilityMenuAction("Costs", "Direct and indirect costs", Icons.Outlined.Payments, onCostSummaryClick),
                    TraceabilityMenuAction("Mortality Records", "Animal losses and records", Icons.AutoMirrored.Outlined.Assignment, onMortalityClick)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TraceabilityMenuGroupHeading(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = BeeftechPrimaryDeep,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            color = BeeftechMutedText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

// One action per existing destination. These are links, not duplicate forms.
private data class TraceabilityMenuAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun TraceabilityMenuGrid(items: List<TraceabilityMenuAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TraceabilityMenuTile(
                    action = pair[0],
                    modifier = Modifier.weight(1f)
                )
                if (pair.size > 1) {
                    TraceabilityMenuTile(
                        action = pair[1],
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Keep the last odd tile at half width, matching the grid.
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TraceabilityMenuTile(
    action: TraceabilityMenuAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = action.onClick),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(
                            color = BeeftechSoftAccent,
                            shape = RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = BeeftechPrimaryDark,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    color = BeeftechPrimaryDark
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = action.title,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BeeftechText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = action.subtitle,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = BeeftechMutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FarmTraceabilityScreenPreview() {
    FarmTraceabilityScreen()
}
