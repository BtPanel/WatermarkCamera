package com.watermarkcamera.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFF116B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EFE8),
    onPrimaryContainer = Color(0xFF063A31),
    secondary = Color(0xFFB4573D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF48170B),
    tertiary = Color(0xFF786000),
    tertiaryContainer = Color(0xFFFFE58A),
    background = Color(0xFFF4F6F5),
    onBackground = Color(0xFF171C1A),
    surface = Color(0xFFFBFCFB),
    onSurface = Color(0xFF171C1A),
    surfaceVariant = Color(0xFFE2E8E5),
    onSurfaceVariant = Color(0xFF424946),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3F1),
    surfaceContainer = Color(0xFFEAEFEC),
    surfaceContainerHigh = Color(0xFFE4E9E6),
    surfaceContainerHighest = Color(0xFFDDE3E0),
    outline = Color(0xFF707975),
    outlineVariant = Color(0xFFC1CAC5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF70D8C0),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF075044),
    onPrimaryContainer = Color(0xFF9EF2DC),
    secondary = Color(0xFFFFB5A2),
    secondaryContainer = Color(0xFF783522),
    tertiary = Color(0xFFEAC54C),
    background = Color(0xFF111513),
    onBackground = Color(0xFFE1E7E3),
    surface = Color(0xFF171B19),
    onSurface = Color(0xFFE1E7E3),
    surfaceVariant = Color(0xFF3F4844),
    onSurfaceVariant = Color(0xFFC0C9C4),
    surfaceContainerLow = Color(0xFF191E1B),
    surfaceContainer = Color(0xFF1E2421),
    surfaceContainerHigh = Color(0xFF282E2B),
    surfaceContainerHighest = Color(0xFF333936),
    outline = Color(0xFF89938E),
    outlineVariant = Color(0xFF3F4844)
)

private val StudioTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun StudioTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
