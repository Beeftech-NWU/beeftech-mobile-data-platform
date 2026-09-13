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
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Save
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
import com.beeftech.database.entity.AnimalMovement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnimalMovementScreen(
    animalReference: String = "",
    movementInformation: String = "",
    responsibleWorker: String = "",
    movementRecords: List<AnimalMovement> = emptyList(),
    onBackClick: () -> Unit = {},
    onMovementInformationChange: (String) -> Unit = {},
    onResponsibleWorkerChange: (String) -> Unit = {},
    onSaveClick: (String, String) -> Unit = { _, _ -> }
) {

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
            eyebrow = if (animalReference.isBlank()) {
                "FARM TRACEABILITY"
            } else {
                "ANIMAL $animalReference"
            },
            title = "Animal Movement",
            subtitle = "Capture livestock movement information",
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

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TraceabilityCard {

                TraceabilityTextField(
                    label = "Movement Information",
                    value = movementState,
                    onValueChange = {
                        movementState = it
                        onMovementInformationChange(it)
                    },
                    icon = Icons.Outlined.EditNote,
                    singleLine = false,
                    minLines = 3
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

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

            Spacer(
                modifier = Modifier.height(26.dp)
            )

            TraceabilityPrimaryButton(
                text = "Save Movement Record",
                icon = Icons.Outlined.Save,
                onClick = {

                    onSaveClick(
                        movementState,
                        workerState
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            TraceabilitySectionTitle(
                "Movement History"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (movementRecords.isEmpty()) {

                TraceabilityCard {

                    Text(
                        text = "No movement records found.",
                        color = BeeftechMutedText
                    )
                }

            } else {

                movementRecords.forEach { movement ->

                    TraceabilityCard {

                        Text(
                            text = movement.movementType,
                            fontWeight = FontWeight.SemiBold,
                            color = BeeftechText
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Responsible worker: " +
                                        movement.responsibleWorker,
                            color = BeeftechMutedText
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        val formattedDate =
                            SimpleDateFormat(
                                "dd MMM yyyy HH:mm",
                                Locale.getDefault()
                            ).format(
                                Date(movement.timestamp)
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
private fun AnimalMovementScreenPreview() {

    AnimalMovementScreen()
}