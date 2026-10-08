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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beeftech.database.entity.AnimalPurchaseEntity
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
    entryMassCoverage: String = "",
    linkedFarm: String = "",
    supplierOptions: List<String> = emptyList(),
    supplierRecords: List<AnimalPurchaseEntity> = emptyList(),
    onBackClick: () -> Unit = {},
    onSupplierNameChange: (String) -> Unit = {},
    onGlnNumberChange: (String) -> Unit = {},
    onPurchaseDateChange: (String) -> Unit = {},
    onPurchaseBatchChange: (String) -> Unit = {},
    onViewFarmClick: () -> Unit = {},
    onRecordMassClick: () -> Unit = {},
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

    val todayPurchaseDate = remember {
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    var purchaseDateState by remember(purchaseDate) {
        mutableStateOf(
            purchaseDate.ifBlank {
                todayPurchaseDate
            }
        )
    }

    val purchaseBatchState =
        remember(
            purchaseBatch,
            supplierNameState,
            purchaseDateState
        ) {
            purchaseBatch
                .trim()
                .takeIf {
                    it.isNotBlank()
                }
                ?: generatePurchaseBatchNumber(
                    supplierName =
                        supplierNameState,
                    purchaseDate =
                        purchaseDateState
                )
        }

    /*
     * Keep the existing callback contract alive for any caller that wants
     * to observe the automatically supplied values.
     */
    LaunchedEffect(
        purchaseDateState,
        purchaseBatchState
    ) {
        onPurchaseDateChange(
            purchaseDateState
        )

        onPurchaseBatchChange(
            purchaseBatchState
        )
    }

    var validationMessage by remember {
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
                "FARM TRACEABILITY"
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
                title = "Supplier Details"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilitySearchableDropdown(
                    label = "Supplier Name",
                    value = supplierNameState,
                    options = supplierOptions,
                    icon = Icons.Outlined.Person,
                    placeholder = "Search registered supplier or enter new supplier",
                    helperText = "Select an existing supplier, or type a new supplier name if it is not listed.",
                    required = true,
                    allowCustomEntry = true,
                    onValueChange = {
                        supplierNameState = it
                        validationMessage = ""
                        onSupplierNameChange(it)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "GLN Number",
                    value = glnNumberState,
                    onValueChange = { rawValue ->
                        val normalized =
                            rawValue
                                .filter {
                                    it.isDigit()
                                }
                                .take(13)

                        glnNumberState =
                            normalized

                        validationMessage =
                            ""

                        onGlnNumberChange(
                            normalized
                        )
                    },
                    icon = Icons.Outlined.Numbers,
                    placeholder = "e.g. 6001234567894",
                    helperText = "Enter the supplier's 13-digit Global Location Number. Leave blank when no GLN is available.",
                    numeric = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityDatePickerField(
                    label = "Date of Purchase",
                    value = purchaseDateState,
                    onValueChange = {
                        purchaseDateState = it
                        validationMessage = ""
                    },
                    helperText = "Set to today automatically. Tap the calendar to choose an earlier purchase date.",
                    required = true,
                    maxToday = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Purchase Batch Number",
                    value = purchaseBatchState,
                    onValueChange = {},
                    icon = Icons.Outlined.Tag,
                    placeholder = "Generated after selecting a supplier",
                    helperText = "Generated automatically from the supplier and purchase date. Animals from the same supplier purchase date reuse the same batch reference.",
                    readOnly = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                title = "Batch Overview"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Groups,
                    title = "Head in Batch",
                    subtitle = if (headInBatch.isBlank()) {
                        "Batch quantity unavailable"
                    } else {
                        "Number of animals"
                    },
                    value = headInBatch
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityInfoRow(
                    icon = Icons.Outlined.MonitorWeight,
                    title = "Average Entry Mass",
                    subtitle =
                        when {
                            averageEntryMass.isNotBlank() &&
                                entryMassCoverage.isNotBlank() -> {

                                entryMassCoverage
                            }

                            averageEntryMass.isNotBlank() -> {

                                "Recorded batch average"
                            }

                            headInBatch.isNotBlank() -> {

                                "No entry masses recorded for this batch"
                            }

                            else -> {

                                "Save a purchase batch to calculate the average"
                            }
                        },
                    value = averageEntryMass
                )

                if (
                    headInBatch.isNotBlank() &&
                    averageEntryMass.isBlank()
                ) {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    TraceabilitySecondaryButton(
                        text = "Record / Update Mass",
                        icon = Icons.Outlined.MonitorWeight,
                        onClick = onRecordMassClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                title = "Farm Location"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Linked Farm",
                    subtitle = linkedFarm.ifBlank {
                        "Supplier farm information unavailable"
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilitySecondaryButton(
                    text = "View Farm",
                    icon = Icons.Outlined.Map,
                    onClick = onViewFarmClick
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            TraceabilityFormMessage(
                message = validationMessage
            )

            if (validationMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            TraceabilityPrimaryButton(
                text = "Save Supplier",
                icon = Icons.Outlined.Save,
                onClick = {
                    validationMessage =
                        when {
                            supplierNameState
                                .trim()
                                .isBlank() -> {

                                "Select or enter a supplier name."
                            }

                            glnNumberState
                                .isNotBlank() &&
                                glnNumberState.length != 13 -> {

                                "GLN must contain exactly 13 digits."
                            }

                            !isValidPurchaseDate(
                                purchaseDateState
                            ) -> {

                                "Purchase date must use DD/MM/YYYY."
                            }

                            else -> {
                                ""
                            }
                        }

                    if (validationMessage.isBlank()) {
                        onSaveClick(
                            supplierNameState.trim(),
                            glnNumberState.trim(),
                            purchaseDateState.trim(),
                            purchaseBatchState.trim()
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            TraceabilitySectionTitle(
                "Supplier History"
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (supplierRecords.isEmpty()) {
                TraceabilityCard {
                    Text(
                        text = "No supplier details yet. Supplier and purchase information will appear here after it is saved.",
                        color = BeeftechMutedText
                    )
                }
            } else {
                supplierRecords.forEach { record ->
                    TraceabilityCard {
                        Text(
                            text = record.sellerName,
                            fontWeight = FontWeight.Bold,
                            color = BeeftechText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!record.glnNumber.isNullOrBlank()) {
                            Text(
                                text = "GLN: ${record.glnNumber}",
                                color = BeeftechMutedText
                            )
                        }

                        if (!record.purchaseBatchNumber.isNullOrBlank()) {
                            Text(
                                text = "Batch: ${record.purchaseBatchNumber}",
                                color = BeeftechMutedText
                            )
                        }

                        val notes = record.notes
                        if (!notes.isNullOrBlank()) {
                            Text(
                                text = notes,
                                color = BeeftechMutedText
                            )
                        }

                        Text(
                            text = "Purchase date: " + SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            ).format(Date(record.purchaseDate)),
                            color = BeeftechMutedText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun generatePurchaseBatchNumber(
    supplierName: String,
    purchaseDate: String
): String {
    val normalizedSupplier =
        supplierName
            .trim()
            .lowercase(
                Locale.ROOT
            )

    if (
        normalizedSupplier.isBlank() ||
        !isValidPurchaseDate(
            purchaseDate
        )
    ) {
        return ""
    }

    val supplierPrefix =
        normalizedSupplier
            .filter {
                it.isLetterOrDigit()
            }
            .uppercase(
                Locale.ROOT
            )
            .take(3)
            .ifBlank {
                "SUP"
            }

    val supplierHash =
        Integer
            .toHexString(
                normalizedSupplier
                    .hashCode()
            )
            .uppercase(
                Locale.ROOT
            )
            .padStart(
                8,
                '0'
            )
            .takeLast(4)

    val dateToken =
        purchaseDate
            .trim()
            .let {
                    value ->

                val match =
                    Regex(
                        """^(\d{2})/(\d{2})/(\d{4})$"""
                    )
                        .matchEntire(
                            value
                        )

                if (
                    match == null
                ) {
                    ""
                } else {
                    val day =
                        match
                            .groupValues[1]

                    val month =
                        match
                            .groupValues[2]

                    val year =
                        match
                            .groupValues[3]

                    "$year$month$day"
                }
            }

    return "PB-$dateToken-$supplierPrefix-$supplierHash"
}


private fun isValidPurchaseDate(
    value: String
): Boolean {
    val normalized =
        value.trim()

    if (
        normalized.isBlank()
    ) {
        return false
    }

    return try {
        val formatter =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.US
            )
                .apply {
                    isLenient = false
                }

        val parsed =
            formatter.parse(
                normalized
            )

        parsed != null &&
            formatter.format(
                parsed
            ) == normalized

    } catch (
        _: Exception
    ) {
        false
    }
}


@Preview(showBackground = true)
@Composable
private fun SupplierScreenPreview() {
    SupplierScreen()
}
