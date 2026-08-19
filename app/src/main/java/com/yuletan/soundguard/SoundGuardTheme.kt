package com.yuletan.soundguard

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF111318)
private val Background = Color(0xFFFAFAF8)
private val Surface = Color(0xFFFFFFFF)
private val Secondary = Color(0xFF6B7280)
private val Hairline = Color(0xFFE5E7EB)
private val Danger = Color(0xFFDC2626)

val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3F4F2),
    onSurfaceVariant = Secondary,
    outline = Hairline,
    error = Danger,
    errorContainer = Color(0xFFFBEAEA),
    onErrorContainer = Color(0xFF7F1D1D),
    primaryContainer = Color(0xFFE8F5EE),
    onPrimaryContainer = Color(0xFF14532D),
    secondaryContainer = Color(0xFFFBF0DF),
    onSecondaryContainer = Color(0xFF78350F),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFF5F5F4),
    onPrimary = Ink,
    background = Color(0xFF121212),
    onBackground = Color(0xFFF5F5F4),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF2A2A2A),
    error = Danger,
    errorContainer = Color(0xFF3B1717),
    onErrorContainer = Color(0xFFFECACA),
    primaryContainer = Color(0xFF163B27),
    onPrimaryContainer = Color(0xFFBBF7D0),
    secondaryContainer = Color(0xFF3B2915),
    onSecondaryContainer = Color(0xFFFDE68A),
)

private val SoundGuardTypography = Typography().let {
    it.copy(
        headlineLarge = it.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        bodyLarge = it.bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
        bodyMedium = it.bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
        labelMedium = it.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
    )
}

@Composable
fun SoundGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = SoundGuardTypography,
        content = content,
    )
}

val androidx.compose.material3.ColorScheme.success: Color
    get() = if (this == DarkColors) Color(0xFF4ADE80) else Color(0xFF16A34A)
val androidx.compose.material3.ColorScheme.successTint: Color
    get() = if (this == DarkColors) Color(0xFF122B1C) else Color(0xFFE8F5EE)
val androidx.compose.material3.ColorScheme.onSuccessTint: Color
    get() = if (this == DarkColors) Color(0xFFBBF7D0) else Color(0xFF14532D)
val androidx.compose.material3.ColorScheme.warning: Color
    get() = if (this == DarkColors) Color(0xFFFBBF24) else Color(0xFFD97706)
val androidx.compose.material3.ColorScheme.warningTint: Color
    get() = if (this == DarkColors) Color(0xFF33230D) else Color(0xFFFBF0DF)
val androidx.compose.material3.ColorScheme.onWarningTint: Color
    get() = if (this == DarkColors) Color(0xFFFDE68A) else Color(0xFF92400E)
val androidx.compose.material3.ColorScheme.dangerTint: Color
    get() = if (this == DarkColors) Color(0xFF331414) else Color(0xFFFBEAEA)
val androidx.compose.material3.ColorScheme.danger: Color
    get() = if (this == DarkColors) Color(0xFFF87171) else Color(0xFFDC2626)
val androidx.compose.material3.ColorScheme.onDangerTint: Color
    get() = if (this == DarkColors) Color(0xFFFECACA) else Color(0xFF991B1B)
