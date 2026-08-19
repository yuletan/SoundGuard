package com.soundguard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SoundGuard palette — monochrome-first, with a small, strictly-reserved
 * semantic set for incident severity.
 *
 * Rule of thumb: if a color isn't describing a real incident state
 * (via IncidentStatus below), it should be one of the Ink/Surface tokens.
 * Never use Danger/Warning/Success decoratively, for branding, or on
 * test/demo buttons — that's what makes them mean something when they
 * do show up.
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
val WarningAmber = Color(0xFFA15C00)
val WarningSurface = Color(0xFFFFF4E1)
val SuccessGreen = Color(0xFF2E7D32)
val SuccessSurface = Color(0xFFEAF5EA)


/**
 * Mirrors your real state machine:
 * Detected -> WaitingUser -> (2min) CaregiverNotified -> (2min) Escalated
 * -> Acknowledged / Resolved / FalseAlarm
 *
 * Rename the cases to match your actual enum. The point is: nothing in
 * the UI layer should ever render `incident.status.name` directly (that's
 * where "waiting_user" was leaking into the chat) — it should always go
 * through .label() and .textColor()/.surfaceColor() below.
 */
enum class IncidentStatus {
    WAITING_USER,
    CAREGIVER_NOTIFIED,
    ESCALATED,
    ACKNOWLEDGED,
    RESOLVED,
    FALSE_ALARM
}

fun IncidentStatus.label(): String = when (this) {
    IncidentStatus.WAITING_USER -> "Awaiting your response"
    IncidentStatus.CAREGIVER_NOTIFIED -> "Caregiver notified"
    IncidentStatus.ESCALATED -> "Escalated"
    IncidentStatus.ACKNOWLEDGED -> "Acknowledged"
    IncidentStatus.RESOLVED -> "Resolved"
    IncidentStatus.FALSE_ALARM -> "False alarm"
}

fun IncidentStatus.textColor(): Color = when (this) {
    IncidentStatus.WAITING_USER,
    IncidentStatus.CAREGIVER_NOTIFIED -> WarningAmber
    IncidentStatus.ESCALATED -> DangerRed
    IncidentStatus.ACKNOWLEDGED,
    IncidentStatus.RESOLVED -> SuccessGreen
    IncidentStatus.FALSE_ALARM -> Ink500
}

fun IncidentStatus.surfaceColor(): Color = when (this) {
    IncidentStatus.WAITING_USER,
    IncidentStatus.CAREGIVER_NOTIFIED -> WarningSurface
    IncidentStatus.ESCALATED -> DangerSurface
    IncidentStatus.ACKNOWLEDGED,
    IncidentStatus.RESOLVED -> SuccessSurface
    IncidentStatus.FALSE_ALARM -> SurfaceVariant
}

/**
 * Example usage in a chat bubble composable:
 *
 * AssistChip(
 *     onClick = {},
 *     label = { Text(incident.status.label()) },
 *     colors = AssistChipDefaults.assistChipColors(
 *         containerColor = incident.status.surfaceColor(),
 *         labelColor = incident.status.textColor()
 *     )
 * )
 */
