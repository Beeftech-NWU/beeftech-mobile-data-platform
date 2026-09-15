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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Route
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
fun AnimalMovementScreen(
    animalReference: String = "",
    movementInformation: String = "",
    responsibleWorker: String = "",
    workerOptions: List<String> = emptyList(),
    foundAnimalReference: String = "",
    foundMovementInformation: String = "",
    foundMovementDate: String = "",
    foundResponsibleWorker: String = "",
    onBackClick: () -> Unit = {},
    onAnimalReferenceChange: (String) -> Unit = {},
    onMovementInformationChange: (String) -> Unit = {},
    onResponsibleWorkerChange: (String) -> Unit = {},
    onAddMovementClick: (
        animalReference: String,
        movementInformation: String,
        responsibleWorker: String
    ) -> Unit = { _, _, _ -> },
    onSaveClick: () -> Unit = {}
) {
    var animalReferenceState by remember(animalReference) {
        mutableStateOf(animalReference)
    }

    var movementState by remember(movementInformation) {
        mutableStateOf(movementInformation)
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
            eyebrow = "FARM TRACEABILITY",
            title = "Animal Movement",
            subtitle = "Capture and review livestock movement records",
            icon = Icons.Outlined.Route,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            TraceabilitySectionTitle("Movement Details")

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityTextField(
                    label = "Animal Tag / Reference",
                    value = animalReferenceState,
                    onValueChange = {
                        animalReferenceState = it
                        onAnimalReferenceChange(it)
                    },
                    icon = Icons.Outlined.Pets
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilityTextField(
                    label = "Movement Details",
                    value = movementState,
                    onValueChange = {
                        movementState = it
                        onMovementInformationChange(it)
                    },
                    icon = Icons.Outlined.EditNote,
                    singleLine = false,
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                TraceabilitySearchableDropdown(
                    label = "Responsible Worker",
                    value = workerState,
                    options = workerOptions,
                    icon = Icons.Outlined.Person,
                    onValueChange = {
                        workerState = it
                        onResponsibleWorkerChange(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            TraceabilitySecondaryButton(
                text = "Add Movement Record",
                icon = Icons.Outlined.Add,
                onClick = {
                    val reference = animalReferenceState.trim()
                    val movement = movementState.trim()
                    val worker = workerState.trim()

                    if (
                        reference.isNotBlank() &&
                        movement.isNotBlank() &&
                        worker.isNotBlank()
                    ) {
                        onAddMovementClick(
                            reference,
                            movement,
                            worker
                        )

                        movementState = ""
                        workerState = ""

                        onMovementInformationChange("")
                        onResponsibleWorkerChange("")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityPrimaryButton(
                text = "Save Movement Records",
                icon = Icons.Outlined.Save,
                onClick = onSaveClick
            )

            if (foundAnimalReference.isNotBlank()) {
                Spacer(modifier = Modifier.height(30.dp))

                TraceabilitySectionTitle("Movement Record")

                Spacer(modifier = Modifier.height(12.dp))

                TraceabilityCard {
                    TraceabilityInfoRow(
                        icon = Icons.Outlined.Pets,
                        title = "Animal Tag / Reference",
                        subtitle = foundAnimalReference
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TraceabilityInfoRow(
                        icon = Icons.Outlined.EditNote,
                        title = "Movement Details",
                        subtitle = foundMovementInformation.ifBlank {
                            "Movement information unavailable"
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TraceabilityInfoRow(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Movement Date",
                        subtitle = foundMovementDate.ifBlank {
                            "Movement date unavailable"
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TraceabilityInfoRow(
                        icon = Icons.Outlined.Person,
                        title = "Responsible Worker",
                        subtitle = foundResponsibleWorker.ifBlank {
                            "Responsible worker unavailable"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimalMovementScreenPreview() {
    AnimalMovementScreen()
}