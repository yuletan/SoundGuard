package com.yuletan.soundguard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusChip(label: String, tone: IncidentStatusTone = IncidentStatusTone.Neutral) {
    val (background, foreground) = when (tone) {
        IncidentStatusTone.Success -> MaterialTheme.colorScheme.successTint to MaterialTheme.colorScheme.onSuccessTint
        IncidentStatusTone.Warning -> MaterialTheme.colorScheme.warningTint to MaterialTheme.colorScheme.onWarningTint
        IncidentStatusTone.Danger -> MaterialTheme.colorScheme.dangerTint to MaterialTheme.colorScheme.onDangerTint
        IncidentStatusTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.background(background, RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("•", color = foreground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        Text(label, color = foreground, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 8.dp))
    }
}

enum class IncidentStatusTone { Success, Warning, Danger, Neutral }

@Composable
fun CountdownRing(remainingMs: Long, totalMs: Long, modifier: Modifier = Modifier) {
    val progress = (remainingMs.toFloat() / totalMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    androidx.compose.foundation.layout.Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(72.dp),
            color = MaterialTheme.colorScheme.danger,
            trackColor = MaterialTheme.colorScheme.outline,
            strokeWidth = 4.dp,
        )
        Text(
            "${remainingMs / 60_000}:${((remainingMs / 1_000) % 60).toString().padStart(2, '0')}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun ConfidenceBar(confidence: Float, label: String = "Confidence") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        androidx.compose.material3.LinearProgressIndicator(
            progress = { confidence.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f),
            color = if (confidence >= .75f) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.warning,
            trackColor = MaterialTheme.colorScheme.outline,
        )
        Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun OtpCodeInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isLetterOrDigit).uppercase().take(6)) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        decorationBox = { inner ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(6) { index ->
                    val focused = index == value.length.coerceAtMost(5)
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(44.dp, 52.dp)
                            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(value.getOrNull(index)?.toString() ?: "", fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            inner()
        },
    )
}
