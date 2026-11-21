package com.ferrovelho.game

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Visual moderno: grafite profundo, violeta elétrico + verde-água, cantos bem arredondados. */
private val GameColors = darkColorScheme(
    primary = Color(0xFF9A8CFF), onPrimary = Color(0xFF15103F),
    primaryContainer = Color(0xFF2E2870), onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFF3DDBC0), onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF173F3A), onSecondaryContainer = Color(0xFFBFF5EA),
    tertiary = Color(0xFFFFB86B), onTertiary = Color(0xFF442700),
    error = Color(0xFFFF6B7A),
    background = Color(0xFF0D0E13), onBackground = Color(0xFFE8E9F0),
    surface = Color(0xFF13151C), onSurface = Color(0xFFE8E9F0),
    surfaceVariant = Color(0xFF232733), onSurfaceVariant = Color(0xFFB4B8CC),
    surfaceContainerLowest = Color(0xFF0A0B0F), surfaceContainerLow = Color(0xFF161820),
    surfaceContainer = Color(0xFF1B1E27), surfaceContainerHigh = Color(0xFF222633), surfaceContainerHighest = Color(0xFF2A2F3E),
    outline = Color(0xFF4B516A), outlineVariant = Color(0xFF2F3447),
)

private val GameShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

private val Sans = FontFamily.SansSerif
private val GameType = Typography(
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun GameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GameColors, shapes = GameShapes, typography = GameType, content = content)
}
