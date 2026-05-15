package com.microshare.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = PrimaryLight,
    secondary = Accent,
    onSecondary = White,
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Divider,
)

val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    bodyLarge = TextStyle(fontSize = 16.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, color = TextSecondary),
    bodySmall = TextStyle(fontSize = 12.sp, color = TextHint),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun MicroShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
