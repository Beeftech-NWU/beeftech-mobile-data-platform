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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LocationFeedScreen(
    animalReference: String = "",
    destination: String = "",
    daysInDestination: String = "",
    rationName: String = "",
    rationDays: String = "",
    rationCost: String = "",
    onBackClick: () -> Unit = {},
    onDestinationChange: (String) -> Unit = {},
    onDaysInDestinationChange: (String) -> Unit = {},
    onRationNameChange: (String) -> Unit = {},
    onRationDaysChange: (String) -> Unit = {},
    onRationCostChange: (String) -> Unit = {},
    onAddRationClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
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
            .verticalScroll(rememberScrollState())
    ) {

        TraceabilityHeader(
            eyebrow = if (animalReference.isBlank()) {
                "ANIMAL"
            } else {
                "ANIMAL $animalReference"
            },
            title = "Location & Feed",
            subtitle = "Destination and ration information",
            icon = Icons.Outlined.LocationOn,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            TraceabilitySectionTitle("Location")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityTextField(
                    label = "Destination",
                    value = destinationState,
                    onValueChange = {
                        destinationState = it
                        onDestinationChange(it)
                    },
                    icon = Icons.Outlined.LocationOn
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Days in Destination",
                    value = daysState,
                    onValueChange = {
                        daysState = it
                        onDaysInDestinationChange(it)
                    },
                    icon = Icons.Outlined.Numbers
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("On Ration")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityTextField(
                    label = "Ration Name",
                    value = rationNameState,
                    onValueChange = {
                        rationNameState = it
                        onRationNameChange(it)
                    },
                    icon = Icons.Outlined.Restaurant
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Days",
                    value = rationDaysState,
                    onValueChange = {
                        rationDaysState = it
                        onRationDaysChange(it)
                    },
                    icon = Icons.Outlined.Numbers
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Cost",
                    value = rationCostState,
                    onValueChange = {
                        rationCostState = it
                        onRationCostChange(it)
                    },
                    icon = Icons.Outlined.Payments
                )

                Spacer(modifier = Modifier.height(18.dp))

                TraceabilitySecondaryButton(
                    text = "Add Ration Entry",
                    icon = Icons.Outlined.Add,
                    onClick = onAddRationClick
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            TraceabilityPrimaryButton(
                text = "Save Location & Feed",
                icon = Icons.Outlined.Save,
                onClick = onSaveClick
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationFeedScreenPreview() {
    LocationFeedScreen()
}