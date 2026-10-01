package com.beeftech.demoapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.beeftech.database.runtime.SunlightUiState

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40
    )

/*
 * Outdoor / direct-sunlight color scheme.
 *
 * Dynamic Android colors are deliberately disabled while
 * this mode is active so contrast remains predictable.
 */
private val SunlightColorScheme =
    lightColorScheme(

        primary =
            Color(0xFF004D1A),

        onPrimary =
            Color.White,

        primaryContainer =
            Color(0xFFD7F8DF),

        onPrimaryContainer =
            Color.Black,

        secondary =
            Color.Black,

        onSecondary =
            Color.White,

        secondaryContainer =
            Color.White,

        onSecondaryContainer =
            Color.Black,

        tertiary =
            Color(0xFF664000),

        onTertiary =
            Color.White,

        background =
            Color.White,

        onBackground =
            Color.Black,

        surface =
            Color.White,

        onSurface =
            Color.Black,

        surfaceVariant =
            Color(0xFFF2F2F2),

        onSurfaceVariant =
            Color.Black,

        outline =
            Color.Black,

        error =
            Color(0xFF8B0000),

        onError =
            Color.White
    )

@Composable
fun BeeftechTheme(
    darkTheme: Boolean =
        isSystemInDarkTheme(),
    dynamicColor: Boolean =
        true,
    content: @Composable () -> Unit
) {

    val sunlightMode by
        SunlightUiState
            .isSunlightMode
            .collectAsState()

    val colorScheme =
        when {

            /*
             * Sunlight mode takes priority over system dark mode
             * and Android dynamic colors.
             */
            sunlightMode ->
                SunlightColorScheme

            dynamicColor &&
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S -> {

                val context =
                    LocalContext.current

                if (darkTheme) {
                    dynamicDarkColorScheme(
                        context
                    )
                } else {
                    dynamicLightColorScheme(
                        context
                    )
                }
            }

            darkTheme ->
                DarkColorScheme

            else ->
                LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (sunlightMode) SunlightTypography else Typography,
        content = content
    )
}