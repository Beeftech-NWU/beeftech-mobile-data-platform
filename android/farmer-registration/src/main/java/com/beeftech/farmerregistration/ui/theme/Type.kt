package com.beeftech.farmerregistration.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography =
    Typography(
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    )

val SunlightTypography =
    Typography(
        headlineLarge = Typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 42.sp),
        headlineMedium = Typography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 38.sp),
        headlineSmall = Typography.headlineSmall.copy(fontSize = 27.sp, lineHeight = 34.sp),
        titleLarge = Typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp),
        titleMedium = Typography.titleMedium.copy(fontSize = 20.sp, lineHeight = 27.sp),
        titleSmall = Typography.titleSmall.copy(fontSize = 18.sp, lineHeight = 24.sp),
        bodyLarge = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 27.sp),
        bodyMedium = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 25.sp),
        bodySmall = Typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
        labelLarge = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
        labelMedium = Typography.labelMedium.copy(fontSize = 14.sp),
        labelSmall = Typography.labelSmall.copy(fontSize = 13.sp)
    )
