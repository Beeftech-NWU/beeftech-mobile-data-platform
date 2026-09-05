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
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TreatmentsScreen(
    animalReference: String = "",
    disease: String = "",
    treatment: String = "",
    batchNumber: String = "",
    volumeUsed: String = "",
    cost: String = "",
    onBackClick: () -> Unit = {},
    onDiseaseChange: (String) -> Unit = {},
    onTreatmentChange: (String) -> Unit = {},
    onBatchNumberChange: (String) -> Unit = {},
    onVolumeUsedChange: (String) -> Unit = {},
    onCostChange: (String) -> Unit = {},
    onAddTreatmentClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {

    var diseaseState by remember(disease) {
        mutableStateOf(disease)
    }

    var treatmentState by remember(treatment) {
        mutableStateOf(treatment)
    }

    var batchState by remember(batchNumber) {
        mutableStateOf(batchNumber)
    }

    var volumeState by remember(volumeUsed) {
        mutableStateOf(volumeUsed)
    }

    var costState by remember(cost) {
        mutableStateOf(cost)
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
            title = "Treatments",
            subtitle = "Disease and medication records",
            icon = Icons.Outlined.Medication,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            TraceabilitySectionTitle("Disease")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityTextField(
                    label = "Disease",
                    value = diseaseState,
                    onValueChange = {
                        diseaseState = it
                        onDiseaseChange(it)
                    },
                    icon = Icons.Outlined.Healing
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle("Medication")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityTextField(
                    label = "Treatment",
                    value = treatmentState,
                    onValueChange = {
                        treatmentState = it
                        onTreatmentChange(it)
                    },
                    icon = Icons.Outlined.Medication
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Batch No.",
                    value = batchState,
                    onValueChange = {
                        batchState = it
                        onBatchNumberChange(it)
                    },
                    icon = Icons.Outlined.Numbers
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Volume Used",
                    value = volumeState,
                    onValueChange = {
                        volumeState = it
                        onVolumeUsedChange(it)
                    },
                    icon = Icons.Outlined.Science
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Cost",
                    value = costState,
                    onValueChange = {
                        costState = it
                        onCostChange(it)
                    },
                    icon = Icons.Outlined.Payments
                )

                Spacer(modifier = Modifier.height(18.dp))

                TraceabilitySecondaryButton(
                    text = "Add Another Treatment",
                    icon = Icons.Outlined.Add,
                    onClick = onAddTreatmentClick
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            TraceabilityPrimaryButton(
                text = "Save Treatment Record",
                icon = Icons.Outlined.Save,
                onClick = onSaveClick
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TreatmentsScreenPreview() {
    TreatmentsScreen()
}