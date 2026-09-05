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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FarmerFarmProfileScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onCaptureLocationClick: () -> Unit = {}
) {

    var firstName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    var farmName by remember { mutableStateOf("") }
    var farmReference by remember { mutableStateOf("") }
    var farmAddress by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {

        TraceabilityHeader(
            eyebrow = "FARM TRACEABILITY",
            title = "Farmer & Farm Profile",
            subtitle = "Farmer, farm and location information",
            icon = Icons.Outlined.HomeWork,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            TraceabilitySectionTitle("Farmer Details")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityTextField(
                    label = "First Name",
                    value = firstName,
                    onValueChange = { firstName = it },
                    icon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Surname",
                    value = surname,
                    onValueChange = { surname = it },
                    icon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Contact Number",
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    icon = Icons.Outlined.Call
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("Farm Details")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityTextField(
                    label = "Farm Name",
                    value = farmName,
                    onValueChange = { farmName = it },
                    icon = Icons.Outlined.Home
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Farm Reference",
                    value = farmReference,
                    onValueChange = { farmReference = it },
                    icon = Icons.Outlined.Tag
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Farm Address",
                    value = farmAddress,
                    onValueChange = { farmAddress = it },
                    icon = Icons.Outlined.LocationOn,
                    singleLine = false,
                    minLines = 3
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("Farm Location")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "GPS Coordinates",
                    subtitle = "No location captured"
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilitySecondaryButton(
                    text = "Capture Current Location",
                    icon = Icons.Outlined.MyLocation,
                    onClick = onCaptureLocationClick
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            TraceabilityPrimaryButton(
                text = "Save Profile",
                icon = Icons.Outlined.Save,
                onClick = onSaveClick
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FarmerFarmProfileScreenPreview() {
    FarmerFarmProfileScreen()
}