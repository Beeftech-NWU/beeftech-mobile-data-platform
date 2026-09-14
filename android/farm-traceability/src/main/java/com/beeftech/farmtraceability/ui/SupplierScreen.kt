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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tag
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
import com.beeftech.database.entity.Supplier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierScreen(
    animalReference: String = "",
    supplierName: String = "",
    glnNumber: String = "",
    purchaseDate: String = "",
    purchaseBatch: String = "",
    headInBatch: String = "",
    averageEntryMass: String = "",
    supplierRecords: List<Supplier> = emptyList(),
    onBackClick: () -> Unit = {},
    onSupplierNameChange: (String) -> Unit = {},
    onGlnNumberChange: (String) -> Unit = {},
    onPurchaseDateChange: (String) -> Unit = {},
    onPurchaseBatchChange: (String) -> Unit = {},
    onViewFarmClick: () -> Unit = {},
    onSaveClick: (
        supplierName: String,
        glnNumber: String,
        purchaseDate: String,
        purchaseBatchNumber: String
    ) -> Unit = { _, _, _, _ -> }
) {

    var supplierNameState by remember(supplierName) {
        mutableStateOf(supplierName)
    }

    var glnNumberState by remember(glnNumber) {
        mutableStateOf(glnNumber)
    }

    var purchaseDateState by remember(purchaseDate) {
        mutableStateOf(purchaseDate)
    }

    var purchaseBatchState by remember(purchaseBatch) {
        mutableStateOf(purchaseBatch)
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
            title = "Supplier",
            subtitle = "Origin and purchase information",
            icon = Icons.Outlined.LocalShipping,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            TraceabilitySectionTitle(
                "Supplier Details"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TraceabilityCard {

                TraceabilityTextField(
                    label = "Name",
                    value = supplierNameState,
                    onValueChange = {
                        supplierNameState = it
                        onSupplierNameChange(it)
                    },
                    icon = Icons.Outlined.Person
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                TraceabilityTextField(
                    label = "GLN Number",
                    value = glnNumberState,
                    onValueChange = {
                        glnNumberState = it
                        onGlnNumberChange(it)
                    },
                    icon = Icons.Outlined.Numbers
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                TraceabilityTextField(
                    label = "Date of Purchase",
                    value = purchaseDateState,
                    onValueChange = {
                        purchaseDateState = it
                        onPurchaseDateChange(it)
                    },
                    icon = Icons.Outlined.CalendarMonth
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                TraceabilityTextField(
                    label = "Purchase Batch Number",
                    value = purchaseBatchState,
                    onValueChange = {
                        purchaseBatchState = it
                        onPurchaseBatchChange(it)
                    },
                    icon = Icons.Outlined.Tag
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TraceabilitySectionTitle(
                "Batch Overview"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TraceabilityCard {

                TraceabilityInfoRow(
                    icon = Icons.Outlined.Groups,
                    title = "Head in Batch",
                    subtitle = "Number of animals",
                    value = headInBatch
                )

                TraceabilityInfoRow(
                    icon = Icons.Outlined.MonitorWeight,
                    title = "Average Entry Mass",
                    subtitle = "Recorded batch average",
                    value = averageEntryMass
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TraceabilitySectionTitle(
                "Farm Location"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TraceabilityCard {

                TraceabilityInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Linked Farm",
                    subtitle = "Supplier farm location"
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                TraceabilitySecondaryButton(
                    text = "View Farm",
                    icon = Icons.Outlined.Map,
                    onClick = onViewFarmClick
                )
            }

            Spacer(
                modifier = Modifier.height(26.dp)
            )

            TraceabilityPrimaryButton(
                text = "Save Supplier",
                icon = Icons.Outlined.Save,
                onClick = {

                    onSaveClick(
                        supplierNameState,
                        glnNumberState,
                        purchaseDateState,
                        purchaseBatchState
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            TraceabilitySectionTitle(
                "Supplier History"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (supplierRecords.isEmpty()) {

                TraceabilityCard {

                    Text(
                        text = "No supplier records found.",
                        color = BeeftechMutedText
                    )
                }

            } else {

                supplierRecords.forEach { record ->

                    TraceabilityCard {

                        Text(
                            text = record.supplierName,
                            fontWeight = FontWeight.Bold,
                            color = BeeftechText
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "GLN: ${record.glnNumber}",
                            color = BeeftechMutedText
                        )

                        Text(
                            text =
                                "Purchase date: ${record.purchaseDate}",
                            color = BeeftechMutedText
                        )

                        Text(
                            text =
                                "Batch: ${record.purchaseBatchNumber}",
                            color = BeeftechMutedText
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        val formattedDate =
                            SimpleDateFormat(
                                "dd MMM yyyy HH:mm",
                                Locale.getDefault()
                            ).format(
                                Date(record.timestamp)
                            )

                        Text(
                            text = formattedDate,
                            color = BeeftechMutedText
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SupplierScreenPreview() {

    SupplierScreen()
}