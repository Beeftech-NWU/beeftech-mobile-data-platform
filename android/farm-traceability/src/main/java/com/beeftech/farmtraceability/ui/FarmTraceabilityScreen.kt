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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BeeftechPrimaryDeep)
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = 30.dp,
                    bottom = 25.dp
                )
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
                text = "Manage livestock traceability records and farm information",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = BeeftechSoftAccent
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 24.dp
                )
        ) {

            TraceabilitySectionTitle(
                title = "Traceability Options"
            )

            Spacer(modifier = Modifier.height(16.dp))

            TraceabilityMenuCard(
                title = "Farmer & Farm Profile",
                subtitle = "Farmer, farm and location details",
                icon = Icons.Outlined.HomeWork,
                onClick = onFarmerFarmProfileClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Find Animal",
                subtitle = "Locate an animal by reference number",
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
                subtitle = "Capture livestock movement information",
                icon = Icons.Outlined.Route,
                onClick = onAnimalMovementClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Supplier",
                subtitle = "Origin and purchase information",
                icon = Icons.Outlined.LocalShipping,
                onClick = onSupplierClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Location & Feed",
                subtitle = "Destination and ration information",
                icon = Icons.Outlined.LocationOn,
                onClick = onLocationFeedClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Treatments",
                subtitle = "Disease and medication records",
                icon = Icons.Outlined.Medication,
                onClick = onTreatmentsClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Cost Summary",
                subtitle = "Direct and indirect animal costs",
                icon = Icons.Outlined.Payments,
                onClick = onCostSummaryClick
            )

            MenuSpacer()

            TraceabilityMenuCard(
                title = "Mortality Records",
                subtitle = "Capture livestock mortality information",
                icon = Icons.Outlined.Assignment,
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