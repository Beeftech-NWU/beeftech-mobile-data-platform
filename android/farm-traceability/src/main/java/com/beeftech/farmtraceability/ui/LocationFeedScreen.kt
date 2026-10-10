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
import com.beeftech.database.entity.AnimalMovementEntity
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
    locationFeedRecords: List<AnimalMovementEntity> = emptyList(),
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

    var validationMessage by remember {
        mutableStateOf("")
    }

    val destinationHelperText =
        if (
            destinationOptions.isEmpty()
        ) {
            "No saved farms, locations or pens are available yet. Enter a destination manually."
        } else {
            "${destinationOptions.size} saved destination option${if (destinationOptions.size == 1) "" else "s"} loaded from registered farms, locations and pens. You can also enter a new destination."
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
                    options = destinationOptions,
                    icon = Icons.Outlined.LocationOn,
                    placeholder = "Search, select or enter destination",
                    helperText = destinationHelperText,
                    required = true,
                    allowCustomEntry = true,
                    onValueChange = {
                        destinationState = it
                        validationMessage = ""
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
                    icon = Icons.Outlined.Numbers,
                    placeholder = "0",
                    helperText = "Number of days at this destination.",
                    numeric = true
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
                TraceabilitySearchableDropdown(
                    label = "Ration",
                    value = rationNameState,
                    options = rationOptions,
                    icon = Icons.Outlined.Restaurant,
                    placeholder = "Search, select or enter ration",
                    helperText = "Choose a saved ration or enter a new ration name if it is not listed.",
                    required = true,
                    allowCustomEntry = true,
                    onValueChange = {
                        rationNameState = it
                        validationMessage = ""
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
                    icon = Icons.Outlined.Numbers,
                    placeholder = "0",
                    helperText = "Number of days this ration was provided.",
                    numeric = true
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
                    icon = Icons.Outlined.Payments,
                    placeholder = "0.00",
                    helperText = "Enter the ration cost in ZAR.",
                    decimal = true
                )


            }

            Spacer(
                modifier =
                    Modifier.height(26.dp)
            )

            TraceabilityFormMessage(
                message = validationMessage
            )

            if (validationMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            TraceabilityPrimaryButton(
                text =
                    "Save Location & Feed",
                icon =
                    Icons.Outlined.Save,
                onClick = {
                    validationMessage = when {
                        destinationState.trim().isBlank() ->
                            "Select or enter a destination."
                        rationNameState.trim().isBlank() ->
                            "Select or enter a ration."
                        else -> ""
                    }

                    if (validationMessage.isBlank()) {
                        onSaveClick(
                            destinationState.trim(),
                            daysState.trim(),
                            rationNameState.trim(),
                            rationDaysState.trim(),
                            rationCostState.trim()
                        )
                    }
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
                            "No location and feed records yet. Saved pen and ration information will appear here.",
                        color =
                            BeeftechMutedText
                    )
                }
            } else {
                locationFeedRecords.forEach { record ->
                    TraceabilityCard {
                        Text(
                            text = record.destinationFarmId,
                            fontWeight = FontWeight.Bold,
                            color = BeeftechText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val feedType = record.feedLocationType
                        if (!feedType.isNullOrEmpty()) {
                            Text(
                                text = "Ration: $feedType",
                                color = BeeftechMutedText
                            )
                        }

                        val notes = record.notes
                        if (!notes.isNullOrEmpty()) {
                            Text(
                                text = notes,
                                color = BeeftechMutedText
                            )
                        }

                        Text(
                            text = "Date: " + SimpleDateFormat(
                                "dd MMM yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date(record.movementDate)),
                            color = BeeftechMutedText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
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
