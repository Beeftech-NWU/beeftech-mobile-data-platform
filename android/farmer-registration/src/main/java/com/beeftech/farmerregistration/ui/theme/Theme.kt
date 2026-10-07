package com.beeftech.farmerregistration.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beeftech.database.runtime.SunlightUiState

private val BeefTechColorScheme =
    lightColorScheme(
        primary = BeefPrimary,
        onPrimary = Color.White,
        primaryContainer = BeefSoftGreen,
        onPrimaryContainer = BeefPrimaryStrong,
        secondary = BeefAccent,
        onSecondary = Color.White,
        secondaryContainer = BeefSoftSurface,
        onSecondaryContainer = BeefText,
        background = BeefBackground,
        onBackground = BeefText,
        surface = BeefSurface,
        onSurface = BeefText,
        surfaceVariant = BeefSoftSurface,
        onSurfaceVariant = BeefMutedText,
        outline = BeefBorder,
        error = BeefDanger,
        onError = Color.White
    )

private val SunlightColorScheme =
    lightColorScheme(
        primary = Color(0xFF004D1A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7F8DF),
        onPrimaryContainer = Color.Black,
        secondary = Color.Black,
        onSecondary = Color.White,
        secondaryContainer = Color.White,
        onSecondaryContainer = Color.Black,
        tertiary = Color(0xFF664000),
        onTertiary = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFF2F2F2),
        onSurfaceVariant = Color.Black,
        outline = Color.Black,
        error = Color(0xFF8B0000),
        onError = Color.White
    )

private val BeefTechShapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(18.dp)
    )

@Composable
fun BeeftechTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val sunlightMode by SunlightUiState.isSunlightMode.collectAsState()

    MaterialTheme(
        colorScheme = if (sunlightMode) SunlightColorScheme else BeefTechColorScheme,
        typography = if (sunlightMode) SunlightTypography else Typography,
        shapes = BeefTechShapes,
        content = content
    )
}
