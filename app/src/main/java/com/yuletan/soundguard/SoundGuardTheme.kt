package com.yuletan.soundguard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * SoundGuard palette — monochrome-first, with a small, strictly-reserved
 * semantic set for incident severity.
 *
 * Rule of thumb: if a color isn't describing a real incident state
 * (via IncidentStatus below), it should be one of the Ink/Surface tokens.
 * Never use Danger/Warning/Success decoratively, for branding, or on
 * test/demo buttons — that's what makes them mean something when they do show up.
 */

// ---- Neutral scale — ~95% of the UI lives here ----
val Ink900 = Color(0xFF111111)          // primary text, primary button fill
val Ink700 = Color(0xFF404040)          // secondary text
val Ink500 = Color(0xFF767676)          // tertiary text, inactive icons
val Ink300 = Color(0xFFC2C2C2)          // borders, dividers, disabled state
val Ink100 = Color(0xFFE8E8E6)          // hairline dividers
val SurfaceWhite = Color(0xFFFFFFFF)    // cards
val BackgroundGray = Color(0xFFF6F6F4)  // screen background
val SurfaceVariant = Color(0xFFEFEFED)  // chips, subtle fills

// ---- Semantic — reserved for IncidentStatus only, nothing else ----
val DangerRed = Color(0xFFC62828)
val DangerSurface = Color(0xFFFDECEA)
val DangerSurfaceDark = Color(0xFF381414)
val DangerRedDark = Color(0xFFF87171)

val WarningAmber = Color(0xFFA15C00)
val WarningSurface = Color(0xFFFFF4E1)
val WarningSurfaceDark = Color(0xFF3B2915)
val WarningAmberDark = Color(0xFFFBBF24)

val SuccessGreen = Color(0xFF2E7D32)
val SuccessSurface = Color(0xFFEAF5EA)
val SuccessSurfaceDark = Color(0xFF14301D)
val SuccessGreenDark = Color(0xFF4ADE80)

// ---- Dark theme neutrals ----
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkInk900 = Color(0xFFF5F5F4)
val DarkInk700 = Color(0xFFD4D4D4)
val DarkInk500 = Color(0xFF9E9E9E)
val DarkInk300 = Color(0xFF4B4B4B)
val DarkInk100 = Color(0xFF2E2E2E)

val LightColors = lightColorScheme(
    primary = Ink900,
    onPrimary = Color.White,
    background = BackgroundGray,
    onBackground = Ink900,
    surface = SurfaceWhite,
    onSurface = Ink900,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Ink700,
    outline = Ink300,
    outlineVariant = Ink100,
    error = DangerRed,
    errorContainer = DangerSurface,
    onErrorContainer = DangerRed,
    primaryContainer = SurfaceVariant,
    onPrimaryContainer = Ink900,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = Ink700,
)

val DarkColors = darkColorScheme(
    primary = DarkInk900,
    onPrimary = Color(0xFF111111),
    background = DarkBackground,
    onBackground = DarkInk900,
    surface = DarkSurface,
    onSurface = DarkInk900,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInk700,
    outline = DarkInk300,
    outlineVariant = DarkInk100,
    error = DangerRedDark,
    errorContainer = DangerSurfaceDark,
    onErrorContainer = DangerRedDark,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkInk900,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkInk700,
)

private val SoundGuardTypography = Typography().let {
    it.copy(
        headlineLarge = it.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, fontWeight = FontWeight.Bold),
        headlineMedium = it.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, fontWeight = FontWeight.Bold),
        headlineSmall = it.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        titleMedium = it.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = it.bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
        bodyMedium = it.bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
        bodySmall = it.bodySmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 12.sp),
        labelLarge = it.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        labelMedium = it.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelSmall = it.labelSmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
    )
}

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun SoundGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = SoundGuardTypography,
            content = content,
        )
    }
}

val ColorScheme.success: Color
    get() = if (this == DarkColors) SuccessGreenDark else SuccessGreen

val ColorScheme.successTint: Color
    get() = if (this == DarkColors) SuccessSurfaceDark else SuccessSurface

val ColorScheme.onSuccessTint: Color
    get() = if (this == DarkColors) SuccessGreenDark else SuccessGreen

val ColorScheme.warning: Color
    get() = if (this == DarkColors) WarningAmberDark else WarningAmber

val ColorScheme.warningTint: Color
    get() = if (this == DarkColors) WarningSurfaceDark else WarningSurface

val ColorScheme.onWarningTint: Color
    get() = if (this == DarkColors) WarningAmberDark else WarningAmber

val ColorScheme.danger: Color
    get() = if (this == DarkColors) DangerRedDark else DangerRed

val ColorScheme.dangerTint: Color
    get() = if (this == DarkColors) DangerSurfaceDark else DangerSurface

val ColorScheme.onDangerTint: Color
    get() = if (this == DarkColors) DangerRedDark else DangerRed

val ColorScheme.neutralTint: Color
    get() = if (this == DarkColors) DarkSurfaceVariant else SurfaceVariant

val ColorScheme.onNeutralTint: Color
    get() = if (this == DarkColors) DarkInk700 else Ink700

val ColorScheme.ink700: Color
    get() = if (this == DarkColors) DarkInk700 else Ink700

val ColorScheme.ink500: Color
    get() = if (this == DarkColors) DarkInk500 else Ink500

val ColorScheme.ink300: Color
    get() = if (this == DarkColors) DarkInk300 else Ink300

val ColorScheme.ink100: Color
    get() = if (this == DarkColors) DarkInk100 else Ink100

fun severityFromString(value: String): SoundSeverity = when (value.trim().lowercase()) {
    "high" -> SoundSeverity.High
    "medium" -> SoundSeverity.Medium
    "low" -> SoundSeverity.Low
    else -> SoundSeverity.None
}

fun severityTone(severity: SoundSeverity): IncidentStatusTone = when (severity) {
    SoundSeverity.High -> IncidentStatusTone.Danger
    SoundSeverity.Medium -> IncidentStatusTone.Warning
    SoundSeverity.Low,
    SoundSeverity.None -> IncidentStatusTone.Neutral
}

fun severityChipLabel(severity: SoundSeverity): String = when (severity) {
    SoundSeverity.High -> "HIGH RISK"
    SoundSeverity.Medium -> "MEDIUM RISK"
    SoundSeverity.Low -> "LOW"
    SoundSeverity.None -> "MONITORING"
}

// Helper functions for incident status
fun formatIncidentLabel(status: String): String = when (status.lowercase()) {
    "waiting_user", "waitinguser", "detected" -> "Awaiting response"
    "caregiver_notified", "caregivernotified" -> "Caregiver notified"
    "escalated" -> "Escalated"
    "caregiver_acknowledged", "acknowledged" -> "Acknowledged"
    "resolved" -> "Resolved"
    "false_alarm", "falsealarm" -> "False alarm"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
