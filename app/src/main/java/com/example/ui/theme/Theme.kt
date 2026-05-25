package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IndustrialDarkColorScheme = darkColorScheme(
    primary = IndustrialAmber,
    secondary = SyncTeal,
    tertiary = ContactlessGreen,
    background = TerminalBlack,
    surface = TerminalDarkGray,
    onPrimary = TerminalBlack,
    onSecondary = TerminalBlack,
    onTertiary = TerminalBlack,
    onBackground = TerminalOffWhite,
    onSurface = TerminalOffWhite,
    error = RefundRed
)

// The terminal prioritizes its signature High-Contrast Dark Theme for clarity at all hours.
private val IndustrialLightColorScheme = lightColorScheme(
    primary = CashierOrange,
    secondary = SyncTeal,
    tertiary = ContactlessGreen,
    background = Color(0xFFFAF9F6),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = TerminalBlack,
    onTertiary = Color.White,
    onBackground = TerminalBlack,
    onSurface = TerminalBlack,
    error = RefundRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark-Mode terminal as the primary brand feel, as requested!
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our robust industrial terminal branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IndustrialDarkColorScheme else IndustrialLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
