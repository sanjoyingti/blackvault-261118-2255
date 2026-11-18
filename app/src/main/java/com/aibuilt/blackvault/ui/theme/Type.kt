package com.aibuilt.blackvault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Mono = FontFamily.Monospace

val VaultTypography = Typography(
    displayLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = 2.sp),
    displayMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 1.5.sp),
    headlineLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 21.sp),
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = Mono, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = Mono, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = Mono, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp)
)
