package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GymSenseDarkColorScheme = darkColorScheme(
    primary = KineticEmerald,
    onPrimary = Color(0xFF00210E),
    primaryContainer = KineticEmeraldContainer,
    onPrimaryContainer = OnKineticEmeraldContainer,
    secondary = BiometricCyan,
    onSecondary = Color(0xFF001E2F),
    secondaryContainer = BiometricCyanContainer,
    onSecondaryContainer = OnBiometricCyanContainer,
    tertiary = MetabolicAmber,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = MetabolicAmberContainer,
    onTertiaryContainer = OnMetabolicAmberContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryLight,
    surface = ObsidianSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryMuted,
    outline = OutlineSubtle,
    error = AlertCoral,
    onError = Color.White
)

private val GymSenseLightColorScheme = lightColorScheme(
    primary = LightEmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = LightEmeraldContainer,
    onPrimaryContainer = LightOnEmeraldContainer,
    secondary = LightCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightCyanContainer,
    onSecondaryContainer = LightOnCyanContainer,
    tertiary = LightAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = LightAmberContainer,
    onTertiaryContainer = LightOnAmberContainer,
    background = LightAlabasterBackground,
    onBackground = TextPrimaryDark,
    surface = LightPureSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryDarkMuted,
    outline = OutlineLightSubtle,
    error = Color(0xFFB91C1C),
    onError = Color.White
)

val GymSenseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GymSenseDarkColorScheme else GymSenseLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GymSenseShapes,
        content = content
    )
}
