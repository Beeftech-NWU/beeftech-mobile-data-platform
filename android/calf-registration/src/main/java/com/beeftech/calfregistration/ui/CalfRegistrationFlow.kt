package com.beeftech.calfregistration.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel

private enum class CalfFlowStep {
    HOME,
    TAG_IDENTITY,
    APPEARANCE_PARENTAGE,
    REGISTERED_LIST,
    CALF_DETAIL
}

@Composable
fun CalfRegistrationFlow(
    viewModel: CalfRegistrationViewModel
) {
    var currentStep by remember {
        mutableStateOf(CalfFlowStep.HOME)
    }

    val navigationHistory = remember {
        mutableStateListOf<CalfFlowStep>()
    }

    var selectedCalf by remember {
        mutableStateOf<CalfRegistrationData?>(null)
    }

    var confirmationSuccess by remember {
        mutableStateOf(false)
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
            transponderNumber =
                "${(41..99).random()}"
        )
    }

    fun navigateTo(step: CalfFlowStep) {
        navigationHistory.add(currentStep)
        currentStep = step
    }

    fun navigateBack() {
        currentStep =
            if (navigationHistory.isNotEmpty()) {
                navigationHistory.removeAt(navigationHistory.lastIndex)
            } else {
                CalfFlowStep.HOME
            }
    }

    BackHandler(enabled = currentStep != CalfFlowStep.HOME) {
        navigateBack()
    }

    // loadAll() is a one-shot read, so refresh when the list-bearing screens open.
    LaunchedEffect(currentStep) {
        if (
            currentStep == CalfFlowStep.HOME ||
            currentStep == CalfFlowStep.REGISTERED_LIST
        ) {
            viewModel.loadCalves()
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

                        // On failure keep the form so the user can fix it.
                        if (confirmationSuccess) {
                            formData = createNextCalfForm()
                            navigationHistory.clear()
                            navigationHistory.add(CalfFlowStep.HOME)
                            currentStep = CalfFlowStep.REGISTERED_LIST
                        }
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    when (currentStep) {

        CalfFlowStep.HOME -> {
            CalfRegistrationHomeScreen(
                registeredCount = registeredCalves.size,
                onRegisterCalfClick = {
                    formData = createNextCalfForm()
                    navigateTo(CalfFlowStep.TAG_IDENTITY)
                },
                onViewRegisteredClick = {
                    navigateTo(CalfFlowStep.REGISTERED_LIST)
                }
            )
        }

        CalfFlowStep.TAG_IDENTITY -> {
            TagIdentityScreen(
                formData = formData,
                onFormDataChange = { updated -> formData = updated },
                onCheckTagDuplicate = { tag -> viewModel.isTagRegistered(tag) },
                onNextClick = { navigateTo(CalfFlowStep.APPEARANCE_PARENTAGE) },
                onBackClick = { navigateBack() }
            )
        }

        CalfFlowStep.APPEARANCE_PARENTAGE -> {
            AppearanceParentageScreen(
                formData = formData,
                onFormDataChange = { updated -> formData = updated },
                onBackClick = { navigateBack() },
                onDiscardClick = {
                    formData = CalfRegistrationData()
                    navigationHistory.clear()
                    currentStep = CalfFlowStep.HOME
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

                        confirmationSuccess = success

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

        CalfFlowStep.REGISTERED_LIST -> {
            CalvesRegisteredScreen(
                registeredCalves = registeredCalves,
                onSelectCalf = { selected ->
                    selectedCalf = selected
                    navigateTo(CalfFlowStep.CALF_DETAIL)
                },
                onRegisterNewCalfClick = {
                    formData = createNextCalfForm()
                    navigateTo(CalfFlowStep.TAG_IDENTITY)
                },
                onBackClick = { navigateBack() }
            )
        }

        CalfFlowStep.CALF_DETAIL -> {
            val calf = selectedCalf
            if (calf == null) {
                LaunchedEffect(Unit) { navigateBack() }
            } else {
                CalfDetailScreen(
                    calf = calf,
                    onBackClick = { navigateBack() }
                )
            }
        }
    }
}
