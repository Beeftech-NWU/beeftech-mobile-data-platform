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
    organisationName: String = "",
    clientCode: String = "",
    emailAddress: String = "",
    vatNumber: String = "",
    coRegIdNo: String = "",
    landOwnership: String = "",
    faCodeRmis: String = "",
    glnNumber: String = "",
    businessRoles: String = "",
    farmAddress: String = "",
    streetCode: String = "",
    postalAddress: String = "",
    country: String = "",
    gpsCoordinates: String = "",
    syncStatus: String = "",
    isLoading: Boolean = false,
    errorMessage: String = "",
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
            subtitle = "View registered farmer and location information",
            icon = Icons.Outlined.HomeWork,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            if (isLoading) {
                TraceabilityCard {
                    TraceabilityInfoRow(
                        icon = Icons.Outlined.Person,
                        title = "Loading",
                        subtitle = "Loading farmer profile..."
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (errorMessage.isNotBlank()) {
                TraceabilityCard {
                    TraceabilityInfoRow(
                        icon = Icons.Outlined.Person,
                        title = "Profile unavailable",
                        subtitle = errorMessage
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            TraceabilitySectionTitle(
                title = "Farmer Details"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Person,
                    title = "Organisation Name",
                    subtitle = organisationName.ifBlank {
                        "Organisation information unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "Client Code",
                    subtitle = clientCode.ifBlank {
                        "Client code unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Person,
                    title = "Email Address",
                    subtitle = emailAddress.ifBlank {
                        "Email address unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "VAT Number",
                    subtitle = vatNumber.ifBlank {
                        "VAT number unavailable"
                    }
                )


                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "Co-Reg / ID No.",
                    subtitle = coRegIdNo.ifBlank {
                        "Co-Reg / ID number unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.HomeWork,
                    title = "Land Ownership",
                    subtitle = landOwnership.ifBlank {
                        "Land ownership unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "FA Code (RMIS)",
                    subtitle = faCodeRmis.ifBlank {
                        "FA code unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "GLN Number",
                    subtitle = glnNumber.ifBlank {
                        "GLN number unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.HomeWork,
                    title = "Business Role(s)",
                    subtitle = businessRoles.ifBlank {
                        "Business role information unavailable"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                title = "Farm Location"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Home,
                    title = "Farm Address",
                    subtitle = farmAddress.ifBlank {
                        "Farm address unavailable"
                    }
                )


                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Home,
                    title = "Postal Address",
                    subtitle = postalAddress.ifBlank {
                        "Postal address unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "Street Code",
                    subtitle = streetCode.ifBlank {
                        "Street code unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Country",
                    subtitle = country.ifBlank {
                        "Country unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "GPS Coordinates",
                    subtitle = gpsCoordinates.ifBlank {
                        "Location information unavailable"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                title = "Sync Information"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Tag,
                    title = "Sync Status",
                    subtitle = when (syncStatus.uppercase()) {
                        "PENDING" -> "Pending Sync"
                        "PROCESSING" -> "Processing"
                        "SYNCED" -> "Registered"
                        else -> syncStatus.ifBlank {
                            "Sync status unavailable"
                        }
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
    FarmerFarmProfileScreen(
        organisationName = "Example Farm",
        clientCode = "TEST20",
        emailAddress = "farmer@example.com",
        vatNumber = "1234567890",
        coRegIdNo = "9608551/07",
        landOwnership = "Owned",
        faCodeRmis = "FA-RMIS-01",
        glnNumber = "GLN-123",
        businessRoles = "Buyer, Supplier",
        farmAddress = "1 Example Road, Gauteng, 1459",
        streetCode = "1459",
        postalAddress = "PO Box 117",
        country = "South Africa",
        gpsCoordinates = "-26.2041, 28.0473",
        syncStatus = "SYNCED"
    )
}
