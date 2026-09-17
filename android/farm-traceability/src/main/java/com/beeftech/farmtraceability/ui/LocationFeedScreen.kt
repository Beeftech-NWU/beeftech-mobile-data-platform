package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beeftech.database.entity.LocationFeed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocationFeedScreen(
    animalReference: String = "",
    destination: String = "",
    daysInDestination: String = "",
    rationName: String = "",
    rationDays: String = "",
    rationCost: String = "",
    destinationOptions: List<String> = emptyList(),
    rationOptions: List<String> = emptyList(),
    locationFeedRecords: List<LocationFeed> = emptyList(),
    onBackClick: () -> Unit = {},
    onDestinationChange: (String) -> Unit = {},
    onDaysInDestinationChange: (String) -> Unit = {},
    onRationNameChange: (String) -> Unit = {},
    onRationDaysChange: (String) -> Unit = {},
    onRationCostChange: (String) -> Unit = {},
    onAddRationClick: () -> Unit = {},
    onSaveClick: (
        destination: String,
        daysInDestination: String,
        rationName: String,
        rationDays: String,
        rationCost: String
    ) -> Unit = { _, _, _, _, _ -> }
) {
    var destinationState by remember(destination) {
        mutableStateOf(destination)
    }

    var daysState by remember(daysInDestination) {
        mutableStateOf(daysInDestination)
    }

    var rationNameState by remember(rationName) {
        mutableStateOf(rationName)
    }

    var rationDaysState by remember(rationDays) {
        mutableStateOf(rationDays)
    }

    var rationCostState by remember(rationCost) {
        mutableStateOf(rationCost)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(
                rememberScrollState()
            )
    ) {
        TraceabilityHeader(
            eyebrow =
                if (animalReference.isBlank()) {
                    "FARM TRACEABILITY"
                } else {
                    "ANIMAL $animalReference"
                },
            title = "Location & Feed",
            subtitle =
                "Destination and ration information",
            icon =
                Icons.Outlined.LocationOn,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            TraceabilitySectionTitle(
                title = "Location"
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            TraceabilityCard {
                TraceabilitySearchableDropdown(
                    label = "Destination",
                    value = destinationState,
                    options =
                        destinationOptions,
                    icon =
                        Icons.Outlined.LocationOn,
                    onValueChange = {
                        destinationState = it
                        onDestinationChange(it)
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                TraceabilityTextField(
                    label =
                        "Days in Destination",
                    value = daysState,
                    onValueChange = {
                        daysState = it
                        onDaysInDestinationChange(
                            it
                        )
                    },
                    icon =
                        Icons.Outlined.Numbers
                )
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            TraceabilitySectionTitle(
                title = "Ration Entry"
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            TraceabilityCard {
                TraceabilityDropdown(
                    label = "Ration",
                    value = rationNameState,
                    options = rationOptions,
                    icon =
                        Icons.Outlined.Restaurant,
                    onValueChange = {
                        rationNameState = it
                        onRationNameChange(it)
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                TraceabilityTextField(
                    label = "Days",
                    value = rationDaysState,
                    onValueChange = {
                        rationDaysState = it
                        onRationDaysChange(it)
                    },
                    icon =
                        Icons.Outlined.Numbers
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                TraceabilityTextField(
                    label = "Cost",
                    value = rationCostState,
                    onValueChange = {
                        rationCostState = it
                        onRationCostChange(it)
                    },
                    icon =
                        Icons.Outlined.Payments
                )

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                TraceabilitySecondaryButton(
                    text =
                        "Add Ration Entry",
                    icon =
                        Icons.Outlined.Add,
                    onClick =
                        onAddRationClick
                )
            }

            Spacer(
                modifier =
                    Modifier.height(26.dp)
            )

            TraceabilityPrimaryButton(
                text =
                    "Save Location & Feed",
                icon =
                    Icons.Outlined.Save,
                onClick = {
                    onSaveClick(
                        destinationState,
                        daysState,
                        rationNameState,
                        rationDaysState,
                        rationCostState
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )

            TraceabilitySectionTitle(
                "Location & Feed History"
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (
                locationFeedRecords.isEmpty()
            ) {
                TraceabilityCard {
                    Text(
                        text =
                            "No location and feed records found.",
                        color =
                            BeeftechMutedText
                    )
                }
            } else {
                locationFeedRecords
                    .forEach { record ->

                        TraceabilityCard {
                            Text(
                                text =
                                    record.destination,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    BeeftechText
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        6.dp
                                    )
                            )

                            Text(
                                text =
                                    "Days in destination: ${record.daysInDestination}",
                                color =
                                    BeeftechMutedText
                            )

                            Text(
                                text =
                                    "Ration: ${record.rationName}",
                                color =
                                    BeeftechMutedText
                            )

                            Text(
                                text =
                                    "Ration days: ${record.rationDays}",
                                color =
                                    BeeftechMutedText
                            )

                            Text(
                                text =
                                    "Ration cost: R%.2f"
                                        .format(
                                            record.rationCost
                                        ),
                                color =
                                    BeeftechMutedText
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )

                            val formattedDate =
                                SimpleDateFormat(
                                    "dd MMM yyyy HH:mm",
                                    Locale
                                        .getDefault()
                                ).format(
                                    Date(
                                        record.timestamp
                                    )
                                )

                            Text(
                                text =
                                    formattedDate,
                                color =
                                    BeeftechMutedText
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )
                    }
            }

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationFeedScreenPreview() {
    LocationFeedScreen()
}