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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
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
fun MortalityScreen(
    animalReference: String = "",
    mortalityReason: String = "",
    responsibleWorker: String = "",
    onBackClick: () -> Unit = {},
    onMortalityReasonChange: (String) -> Unit = {},
    onResponsibleWorkerChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {}
) {

    var reasonState by remember(mortalityReason) {
        mutableStateOf(mortalityReason)
    }

    var workerState by remember(responsibleWorker) {
        mutableStateOf(responsibleWorker)
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
            title = "Mortality Record",
            subtitle = "Capture livestock mortality information",
            icon = Icons.Outlined.Assignment,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            TraceabilitySectionTitle("Mortality Details")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {

                TraceabilityTextField(
                    label = "Mortality Reason",
                    value = reasonState,
                    onValueChange = {
                        reasonState = it
                        onMortalityReasonChange(it)
                    },
                    icon = Icons.Outlined.EditNote,
                    singleLine = false,
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Responsible Worker",
                    value = workerState,
                    onValueChange = {
                        workerState = it
                        onResponsibleWorkerChange(it)
                    },
                    icon = Icons.Outlined.Person
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            TraceabilityPrimaryButton(
                text = "Save Mortality Record",
                icon = Icons.Outlined.Save,
                onClick = onSaveClick
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MortalityScreenPreview() {
    MortalityScreen()
}