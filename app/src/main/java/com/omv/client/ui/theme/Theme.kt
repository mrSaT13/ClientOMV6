package com.omv.client.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun OmvClientTheme(
    darkTheme: Int = 0,
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val isDark = when (darkTheme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val accent = accentColors.getOrElse(accentIndex) { accentColors[0] }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accent.primary,
            onPrimary = Color.Black,
            primaryContainer = accent.primaryDark,
            onPrimaryContainer = Color.White,
            secondary = Teal200,
            onSecondary = Color.Black,
            secondaryContainer = Teal700,
            onSecondaryContainer = Color.White,
            tertiary = Purple500,
            background = DarkBackground,
            onBackground = Grey200,
            surface = DarkSurface,
            onSurface = Grey200,
            surfaceVariant = Grey800,
            onSurfaceVariant = Grey300,
            error = Red500,
            onError = Color.Black
        )
    } else {
        lightColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            primaryContainer = accent.primaryDark,
            onPrimaryContainer = Color.White,
            secondary = Teal200,
            onSecondary = Color.Black,
            secondaryContainer = Teal700,
            onSecondaryContainer = Color.White,
            tertiary = Purple500,
            background = Grey50,
            onBackground = Grey900,
            surface = Color.White,
            onSurface = Grey900,
            surfaceVariant = Grey200,
            onSurfaceVariant = Grey600,
            error = Red500,
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
