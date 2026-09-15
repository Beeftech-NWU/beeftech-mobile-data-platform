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
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Timer
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
fun AnimalRecordScreen(
    animalReference: String = "",
    breed: String = "",
    gender: String = "",
    entryMass: String = "",
    lastMass: String = "",
    daysAtFacility: String = "",
    averageDailyGain: String = "",
    onBackClick: () -> Unit = {},
    onSupplierClick: () -> Unit = {},
    onLocationFeedClick: () -> Unit = {},
    onTreatmentsClick: () -> Unit = {},
    onAnimalMovementClick: () -> Unit = {},
    onCostSummaryClick: () -> Unit = {},
    onMortalityClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {

        TraceabilityHeader(
            eyebrow = "FARM TRACEABILITY",
            title = "Animal Record",
            subtitle = "View animal and linked traceability information",
            icon = Icons.Outlined.Description,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            // ---------------------------------------------------------
            // ANIMAL IDENTITY
            // ---------------------------------------------------------

            TraceabilitySectionTitle(
                title = "Animal Identity"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "Tag Reference",
                    subtitle = animalReference.ifBlank {
                        "No animal selected"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Pets,
                    title = "Breed",
                    subtitle = breed.ifBlank {
                        "Breed information unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Female,
                    title = "Gender",
                    subtitle = gender.ifBlank {
                        "Gender information unavailable"
                    }
                )
            }

            // ---------------------------------------------------------
            // ANIMAL PERFORMANCE
            // ---------------------------------------------------------

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                title = "Animal Performance"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityInfoRow(
                    icon = Icons.Outlined.MonitorWeight,
                    title = "Entry Mass",
                    subtitle = entryMass.ifBlank {
                        "Entry mass unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.MonitorWeight,
                    title = "Last Mass",
                    subtitle = lastMass.ifBlank {
                        "Last mass unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Timer,
                    title = "Days at Facility",
                    subtitle = daysAtFacility.ifBlank {
                        "Facility duration unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    title = "Average Daily Gain (ADG)",
                    subtitle = averageDailyGain.ifBlank {
                        "ADG information unavailable"
                    }
                )
            }

            // ---------------------------------------------------------
            // LINKED TRACEABILITY RECORDS
            // ---------------------------------------------------------

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                title = "Traceability Record"
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimalRecordNavigationCard(
                title = "Supplier",
                subtitle = "View origin and purchase information",
                icon = Icons.Outlined.LocalShipping,
                onClick = onSupplierClick
            )

            RecordSpacer()

            AnimalRecordNavigationCard(
                title = "Location & Feed",
                subtitle = "View destination and ration information",
                icon = Icons.Outlined.LocationOn,
                onClick = onLocationFeedClick
            )

            RecordSpacer()

            AnimalRecordNavigationCard(
                title = "Treatments",
                subtitle = "View disease and treatment records",
                icon = Icons.Outlined.Medication,
                onClick = onTreatmentsClick
            )

            RecordSpacer()

            AnimalRecordNavigationCard(
                title = "Animal Movement",
                subtitle = "View and capture movement records",
                icon = Icons.Outlined.Route,
                onClick = onAnimalMovementClick
            )

            RecordSpacer()

            AnimalRecordNavigationCard(
                title = "Cost Summary",
                subtitle = "View direct and indirect animal costs",
                icon = Icons.Outlined.Payments,
                onClick = onCostSummaryClick
            )

            RecordSpacer()

            AnimalRecordNavigationCard(
                title = "Mortality Records",
                subtitle = "View and capture mortality records",
                icon = Icons.AutoMirrored.Outlined.Assignment,
                onClick = onMortalityClick
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun RecordSpacer() {
    Spacer(modifier = Modifier.height(11.dp))
}

@Composable
private fun AnimalRecordNavigationCard(
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = BeeftechSoftAccent,
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BeeftechPrimaryDark,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.size(13.dp))

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
private fun AnimalRecordScreenPreview() {
    AnimalRecordScreen()
}