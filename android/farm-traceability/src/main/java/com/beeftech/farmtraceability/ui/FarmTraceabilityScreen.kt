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
    onBackClick: () -> Unit = {},
    onRetrySyncClick: () -> Unit = {},
    onFarmerFarmProfileClick: () -> Unit = {},
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
                    top = 18.dp,
                    bottom = 25.dp
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

            Spacer(modifier = Modifier.height(4.dp))

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

                Text(
                    text = "Farm Traceability",
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechWhite
                )

                Spacer(modifier = Modifier.height(7.dp))

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
                    horizontal = 18.dp,
                    vertical = 24.dp
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

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ---------------------------------------------------------
            // SYNC STATUS
            // ---------------------------------------------------------

            TraceabilitySectionTitle(
                title = "Sync Status"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityInfoRow(
                    icon = Icons.Outlined.CloudSync,
                    title = "Pending Records",
                    subtitle = pendingRecordCount?.let {
                        "$it record${if (it == 1) "" else "s"} waiting to sync"
                    } ?: "Pending record count unavailable"
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.CloudDone,
                    title = "Last Sync",
                    subtitle = lastSync.ifBlank {
                        "Last sync information unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Schedule,
                    title = "Scheduled Sync",
                    subtitle = "Morning 05:00–06:00 • Evening 18:00–19:00"
                )

                if (syncStatus.isNotBlank()) {

                    Spacer(modifier = Modifier.height(14.dp))

                    TraceabilityInfoRow(
                        icon = Icons.Outlined.CloudSync,
                        title = "Current Status",
                        subtitle = syncStatus
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                TraceabilitySecondaryButton(
                    text = "Retry Sync",
                    icon = Icons.Outlined.Refresh,
                    onClick = onRetrySyncClick
                )
            }

            // ---------------------------------------------------------
            // TRACEABILITY OPTIONS
            // ---------------------------------------------------------

            Spacer(modifier = Modifier.height(28.dp))

            TraceabilitySectionTitle(
                title = "Traceability Options"
            )

            Spacer(modifier = Modifier.height(16.dp))

            TraceabilityMenuCard(
                title = "Farmer & Farm Profile",
                subtitle = "View farmer, farm and location details",
                icon = Icons.Outlined.HomeWork,
                onClick = onFarmerFarmProfileClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Find Animal",
                subtitle = "Find an animal using its tag reference",
                icon = Icons.Outlined.Search,
                onClick = onFindAnimalClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Animal Record",
                subtitle = "View the complete animal record",
                icon = Icons.Outlined.Description,
                onClick = onAnimalRecordClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Animal Movement",
                subtitle = "Capture and review livestock movement records",
                icon = Icons.Outlined.Route,
                onClick = onAnimalMovementClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Supplier",
                subtitle = "View and capture origin and purchase information",
                icon = Icons.Outlined.LocalShipping,
                onClick = onSupplierClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Location & Feed",
                subtitle = "View and capture destination and ration information",
                icon = Icons.Outlined.LocationOn,
                onClick = onLocationFeedClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Treatments",
                subtitle = "View and capture treatment records",
                icon = Icons.Outlined.Medication,
                onClick = onTreatmentsClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Cost Summary",
                subtitle = "View direct and indirect animal costs",
                icon = Icons.Outlined.Payments,
                onClick = onCostSummaryClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Mortality Records",
                subtitle = "Capture and review livestock mortality records",
                Icons.AutoMirrored.Outlined.Assignment,
                onClick = onMortalityClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MenuSpacer() {
    Spacer(modifier = Modifier.height(11.dp))
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
        shape = RoundedCornerShape(14.dp),
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
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(47.dp)
                    .background(
                        color = BeeftechSoftAccent,
                        shape = RoundedCornerShape(12.dp)
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

            Spacer(modifier = Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
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
                fontSize = 26.sp,
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