package com.aibuilt.blackvault.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VaultScheme = darkColorScheme(
    primary = VaultGreen,
    onPrimary = VaultBlack,
    primaryContainer = VaultGreenDim,
    onPrimaryContainer = VaultGreen,
    secondary = VaultGreen,
    onSecondary = VaultBlack,
    error = VaultRed,
    onError = VaultBlack,
    background = VaultBlack,
    onBackground = VaultText,
    surface = VaultCard,
    onSurface = VaultText,
    surfaceVariant = VaultCardAlt,
    onSurfaceVariant = VaultTextDim,
    outline = VaultGrey,
    tertiary = VaultAmber
)

@Composable
fun BlackVaultTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    @Suppress("UNUSED_EXPRESSION")
    LocalContext.current
    MaterialTheme(
        colorScheme = VaultScheme,
        typography = VaultTypography,
        content = content
    )
}
