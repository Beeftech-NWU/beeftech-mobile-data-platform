package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BeeftechPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = BeeftechPrimaryDark,
    onPrimaryContainer = BeeftechPrimaryContainer,
    secondary = BeeftechSecondary,
    onSecondary = Color.Black,
    secondaryContainer = BeeftechSecondaryContainer,
    onSecondaryContainer = BeeftechOnSecondaryContainer,
    tertiary = BeeftechTertiary,
    onTertiary = Color.White,
    tertiaryContainer = BeeftechTertiaryContainer,
    onTertiaryContainer = BeeftechOnTertiaryContainer,
    background = BeeftechBackgroundDark,
    onBackground = BeeftechOnSurfaceDark,
    surface = BeeftechSurfaceDark,
    onSurface = BeeftechOnSurfaceDark,
    surfaceVariant = BeeftechSurfaceVariantDark,
    onSurfaceVariant = BeeftechOnSurfaceVariantDark,
    outline = BeeftechOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = BeeftechPrimary,
    onPrimary = Color.White,
    primaryContainer = BeeftechPrimaryContainer,
    onPrimaryContainer = BeeftechOnPrimaryContainer,
    secondary = BeeftechSecondary,
    onSecondary = Color.White,
    secondaryContainer = BeeftechSecondaryContainer,
    onSecondaryContainer = BeeftechOnSecondaryContainer,
    tertiary = BeeftechTertiary,
    onTertiary = Color.White,
    tertiaryContainer = BeeftechTertiaryContainer,
    onTertiaryContainer = BeeftechOnTertiaryContainer,
    background = BeeftechBackgroundLight,
    onBackground = BeeftechOnSurfaceLight,
    surface = BeeftechSurfaceLight,
    onSurface = BeeftechOnSurfaceLight,
    surfaceVariant = BeeftechSurfaceVariantLight,
    onSurfaceVariant = BeeftechOnSurfaceVariantLight,
    outline = BeeftechOutlineLight,
    outlineVariant = BeeftechOutlineVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
