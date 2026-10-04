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

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = MedicalCyan,
    error = AlertRed,
    errorContainer = AlertRedContainer,
    onErrorContainer = OnAlertRedContainer,
    background = BackgroundLight,
    onBackground = PolishTextPrimary,
    surface = SurfaceLight,
    onSurface = PolishTextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = PolishTextSecondary,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF63D3FF),
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF004D64),
    onPrimaryContainer = Color(0xFFBEE9FF),
    secondary = Color(0xFFB4CAD6),
    onSecondary = Color(0xFF1E333D),
    secondaryContainer = Color(0xFF354A54),
    onSecondaryContainer = Color(0xFFD0E6F2),
    tertiary = Color(0xFF4DD9EC),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0F1416),
    surface = Color(0xFF0F1416),
    surfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF8A9296)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand healthcare colors by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
