package com.beeftech.database.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SunlightUiState {

    private val _isSunlightMode =
        MutableStateFlow(false)

    val isSunlightMode:
        StateFlow<Boolean> =
        _isSunlightMode.asStateFlow()

    fun setSunlightMode(
        enabled: Boolean
    ) {

        _isSunlightMode.value =
            enabled
    }
}