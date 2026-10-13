package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

            // Group all 11 existing actions without altering their callbacks or destinations.
            Spacer(modifier = Modifier.height(16.dp))
            TraceabilityMenuGroupHeading(
                title = "Farmer & Animal Management",
                description = "Profiles, assignments and animal lookup"
            )
            Spacer(modifier = Modifier.height(8.dp))
            TraceabilityMenuCard(
                title = "Farmer & Farm Profile",
                subtitle = "View farmer details and herd overview",
                icon = Icons.Outlined.HomeWork,
                onClick = onFarmerFarmProfileClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Farmer Registration",
                subtitle = "Register a new farmer or client",
                icon = Icons.Outlined.Description,
                onClick = onFarmerRegistrationClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Assign Animal to Farmer",
                subtitle = "Assign an existing registered calf",
                icon = Icons.AutoMirrored.Outlined.Assignment,
                onClick = onAssignAnimalClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Find Animal",
                subtitle = "Search using an animal tag reference",
                icon = Icons.Outlined.Search,
                onClick = onFindAnimalClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Animal Record",
                subtitle = "Animal profile and linked records",
                icon = Icons.Outlined.Description,
                onClick = onAnimalRecordClick
            )

            Spacer(modifier = Modifier.height(16.dp))
            TraceabilityMenuGroupHeading(
                title = "Farm Operations",
                description = "Movements, health, feeding and suppliers"
            )
            Spacer(modifier = Modifier.height(8.dp))
            TraceabilityMenuCard(
                title = "Animal Movement",
                subtitle = "Capture and review livestock movements",
                icon = Icons.Outlined.Route,
                onClick = onAnimalMovementClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Location & Feed",
                subtitle = "Locations, destinations and feed",
                icon = Icons.Outlined.LocationOn,
                onClick = onLocationFeedClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Treatments",
                subtitle = "Health, medicine and treatment history",
                icon = Icons.Outlined.Medication,
                onClick = onTreatmentsClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Supplier",
                subtitle = "Origin and purchase information",
                icon = Icons.Outlined.LocalShipping,
                onClick = onSupplierClick
            )

            Spacer(modifier = Modifier.height(16.dp))
            TraceabilityMenuGroupHeading(
                title = "Records & Costs",
                description = "Financial and mortality records"
            )
            Spacer(modifier = Modifier.height(8.dp))
            TraceabilityMenuCard(
                title = "Costs",
                subtitle = "Direct and indirect animal costs",
                icon = Icons.Outlined.Payments,
                onClick = onCostSummaryClick
            )
            MenuSpacer()
            TraceabilityMenuCard(
                title = "Mortality Records",
                subtitle = "Capture and review mortality events",
                icon = Icons.AutoMirrored.Outlined.Assignment,
                onClick = onMortalityClick
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

@Composable
private fun MenuSpacer() {
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun TraceabilityMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(
            containerColor = BeeftechSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = BeeftechSoftAccent,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BeeftechPrimaryDark,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechText
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = BeeftechMutedText
                )
            }

            Text(
                text = "›",
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                color = BeeftechPrimaryDark
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FarmTraceabilityScreenPreview() {
    FarmTraceabilityScreen()
}
