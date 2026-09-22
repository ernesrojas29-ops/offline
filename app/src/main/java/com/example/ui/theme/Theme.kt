package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmoledColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = AmoledBlack,
    primaryContainer = AmberGoldDark,
    onPrimaryContainer = Color.White,
    secondary = IndigoAccent,
    onSecondary = AmoledBlack,
    tertiary = EmeraldGreen,
    background = AmoledBlack,
    onBackground = TextPrimaryAmoled,
    surface = AmoledSurface,
    onSurface = TextPrimaryAmoled,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = TextSecondaryAmoled,
    outline = AmoledBorder,
    error = RoseError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Por defecto habilitamos tema oscuro para ahorro de batería
    amoledMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme || amoledMode -> AmoledColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
