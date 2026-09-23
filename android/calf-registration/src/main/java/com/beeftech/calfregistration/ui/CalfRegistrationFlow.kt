package com.beeftech.calfregistration.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.beeftech.calfregistration.util.TagColour
import com.beeftech.calfregistration.util.TagNamingUtils
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel

private enum class CalfFlowStep {
    TAG_IDENTITY,
    APPEARANCE_PARENTAGE,
    SESSION_LIST
}

@Composable
fun CalfRegistrationFlow(
    viewModel: CalfRegistrationViewModel
) {
    var currentStep by remember {
        mutableStateOf(CalfFlowStep.TAG_IDENTITY)
    }

    var formData by remember {
        mutableStateOf(CalfRegistrationData())
    }

    val registeredCalves by
        viewModel.registeredCalves.collectAsState()

    // Confirmation dialog state
    var showConfirmationDialog by remember {
        mutableStateOf(false)
    }

    var confirmationTitle by remember {
        mutableStateOf("")
    }

    var confirmationMessage by remember {
        mutableStateOf("")
    }

    fun createNextCalfForm(): CalfRegistrationData {
        return CalfRegistrationData(
            tagNumber =
                TagNamingUtils.formatTag(
                    TagColour.BLUE,
                    (64..99).random().toLong()
                ),
            transponderNumber =
                "${(41..99).random()}"
        )
    }

    fun goBack() {
        currentStep =
            when (currentStep) {
                CalfFlowStep.APPEARANCE_PARENTAGE ->
                    CalfFlowStep.TAG_IDENTITY

                CalfFlowStep.SESSION_LIST ->
                    CalfFlowStep.TAG_IDENTITY

                else ->
                    CalfFlowStep.TAG_IDENTITY
            }
    }

    if (
        currentStep !=
        CalfFlowStep.TAG_IDENTITY
    ) {
        BackHandler {
            goBack()
        }
    }

    /*
     * Confirmation shown after the save operation finishes.
     *
     * The ViewModel tells us whether the operation completed
     * successfully and supplies the appropriate online/offline message.
     */
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                // Require acknowledgement using the OK button.
            },

            title = {
                Text(
                    text = confirmationTitle
                )
            },

            text = {
                Text(
                    text = confirmationMessage
                )
            },

            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog =
                            false

                        // Reset for the next calf.
                        formData =
                            createNextCalfForm()

                        // Navigate only after confirmation.
                        currentStep =
                            CalfFlowStep.SESSION_LIST
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    when (currentStep) {

        CalfFlowStep.TAG_IDENTITY -> {

            TagIdentityScreen(
                formData = formData,

                onFormDataChange = { updated ->
                    formData = updated
                },

                onCheckTagDuplicate = { tag ->
                    viewModel.isTagRegistered(tag)
                },

                onNextClick = {
                    currentStep =
                        CalfFlowStep.APPEARANCE_PARENTAGE
                }
            )
        }

        CalfFlowStep.APPEARANCE_PARENTAGE -> {

            AppearanceParentageScreen(
                formData = formData,

                onFormDataChange = { updated ->
                    formData = updated
                },

                onBackClick = {
                    currentStep =
                        CalfFlowStep.TAG_IDENTITY
                },

                onDiscardClick = {
                    formData =
                        CalfRegistrationData()

                    currentStep =
                        CalfFlowStep.TAG_IDENTITY
                },

                onSaveAndNextClick = {

                    /*
                     * Keep the current tag number because the form
                     * is reset only after the user presses OK.
                     */
                    val savedTagNumber =
                        formData.tagNumber

                    viewModel.saveCalf(
                        formData
                    ) { success, message ->

                        if (success) {

                            confirmationTitle =
                                "Calf Registered Successfully"

                            confirmationMessage =
                                if (
                                    message.contains(
                                        "synced successfully",
                                        ignoreCase = true
                                    )
                                ) {
                                    "Calf $savedTagNumber has been saved " +
                                        "and synced successfully."
                                } else {
                                    "Calf $savedTagNumber has been saved " +
                                        "successfully on this device.\n\n" +
                                        "It will sync automatically when " +
                                        "internet is available."
                                }

                            showConfirmationDialog =
                                true

                        } else {

                            confirmationTitle =
                                "Unable to Register Calf"

                            confirmationMessage =
                                message.ifBlank {
                                    "The calf registration could not be saved."
                                }

                            showConfirmationDialog =
                                true
                        }
                    }
                }
            )
        }

        CalfFlowStep.SESSION_LIST -> {

            CalvesRegisteredScreen(
                registeredCalves =
                    registeredCalves,

                onSelectCalf = { selected ->
                    formData = selected

                    currentStep =
                        CalfFlowStep.TAG_IDENTITY
                },

                onRegisterNewCalfClick = {
                    formData =
                        createNextCalfForm()

                    currentStep =
                        CalfFlowStep.TAG_IDENTITY
                },

                onBackClick = {
                    currentStep =
                        CalfFlowStep.TAG_IDENTITY
                }
            )
        }
    }
}