package com.purelilith.networkscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Sea,
    onPrimary = Color.White,
    primaryContainer = SeaSoft,
    onPrimaryContainer = SeaDeep,
    secondary = Color(0xFF4B6A74),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEBEF),
    onSecondaryContainer = SeaDeep,
    tertiary = Color(0xFF8A6A12),
    onTertiary = Color.White,
    tertiaryContainer = Sand,
    onTertiaryContainer = SandDeep,
    error = Rust,
    onError = Color.White,
    errorContainer = RustSoft,
    onErrorContainer = RustDeep,
    background = Mist,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE3EAEE),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFF8A99A3),
    outlineVariant = Line,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = Color(0xFFEDF2F5),
    surfaceContainerHigh = Color(0xFFE6ECF0),
    surfaceContainerHighest = Color(0xFFDFE7EC)
)

private val DarkColorScheme = darkColorScheme(
    primary = SeaLight,
    onPrimary = Color(0xFF00363E),
    primaryContainer = SeaNight,
    onPrimaryContainer = SeaPale,
    secondary = Color(0xFFA9C3CC),
    onSecondary = Color(0xFF1A333B),
    secondaryContainer = Color(0xFF1B3A44),
    onSecondaryContainer = Color(0xFFD2E8EE),
    tertiary = SandLight,
    onTertiary = Color(0xFF3A2D00),
    tertiaryContainer = SandNight,
    onTertiaryContainer = SandPale,
    error = RustLight,
    onError = Color(0xFF4A1208),
    errorContainer = RustNight,
    onErrorContainer = RustPale,
    background = Night,
    onBackground = Snow,
    surface = Night,
    onSurface = Snow,
    surfaceVariant = Color(0xFF22323B),
    onSurfaceVariant = SnowSoft,
    outline = Color(0xFF62767F),
    outlineVariant = LineDark,
    surfaceContainerLowest = Color(0xFF0A1216),
    surfaceContainerLow = Panel,
    surfaceContainer = Color(0xFF1B2B34),
    surfaceContainerHigh = Color(0xFF21333D),
    surfaceContainerHighest = Color(0xFF283C47)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun appBackgroundBrush(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    val top = if (darkTheme) NightTop else MistTop
    val base = if (darkTheme) Night else Mist
    return Brush.verticalGradient(
        0f to top,
        0.5f to base,
        1f to base
    )
}

@Composable
fun NetworkScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}