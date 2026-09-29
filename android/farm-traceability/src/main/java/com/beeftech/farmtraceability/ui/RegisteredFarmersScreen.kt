package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beeftech.database.entity.FarmerEntity

@Composable
fun RegisteredFarmersScreen(
    farmers: List<FarmerEntity>,
    isLoading: Boolean = false,
    errorMessage: String = "",
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
            title = "Registered Farmers",
            subtitle = "Select a farmer to view their profile",
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
