package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CostSummaryScreen(
    animalReference: String = "",
    transportCost: String = "",
    processingCost: String = "",
    treatmentCost: String = "",
    handlingCost: String = "",
    interestCost: String = "",
    totalAnimalCost: String = "",
    costPerKg: String = "",
    lastMassDate: String = "",
    onBackClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {

        TraceabilityHeader(
            eyebrow = if (animalReference.isBlank()) {
                "ANIMAL COST"
            } else {
                "ANIMAL $animalReference"
            },
            title = "Cost Summary",
            subtitle = "Direct and indirect livestock costs",
            icon = Icons.Outlined.Payments,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            TraceabilitySectionTitle("Direct Costs")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                CostSummaryRow(
                    icon = Icons.Outlined.LocalShipping,
                    title = "Transport",
                    value = displayCost(transportCost)
                )

                CostDivider()

                CostSummaryRow(
                    icon = Icons.Outlined.Settings,
                    title = "Processing",
                    value = displayCost(processingCost)
                )

                CostDivider()

                CostSummaryRow(
                    icon = Icons.Outlined.Medication,
                    title = "Treatment",
                    value = displayCost(treatmentCost)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("Indirect Costs")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                CostSummaryRow(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "Handling",
                    value = displayCost(handlingCost)
                )

                CostDivider()

                CostSummaryRow(
                    icon = Icons.Outlined.Savings,
                    title = "Interest",
                    value = displayCost(interestCost)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("Cost Overview")

            Spacer(modifier = Modifier.height(12.dp))

            TotalCostCard(
                totalAnimalCost = displayCost(totalAnimalCost),
                costPerKg = displayCost(costPerKg),
                lastMassDate = if (lastMassDate.isBlank()) {
                    "Not recorded"
                } else {
                    lastMassDate
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CostSummaryRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    BeeftechSoftAccent,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BeeftechPrimaryDark,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BeeftechText
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BeeftechPrimaryDark
        )
    }
}

@Composable
private fun CostDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = 52.dp,
            top = 3.dp,
            bottom = 3.dp
        ),
        color = BeeftechBorder
    )
}

@Composable
private fun TotalCostCard(
    totalAnimalCost: String,
    costPerKg: String,
    lastMassDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BeeftechPrimaryDeep
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = BeeftechWhite,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column {

                    Text(
                        text = "TOTAL ANIMAL COST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.7.sp,
                        color = BeeftechSoftAccent
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = totalAnimalCost,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.16f)
            )

            Spacer(modifier = Modifier.height(17.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                CostMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.MonitorWeight,
                    label = "COST / KG",
                    value = costPerKg
                )

                CostMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.CalendarMonth,
                    label = "LAST MASS DATE",
                    value = lastMassDate
                )
            }
        }
    }
}

@Composable
private fun CostMetric(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BeeftechSoftAccent,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = BeeftechSoftAccent
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BeeftechWhite
        )
    }
}

private fun displayCost(
    value: String
): String {
    return if (value.isBlank()) {
        "R 0.00"
    } else if (value.trim().startsWith("R")) {
        value
    } else {
        "R $value"
    }
}

@Preview(showBackground = true)
@Composable
private fun CostSummaryScreenPreview() {
    CostSummaryScreen()
}