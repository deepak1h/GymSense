package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
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

val GymSenseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GymSenseDarkColorScheme,
        typography = Typography,
        shapes = GymSenseShapes,
        content = content
    )
}
