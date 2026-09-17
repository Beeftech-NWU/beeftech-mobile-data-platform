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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FarmerFarmProfileScreen(
    farmerName: String = "",
    contactNumber: String = "",
    farmName: String = "",
    farmReference: String = "",
    farmAddress: String = "",
    gpsCoordinates: String = "",
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TraceabilityHeader(
            eyebrow = "FARM TRACEABILITY",
            title = "Farmer & Farm Profile",
            subtitle = "View farmer, farm and location information",
            icon = Icons.Outlined.HomeWork,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            // Farmer Details
            TraceabilitySectionTitle(
                title = "Farmer Details"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Person,
                    title = "Farmer Name",
                    subtitle = farmerName.ifBlank {
                        "Farmer information unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Call,
                    title = "Contact Number",
                    subtitle = contactNumber.ifBlank {
                        "Contact information unavailable"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Farm Details
            TraceabilitySectionTitle(
                title = "Farm Details"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Home,
                    title = "Farm Name",
                    subtitle = farmName.ifBlank {
                        "Farm information unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "Farm Reference",
                    subtitle = farmReference.ifBlank {
                        "Farm reference unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Farm Address",
                    subtitle = farmAddress.ifBlank {
                        "Farm address unavailable"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Farm Location
            TraceabilitySectionTitle(
                title = "Farm Location"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "GPS Coordinates",
                    subtitle = gpsCoordinates.ifBlank {
                        "Location information unavailable"
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FarmerFarmProfileScreenPreview() {
    FarmerFarmProfileScreen()
}