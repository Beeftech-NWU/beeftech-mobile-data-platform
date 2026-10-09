package com.beeftech.calfregistration.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel
import kotlinx.coroutines.launch

private enum class CalfFlowStep {
    HOME,
    STEP_1_TAG,
    STEP_2_DETAILS,
    STEP_3_CONDITION,
    STEP_4_REVIEW,
    REGISTERED_LIST,
    CALF_DETAIL
}

@Composable
fun CalfRegistrationFlow(
    viewModel: CalfRegistrationViewModel,
    onCalfSaved: ((tagNumber: String) -> Unit)? = null,
    onNavigateHome: (() -> Unit)? = null,
    onAssignSavedCalf: ((tagNumber: String) -> Unit)? = null
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

    var formData by remember {
        mutableStateOf(CalfRegistrationData())
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    val registeredCalves by
        viewModel.registeredCalves.collectAsState()

    var savedCalfForAssignment by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun createNextCalfForm(): CalfRegistrationData {
        return CalfRegistrationData()
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

    LaunchedEffect(currentStep) {
        if (
            currentStep == CalfFlowStep.HOME ||
            currentStep == CalfFlowStep.REGISTERED_LIST
        ) {
            viewModel.loadCalves()
        }
    }

    savedCalfForAssignment?.let { tag ->
        AlertDialog(
            onDismissRequest = { savedCalfForAssignment = null },
            title = { Text("Calf saved successfully") },
            text = { Text("Would you like to assign calf $tag to a registered farmer now? You can also do this later in Farm Traceability.") },
            confirmButton = {
                TextButton(onClick = {
                    savedCalfForAssignment = null
                    onAssignSavedCalf?.invoke(tag)
                }) { Text("Assign to farmer") }
            },
            dismissButton = {
                TextButton(onClick = { savedCalfForAssignment = null }) { Text("Not now") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentStep) {
                CalfFlowStep.HOME -> {
                    CalfRegistrationHomeScreen(
                        registeredCount = registeredCalves.size,
                        onRegisterCalfClick = {
                            formData = createNextCalfForm()
                            navigateTo(CalfFlowStep.STEP_1_TAG)
                        },
                        onViewRegisteredClick = {
                            navigateTo(CalfFlowStep.REGISTERED_LIST)
                        }
                    )
                }

                CalfFlowStep.STEP_1_TAG -> {
                    TagIdentityScreen(
                        formData = formData,
                        onFormDataChange = { updated -> formData = updated },
                        onCheckTagDuplicate = { tag -> viewModel.isTagRegistered(tag) },
                        onNextClick = {
                            navigateTo(CalfFlowStep.STEP_2_DETAILS)
                        },
                        onBackClick = { navigateBack() }
                    )
                }

                CalfFlowStep.STEP_2_DETAILS -> {
                    CalfDetailsStepScreen(
                        formData = formData,
                        onFormDataChange = { updated -> formData = updated },
                        onNextClick = {
                            navigateTo(CalfFlowStep.STEP_3_CONDITION)
                        },
                        onBackClick = { navigateBack() }
                    )
                }

                CalfFlowStep.STEP_3_CONDITION -> {
                    CalfConditionStepScreen(
                        formData = formData,
                        onFormDataChange = { updated -> formData = updated },
                        onNextClick = {
                            navigateTo(CalfFlowStep.STEP_4_REVIEW)
                        },
                        onBackClick = { navigateBack() }
                    )
                }

                CalfFlowStep.STEP_4_REVIEW -> {
                    CalfReviewScreen(
                        formData = formData,
                        isSaving = isSaving,
                        onJumpToStep = { stepNumber ->
                            when (stepNumber) {
                                1 -> navigateTo(CalfFlowStep.STEP_1_TAG)
                                2 -> navigateTo(CalfFlowStep.STEP_2_DETAILS)
                                3 -> navigateTo(CalfFlowStep.STEP_3_CONDITION)
                            }
                        },
                        onSaveCalfClick = {
                            val savedTagNumber = formData.tagNumber
                            isSaving = true
                            viewModel.saveCalf(formData) { success, message ->
                                isSaving = false
                                if (success) {
                                    if (onCalfSaved != null) {
                                        formData = createNextCalfForm()
                                        navigationHistory.clear()
                                        currentStep = CalfFlowStep.HOME
                                        onCalfSaved(savedTagNumber)
                                    } else {
                                        formData = createNextCalfForm()
                                        navigationHistory.clear()
                                        currentStep = CalfFlowStep.HOME
                                        if (onAssignSavedCalf != null) {
                                            savedCalfForAssignment = savedTagNumber
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Calf $savedTagNumber saved")
                                            }
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message.ifBlank { "Unable to save calf registration" }
                                        )
                                    }
                                }
                            }
                        },
                        onBackClick = { navigateBack() }
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
                            navigateTo(CalfFlowStep.STEP_1_TAG)
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
    }
}
