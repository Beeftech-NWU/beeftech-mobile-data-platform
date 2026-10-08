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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CostSummaryScreen(
    animalReference: String = "",
    transportCost: String = "",
    processingCost: String = "",
    treatmentCost: String = "",
    feedCost: String = "",
    handlingCost: String = "",
    interestCost: String = "",
    otherCost: String = "",
    totalAnimalCost: String = "",
    costPerKg: String = "",
    lastMassDate: String = "",
    onSaveCost: (
        costType: String,
        amount: String,
        description: String,
        costDate: String,
        onCompleted: (Boolean, String) -> Unit
    ) -> Unit = { _, _, _, _, onCompleted ->
        onCompleted(
            false,
            "Cost saving is unavailable."
        )
    },
    onSaveMass: (
        massKg: String,
        weighDate: String,
        note: String,
        onCompleted: (Boolean, String) -> Unit
    ) -> Unit = { _, _, _, onCompleted ->
        onCompleted(
            false,
            "Mass saving is unavailable."
        )
    },
    onBackClick: () -> Unit = {}
) {
    var selectedCostType by remember {
        mutableStateOf("TRANSPORT")
    }

    var costAmount by remember {
        mutableStateOf("")
    }

    var costNote by remember {
        mutableStateOf("")
    }

    var costMessage by remember {
        mutableStateOf("")
    }

    var transportDistanceKm by remember {
        mutableStateOf("")
    }

    var transportRatePerKm by remember {
        mutableStateOf("")
    }

    var transportTrips by remember {
        mutableStateOf("1")
    }

    var transportAnimals by remember {
        mutableStateOf("1")
    }

    var transportExtras by remember {
        mutableStateOf("0")
    }

    var interestAnnualRate by remember {
        mutableStateOf("")
    }

    var interestDays by remember {
        mutableStateOf("")
    }

    val todayDate =
        remember {
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date())
        }

    var costDate by remember {
        mutableStateOf(todayDate)
    }

    var massValue by remember {
        mutableStateOf("")
    }

    var massDate by remember {
        mutableStateOf(todayDate)
    }

    var massNote by remember {
        mutableStateOf("")
    }

    var massMessage by remember {
        mutableStateOf("")
    }

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
            TraceabilitySectionTitle(
                "Direct Costs"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

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

                CostDivider()

                CostSummaryRow(
                    icon = Icons.Outlined.Restaurant,
                    title = "Feed / Ration",
                    value = displayCost(feedCost)
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TraceabilitySectionTitle(
                "Indirect Costs"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TraceabilityCard {
                CostSummaryRow(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Handling",
                    value = displayCost(handlingCost)
                )

                CostDivider()

                CostSummaryRow(
                    icon = Icons.Outlined.Savings,
                    title = "Interest",
                    value = displayCost(interestCost)
                )

                /*
                 * Only shown when costs exist in categories
                 * without their own line, so the rows add up to the total.
                 */
                if ((otherCost.toDoubleOrNull() ?: 0.0) > 0.0) {

                    CostDivider()

                    CostSummaryRow(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = "Other",
                        value = displayCost(otherCost)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            TraceabilityCard {
                Text(
                    text = "ADD / CALCULATE COST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = BeeftechPrimaryDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "TRANSPORT" to "Transport",
                        "PROCESSING" to "Processing"
                    ).forEachIndexed { index, option ->

                        if (index > 0) {
                            Spacer(modifier = Modifier.size(8.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                selectedCostType = option.first
                                costMessage = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text =
                                    option.second +
                                        if (
                                            selectedCostType ==
                                            option.first
                                        ) {
                                            " ✓"
                                        } else {
                                            ""
                                        }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "HANDLING" to "Handling",
                        "INTEREST" to "Interest"
                    ).forEachIndexed { index, option ->

                        if (index > 0) {
                            Spacer(modifier = Modifier.size(8.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                selectedCostType = option.first
                                costMessage = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text =
                                    option.second +
                                        if (
                                            selectedCostType ==
                                            option.first
                                        ) {
                                            " ✓"
                                        } else {
                                            ""
                                        }
                            )
                        }
                    }
                }

                if (
                    selectedCostType ==
                    "TRANSPORT"
                ) {

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TRANSPORT CALCULATOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityTextField(
                        label = "Distance (km)",
                        value = transportDistanceKm,
                        onValueChange = {
                            transportDistanceKm = it
                            costMessage = ""
                        },
                        icon = Icons.Outlined.LocalShipping,
                        placeholder = "e.g. 100",
                        helperText = "Total distance travelled for one trip.",
                        numeric = true,
                        decimal = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityTextField(
                        label = "Rate per km (R)",
                        value = transportRatePerKm,
                        onValueChange = {
                            transportRatePerKm = it
                            costMessage = ""
                        },
                        icon = Icons.Outlined.Payments,
                        placeholder = "e.g. 12.00",
                        numeric = true,
                        decimal = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityTextField(
                        label = "Number of Trips",
                        value = transportTrips,
                        onValueChange = {
                            transportTrips = it
                            costMessage = ""
                        },
                        icon = Icons.Outlined.LocalShipping,
                        placeholder = "1",
                        numeric = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityTextField(
                        label = "Animals Sharing Trip",
                        value = transportAnimals,
                        onValueChange = {
                            transportAnimals = it
                            costMessage = ""
                        },
                        icon = Icons.Outlined.LocalShipping,
                        placeholder = "1",
                        helperText = "The result is this animal's share of the transport cost.",
                        numeric = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityTextField(
                        label = "Extra Charges (R)",
                        value = transportExtras,
                        onValueChange = {
                            transportExtras = it
                            costMessage = ""
                        },
                        icon = Icons.Outlined.Payments,
                        placeholder = "0.00",
                        helperText = "Optional tolls or other transport charges allocated to this animal.",
                        numeric = true,
                        decimal = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilitySecondaryButton(
                        text = "Calculate Transport Cost",
                        icon = Icons.Outlined.LocalShipping,
                        onClick = {
                            val distance =
                                transportDistanceKm
                                    .replace(",", ".")
                                    .toDoubleOrNull()

                            val rate =
                                transportRatePerKm
                                    .replace(",", ".")
                                    .toDoubleOrNull()

                            val trips =
                                transportTrips
                                    .toIntOrNull()

                            val animals =
                                transportAnimals
                                    .toIntOrNull()

                            val extras =
                                transportExtras
                                    .replace(",", ".")
                                    .toDoubleOrNull()
                                    ?: 0.0

                            if (
                                distance == null ||
                                distance < 0.0 ||
                                rate == null ||
                                rate < 0.0 ||
                                trips == null ||
                                trips <= 0 ||
                                animals == null ||
                                animals <= 0 ||
                                extras < 0.0
                            ) {
                                costMessage =
                                    "Enter valid transport calculator values."
                            } else {
                                val calculated =
                                    (
                                        distance *
                                            rate *
                                            trips.toDouble() /
                                            animals.toDouble()
                                    ) +
                                        extras

                                costAmount =
                                    "%.2f".format(
                                        Locale.US,
                                        calculated
                                    )

                                costMessage = ""
                            }
                        }
                    )
                }

                if (
                    selectedCostType ==
                    "INTEREST"
                ) {

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "INTEREST CALCULATOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechPrimaryDark
                    )

                    val interestPrincipal =
                        (
                            (
                                totalAnimalCost
                                    .toDoubleOrNull()
                                    ?: 0.0
                            ) -
                                (
                                    interestCost
                                        .toDoubleOrNull()
                                        ?: 0.0
                                )
                        )
                            .coerceAtLeast(
                                0.0
                            )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text =
                            "Principal: " +
                                displayCost(
                                    interestPrincipal
                                        .toString()
                                ),
                        fontSize = 13.sp,
                        color = BeeftechMutedText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityTextField(
                        label = "Annual Interest Rate (%)",
                        value = interestAnnualRate,
                        onValueChange = {
                            interestAnnualRate = it
                            costMessage = ""
                        },
                        icon = Icons.Outlined.Savings,
                        placeholder = "e.g. 10",
                        numeric = true,
                        decimal = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityTextField(
                        label = "Interest Period (days)",
                        value = interestDays,
                        onValueChange = {
                            interestDays = it
                            costMessage = ""
                        },
                        icon = Icons.Outlined.Savings,
                        placeholder = "e.g. 30",
                        numeric = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilitySecondaryButton(
                        text = "Calculate Interest",
                        icon = Icons.Outlined.Savings,
                        onClick = {
                            val rate =
                                interestAnnualRate
                                    .replace(",", ".")
                                    .toDoubleOrNull()

                            val days =
                                interestDays
                                    .toIntOrNull()

                            if (
                                interestPrincipal <= 0.0 ||
                                rate == null ||
                                rate < 0.0 ||
                                days == null ||
                                days < 0
                            ) {
                                costMessage =
                                    "Enter a valid interest rate and period."
                            } else {
                                val calculated =
                                    interestPrincipal *
                                        (
                                            rate /
                                                100.0
                                        ) *
                                        days.toDouble() /
                                        365.0

                                costAmount =
                                    "%.2f".format(
                                        Locale.US,
                                        calculated
                                    )

                                costMessage = ""
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityTextField(
                    label = "Amount",
                    value = costAmount,
                    onValueChange = {
                        costAmount = it
                        costMessage = ""
                    },
                    icon = Icons.Outlined.Payments,
                    placeholder = "0.00",
                    helperText =
                        when (
                            selectedCostType
                        ) {
                            "TRANSPORT" ->
                                "Enter the final transport amount or use the calculator above."

                            "PROCESSING" ->
                                "Enter the actual processing amount. BeefTech does not invent a processing cost."

                            "HANDLING" ->
                                "Enter the actual handling amount."

                            else ->
                                "Enter interest manually or use the calculator above."
                        },
                    required = true,
                    numeric = true,
                    decimal = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityTextField(
                    label = "Description",
                    value = costNote,
                    onValueChange = {
                        costNote = it
                        costMessage = ""
                    },
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    placeholder = "Optional description",
                    helperText = "Describe what this cost was for when useful."
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityDatePickerField(
                    label = "Cost Date",
                    value = costDate,
                    onValueChange = {
                        costDate = it
                        costMessage = ""
                    },
                    helperText = "Tap the calendar to select when this cost was incurred.",
                    required = true,
                    maxToday = true
                )

                if (
                    costMessage.isNotBlank()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    TraceabilityFormMessage(
                        message = costMessage
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityPrimaryButton(
                    text = "Save Cost",
                    icon = Icons.Outlined.Save,
                    onClick = {
                        val amount =
                            costAmount
                                .replace(",", ".")
                                .toDoubleOrNull()

                        if (
                            amount == null ||
                            amount < 0.0
                        ) {
                            costMessage =
                                "Enter a valid cost amount."
                        } else {
                            onSaveCost(
                                selectedCostType,
                                costAmount,
                                costNote,
                                costDate
                            ) {
                                    success,
                                    message ->

                                if (
                                    success
                                ) {
                                    costAmount = ""
                                    costNote = ""
                                    costMessage = ""
                                } else {
                                    costMessage =
                                        message
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                "Cost Overview"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TotalCostCard(
                totalAnimalCost =
                    displayCost(totalAnimalCost),

                costPerKg =
                    costPerKg
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            displayCost(it)
                        }
                        ?: "Record mass",

                lastMassDate =
                    lastMassDate.ifBlank {
                        "Record mass"
                    }
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            TraceabilityCard {
                Text(
                    text = "RECORD CURRENT MASS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = BeeftechPrimaryDark
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                TraceabilityTextField(
                    label = "Mass (kg)",
                    value = massValue,
                    onValueChange = {
                        massValue = it
                        massMessage = ""
                    },
                    icon =
                        Icons.Outlined.MonitorWeight,
                    placeholder = "e.g. 218.5",
                    helperText = "Record a new measured mass. Cost per kg will recalculate automatically.",
                    required = true,
                    numeric = true,
                    decimal = true
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                TraceabilityDatePickerField(
                    label = "Weigh Date",
                    value = massDate,
                    onValueChange = {
                        massDate = it
                        massMessage = ""
                    },
                    helperText = "Tap the calendar to select the date this mass was measured.",
                    required = true,
                    maxToday = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityTextField(
                    label = "Mass Note",
                    value = massNote,
                    onValueChange = {
                        massNote = it
                        massMessage = ""
                    },
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    placeholder = "Optional note",
                    helperText = "Optional note about this weighing."
                )

                if (massMessage.isNotBlank()) {
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    TraceabilityFormMessage(
                        message = massMessage
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                TraceabilityPrimaryButton(
                    text = "Save Mass",
                    icon = Icons.Outlined.Save,
                    onClick = {
                        val mass =
                            massValue
                                .replace(",", ".")
                                .toDoubleOrNull()

                        if (
                            mass == null ||
                            mass <= 0.0
                        ) {
                            massMessage =
                                "Enter a valid mass greater than zero."
                        } else {
                            onSaveMass(
                                massValue,
                                massDate,
                                massNote
                            ) {
                                    success,
                                    message ->

                                if (success) {
                                    massValue =
                                        ""
                                    massNote =
                                        ""
                                    massMessage =
                                        ""
                                } else {
                                    massMessage =
                                        message
                                }
                            }
                        }
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
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
            .padding(
                vertical = 8.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    BeeftechSoftAccent,
                    RoundedCornerShape(
                        10.dp
                    )
                ),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BeeftechPrimaryDark,
                modifier =
                    Modifier.size(20.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.size(12.dp)
        )

        Text(
            text = title,
            modifier =
                Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight =
                FontWeight.SemiBold,
            color = BeeftechText
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight =
                FontWeight.Bold,
            color = BeeftechPrimaryDark
        )
    }
}

@Composable
private fun CostDivider() {
    HorizontalDivider(
        modifier =
            Modifier.padding(
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
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    BeeftechPrimaryDeep
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Color.White.copy(
                                alpha = 0.10f
                            ),
                            RoundedCornerShape(
                                11.dp
                            )
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.Payments,
                        contentDescription =
                            null,
                        tint =
                            BeeftechWhite,
                        modifier =
                            Modifier.size(21.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.size(12.dp)
                )

                Column {
                    Text(
                        text =
                            "TOTAL ANIMAL COST",
                        fontSize = 10.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        letterSpacing = 0.7.sp,
                        color =
                            BeeftechSoftAccent
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = totalAnimalCost,
                        fontSize = 27.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color = BeeftechWhite
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            HorizontalDivider(
                color =
                    Color.White.copy(
                        alpha = 0.16f
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(17.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                CostMetric(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Outlined.MonitorWeight,
                    label =
                        "COST / KG",
                    value =
                        costPerKg
                )

                CostMetric(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Outlined.CalendarMonth,
                    label =
                        "LAST MASS DATE",
                    value =
                        lastMassDate
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
            modifier =
                Modifier.size(18.dp)
        )

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight =
                FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = BeeftechSoftAccent
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight =
                FontWeight.SemiBold,
            color = BeeftechWhite
        )
    }
}

private fun displayCost(
    value: String
): String {
    val cleanValue = value.trim()

    return if (cleanValue.isBlank()) {
        "R 0.00"
    } else if (
        cleanValue.startsWith(
            "R",
            ignoreCase = true
        )
    ) {
        cleanValue
    } else {
        "R $cleanValue"
    }
}

@Preview(showBackground = true)
@Composable
private fun CostSummaryScreenPreview() {
    CostSummaryScreen(
        treatmentCost = "150.00",
        feedCost = "500.00",
        totalAnimalCost = "650.00"
    )
}
