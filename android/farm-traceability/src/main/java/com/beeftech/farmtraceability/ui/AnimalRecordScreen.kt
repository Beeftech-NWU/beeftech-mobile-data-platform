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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Tag
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
    onBackClick: () -> Unit = {},
    onSupplierClick: () -> Unit = {},
    onLocationFeedClick: () -> Unit = {},
    onTreatmentsClick: () -> Unit = {},
    onCostSummaryClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {

        TraceabilityHeader(
            eyebrow = if (animalReference.isBlank()) {
                "ANIMAL RECORD"
            } else {
                "ANIMAL $animalReference"
            },
            title = "Animal Record",
            subtitle = "Traceability information linked to this animal",
            icon = Icons.Outlined.Description,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            TraceabilitySectionTitle("Animal Identity")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "Animal Reference",
                    subtitle = if (animalReference.isBlank()) {
                        "No animal selected"
                    } else {
                        "Selected animal"
                    },
                    value = animalReference
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("Traceability Record")

            Spacer(modifier = Modifier.height(12.dp))

            AnimalRecordNavigationCard(
                title = "Supplier",
                subtitle = "Origin and purchase information",
                icon = Icons.Outlined.LocalShipping,
                onClick = onSupplierClick
            )

            Spacer(modifier = Modifier.height(11.dp))

            AnimalRecordNavigationCard(
                title = "Location & Feed",
                subtitle = "Destination and ration information",
                icon = Icons.Outlined.LocationOn,
                onClick = onLocationFeedClick
            )

            Spacer(modifier = Modifier.height(11.dp))

            AnimalRecordNavigationCard(
                title = "Treatments",
                subtitle = "Disease and medication records",
                icon = Icons.Outlined.Medication,
                onClick = onTreatmentsClick
            )

            Spacer(modifier = Modifier.height(11.dp))

            AnimalRecordNavigationCard(
                title = "Cost Summary",
                subtitle = "Direct and indirect animal costs",
                icon = Icons.Outlined.Payments,
                onClick = onCostSummaryClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("Record Status")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                Text(
                    text = "TRACEABILITY INFORMATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = BeeftechPrimaryDark
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = if (animalReference.isBlank()) {
                        "Select an animal to view its linked traceability records."
                    } else {
                        "Traceability sections for this animal are available above."
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = BeeftechMutedText
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
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
                        BeeftechSoftAccent,
                        RoundedCornerShape(11.dp)
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