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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
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
import com.beeftech.database.entity.Mortality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MortalityScreen(
    animalReference: String = "",
    mortalityReason: String = "",
    responsibleWorker: String = "",
    workerOptions: List<String> = emptyList(),
    mortalityCount: Int? = null,
    foundAnimalReference: String = "",
    foundMortalityReason: String = "",
    foundMortalityDate: String = "",
    foundResponsibleWorker: String = "",
    mortalityRecords: List<Mortality> = emptyList(),
    onBackClick: () -> Unit = {},
    onAnimalReferenceChange: (String) -> Unit = {},
    onMortalityReasonChange: (String) -> Unit = {},
    onResponsibleWorkerChange: (String) -> Unit = {},
    onAddMortalityClick: (
        animalReference: String,
        mortalityReason: String,
        responsibleWorker: String
    ) -> Unit = { _, _, _ -> },
    onSaveClick: (
        mortalityReason: String,
        responsibleWorker: String
    ) -> Unit = { _, _ -> }
) {
    var animalReferenceState by remember(animalReference) {
        mutableStateOf(animalReference)
    }

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
            title = "Mortality Records",
            subtitle = "Capture and review livestock mortality records",
            icon = Icons.AutoMirrored.Outlined.Assignment,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            TraceabilitySectionTitle(
                title = "Mortality Details"
            )

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
                text = "Add Mortality Record",
                icon = Icons.Outlined.Add,
                onClick = {
                    val reference = animalReferenceState.trim()
                    val reason = reasonState.trim()
                    val worker = workerState.trim()

                    if (
                        reference.isNotBlank() &&
                        reason.isNotBlank() &&
                        worker.isNotBlank()
                    ) {
                        onAddMortalityClick(
                            reference,
                            reason,
                            worker
                        )

                        reasonState = ""
                        workerState = ""

                        onMortalityReasonChange("")
                        onResponsibleWorkerChange("")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityPrimaryButton(
                text = "Save Mortality Records",
                icon = Icons.Outlined.Save,
                onClick = {
                    onSaveClick(
                        reasonState,
                        workerState
                    )
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            TraceabilitySectionTitle(
                "Mortality History"
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (mortalityRecords.isEmpty()) {
                TraceabilityCard {
                    Text(
                        text = "No mortality records found.",
                        color = BeeftechMutedText
                    )
                }
            } else {
                mortalityRecords.forEach { record ->
                    TraceabilityCard {
                        Text(
                            text = record.causeOfDeath,
                            fontWeight = FontWeight.SemiBold,
                            color = BeeftechText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Responsible worker: ${record.responsibleWorker}",
                            color = BeeftechMutedText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraceabilitySectionTitle(
                title = "Mortality Overview"
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraceabilityCard {
                TraceabilityInfoRow(
                    icon = Icons.Outlined.Numbers,
                    title = "Number of Mortalities",
                    subtitle = mortalityCount?.toString()
                        ?: mortalityRecords.size.toString()
                )
            }

            if (foundAnimalReference.isNotBlank()) {
                Spacer(modifier = Modifier.height(26.dp))

                TraceabilitySectionTitle(
                    title = "Mortality Record"
                )

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
                        title = "Mortality Reason",
                        subtitle = foundMortalityReason.ifBlank {
                            "Mortality reason unavailable"
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TraceabilityInfoRow(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Mortality Date",
                        subtitle = foundMortalityDate.ifBlank {
                            "Mortality date unavailable"
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
private fun MortalityScreenPreview() {
    MortalityScreen()
}