package com.beeftech.calfregistration.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

private enum class CalfFlowStep {
    TAG_IDENTITY,
    APPEARANCE_PARENTAGE,
    SESSION_LIST
}

@Composable
fun CalfRegistrationFlow() {
    var currentStep by remember { mutableStateOf(CalfFlowStep.TAG_IDENTITY) }
    var formData by remember { mutableStateOf(CalfRegistrationData()) }
    var registeredCalves by remember {
        mutableStateOf(CalfRegistrationLookups.initialRegisteredCalves)
    }

    fun goBack() {
        currentStep = when (currentStep) {
            CalfFlowStep.APPEARANCE_PARENTAGE -> CalfFlowStep.TAG_IDENTITY
            CalfFlowStep.SESSION_LIST -> CalfFlowStep.TAG_IDENTITY
            else -> CalfFlowStep.TAG_IDENTITY
        }
    }

    if (currentStep != CalfFlowStep.TAG_IDENTITY) {
        BackHandler {
            goBack()
        }
    }

    when (currentStep) {
        CalfFlowStep.TAG_IDENTITY -> {
            TagIdentityScreen(
                formData = formData,
                onFormDataChange = { updated -> formData = updated },
                onNextClick = {
                    currentStep = CalfFlowStep.APPEARANCE_PARENTAGE
                }
            )
        }

        CalfFlowStep.APPEARANCE_PARENTAGE -> {
            AppearanceParentageScreen(
                formData = formData,
                onFormDataChange = { updated -> formData = updated },
                onBackClick = {
                    currentStep = CalfFlowStep.TAG_IDENTITY
                },
                onDiscardClick = {
                    formData = CalfRegistrationData()
                    currentStep = CalfFlowStep.TAG_IDENTITY
                },
                onSaveAndNextClick = {
                    // Save calf to session list
                    val newCalf = formData.copy(synced = false)
                    registeredCalves = listOf(newCalf) + registeredCalves

                    // Reset form and navigate to session list
                    formData = CalfRegistrationData(
                        tagNumber = "RMB${(25426..25499).random()}",
                        transponderNumber = "${(41..99).random()}"
                    )
                    currentStep = CalfFlowStep.SESSION_LIST
                }
            )
        }

        CalfFlowStep.SESSION_LIST -> {
            CalvesRegisteredScreen(
                registeredCalves = registeredCalves,
                onSelectCalf = { selected ->
                    formData = selected
                    currentStep = CalfFlowStep.TAG_IDENTITY
                },
                onRegisterNewCalfClick = {
                    formData = CalfRegistrationData(
                        tagNumber = "RMB${(25426..25499).random()}",
                        transponderNumber = "${(41..99).random()}"
                    )
                    currentStep = CalfFlowStep.TAG_IDENTITY
                },
                onBackClick = {
                    currentStep = CalfFlowStep.TAG_IDENTITY
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalfRegistrationFlowPreview() {
    CalfRegistrationFlow()
}
