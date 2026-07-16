package com.omv.client.ui.theme

import androidx.compose.ui.graphics.Color

val Blue500 = Color(0xFF2196F3)
val Blue700 = Color(0xFF1976D2)
val Blue900 = Color(0xFF0D47A1)
val Teal200 = Color(0xFF03DAC5)
val Teal700 = Color(0xFF018786)
val Purple500 = Color(0xFF9C27B0)
val Purple900 = Color(0xFF4A148C)
val Orange500 = Color(0xFFFF9800)
val Red500 = Color(0xFFF44336)
val Green500 = Color(0xFF4CAF50)
val Grey50 = Color(0xFFFAFAFA)
val Grey100 = Color(0xFFF5F5F5)
val Grey200 = Color(0xFFEEEEEE)
val Grey300 = Color(0xFFE0E0E0)
val Grey600 = Color(0xFF757575)
val Grey800 = Color(0xFF424242)
val Grey900 = Color(0xFF212121)
val DarkSurface = Color(0xFF121212)
val DarkBackground = Color(0xFF0E0E0E)

// Accent color options
val AccentBlue = Color(0xFF2196F3)
val AccentIndigo = Color(0xFF3F51B5)
val AccentPurple = Color(0xFF9C27B0)
val AccentTeal = Color(0xFF009688)
val AccentGreen = Color(0xFF4CAF50)
val AccentOrange = Color(0xFFFF9800)
val AccentRed = Color(0xFFF44336)
val AccentPink = Color(0xFFE91E63)

data class AccentColor(
    val name: String,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color
)

val accentColors = listOf(
    AccentColor("Blue", AccentBlue, Color(0xFF1976D2), Color(0xFFBBDEFB)),
    AccentColor("Indigo", AccentIndigo, Color(0xFF303F9F), Color(0xFFC5CAE9)),
    AccentColor("Purple", AccentPurple, Color(0xFF7B1FA2), Color(0xFFE1BEE7)),
    AccentColor("Teal", AccentTeal, Color(0xFF00796B), Color(0xFFB2DFDB)),
    AccentColor("Green", AccentGreen, Color(0xFF388E3C), Color(0xFFC8E6C9)),
    AccentColor("Orange", AccentOrange, Color(0xFFF57C00), Color(0xFFFFE0B2)),
    AccentColor("Red", AccentRed, Color(0xFFD32F2F), Color(0xFFFFCDD2)),
    AccentColor("Pink", AccentPink, Color(0xFFC2185B), Color(0xFFF8BBD0))
)
