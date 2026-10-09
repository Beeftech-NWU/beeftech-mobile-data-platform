package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beeftech.database.entity.FarmerEntity

@Composable
fun RegisteredFarmersScreen(
    farmers: List<FarmerEntity>,
    isLoading: Boolean = false,
    errorMessage: String = "",
    selectingForAssignment: Boolean = false,
    onFarmerClick: (String) -> Unit,
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
            title = if (selectingForAssignment) "Select Farmer" else "Registered Farmers",
            subtitle = if (selectingForAssignment) "Choose the farmer receiving the animal" else "Select a farmer to view their profile",
            icon = Icons.Outlined.Person,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            TraceabilitySectionTitle(
                title = "Farmers"
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    TraceabilityCard {
                        TraceabilityInfoRow(
                            icon = Icons.Outlined.Person,
                            title = "Loading",
                            subtitle = "Loading registered farmers..."
                        )
                    }
                }

                errorMessage.isNotBlank() -> {
                    TraceabilityCard {
                        TraceabilityInfoRow(
                            icon = Icons.Outlined.Person,
                            title = "Unable to load farmers",
                            subtitle = errorMessage
                        )
                    }
                }

                farmers.isEmpty() -> {
                    TraceabilityCard {
                        TraceabilityInfoRow(
                            icon = Icons.Outlined.Person,
                            title = "No registered farmers",
                            subtitle = "No farmer records are available yet."
                        )
                    }
                }

                else -> {
                    farmers
                        .sortedWith(
                            compareBy<FarmerEntity> {
                                it.organisation_name.orEmpty()
                            }.thenBy {
                                it.client_code.orEmpty()
                            }
                        )
                        .forEach { farmer ->

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onFarmerClick(
                                            farmer.farmer_id
                                        )
                                    }
                            ) {
                                TraceabilityCard {
                                    TraceabilityInfoRow(
                                        icon = Icons.Outlined.Person,
                                        title = "Organisation",
                                        subtitle =
                                            farmer.organisation_name
                                                ?.takeIf {
                                                    it.isNotBlank()
                                                }
                                                ?: "Organisation unavailable"
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(14.dp)
                                    )

                                    TraceabilityInfoRow(
                                        icon = Icons.Outlined.Tag,
                                        title = "Client Code",
                                        subtitle =
                                            farmer.client_code
                                                ?.takeIf {
                                                    it.isNotBlank()
                                                }
                                                ?: "Client code unavailable"
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(14.dp)
                                    )

                                    FarmerRegistrationStatusPill(
                                        syncStatus =
                                            farmer.sync_status
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun FarmerRegistrationStatusPill(
    syncStatus: String
) {
    val normalizedStatus =
        syncStatus.trim().uppercase()

    val label =
        when (normalizedStatus) {
            "PENDING" -> "Pending Sync"
            "PROCESSING" -> "Processing"
            "SYNCED" -> "Registered"
            else ->
                syncStatus
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: "Status unavailable"
        }

    val backgroundColor =
        when (normalizedStatus) {
            "PENDING" -> Color(0xFFFFF3CD)
            "PROCESSING" -> Color(0xFFDCEBFF)
            "SYNCED" -> Color(0xFFDDF3E4)
            else -> Color(0xFFE9ECEF)
        }

    val textColor =
        when (normalizedStatus) {
            "PENDING" -> Color(0xFF795500)
            "PROCESSING" -> Color(0xFF174A7E)
            "SYNCED" -> Color(0xFF1B5E20)
            else -> Color(0xFF495057)
        }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Registration Status",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                )
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}