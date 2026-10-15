package com.beeftech.demoapp

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * The user's "Bright screen" choice from the More screen. When on, BeefTech holds its
 * window at BRIGHT_SCREEN_LEVEL; when off, the phone's own brightness setting applies.
 * Sunlight mode (SunlightBrightnessController) still boosts to full in direct sun either way.
 * Stored per phone in plain SharedPreferences: it is a display preference, not farm data.
 */
object BrightScreenSetting {

    const val BRIGHT_SCREEN_LEVEL = 0.90f

    private const val PREFS = "beeftech_display"
    private const val KEY_BRIGHT_SCREEN = "bright_screen"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun load(context: Context) {
        _enabled.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BRIGHT_SCREEN, false)
    }

    fun set(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BRIGHT_SCREEN, enabled)
            .apply()
        _enabled.value = enabled
    }
}
