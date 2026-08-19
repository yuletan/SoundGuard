package com.yuletan.soundguard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class IncidentStatusTone { Success, Warning, Danger, Neutral }

@Composable
fun StatusChip(
    label: String,
    tone: IncidentStatusTone = IncidentStatusTone.Neutral,
    modifier: Modifier = Modifier
) {
    val (background, foreground) = when (tone) {
        IncidentStatusTone.Success -> MaterialTheme.colorScheme.successTint to MaterialTheme.colorScheme.onSuccessTint
        IncidentStatusTone.Warning -> MaterialTheme.colorScheme.warningTint to MaterialTheme.colorScheme.onWarningTint
        IncidentStatusTone.Danger -> MaterialTheme.colorScheme.dangerTint to MaterialTheme.colorScheme.onDangerTint
        IncidentStatusTone.Neutral -> MaterialTheme.colorScheme.neutralTint to MaterialTheme.colorScheme.onNeutralTint
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun AvatarCircle(
    text: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 34,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (sizeDp * 0.42).sp,
        )
    }
}

@Composable
fun SoundGuardSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun CountdownRing(remainingMs: Long, totalMs: Long, modifier: Modifier = Modifier) {
    val progress = (remainingMs.toFloat() / totalMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.danger,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeWidth = 3.dp,
        )
        Text(
            "${remainingMs / 60_000}:${((remainingMs / 1_000) % 60).toString().padStart(2, '0')}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun ConfidenceBar(confidence: Float, label: String = "Confidence") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
        LinearProgressIndicator(
            progress = { confidence.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(4.dp),
            color = if (confidence >= .75f) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.warning,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun OtpCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isLetterOrDigit).uppercase().take(length)) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        decorationBox = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "6-CHARACTER CODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.ink500,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(length) { index ->
                        val char = value.getOrNull(index)?.toString() ?: "—"
                        Text(
                            text = char,
                            fontSize = 18.sp,
                            fontWeight = if (index < value.length) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (index < value.length) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.ink300,
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chev_rotation")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expanded = !expanded },
                    )
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.ink500,
                    modifier = Modifier.rotate(rotation).size(20.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun IncidentBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.dangerTint, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(MaterialTheme.colorScheme.danger, CircleShape)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.danger,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

enum class SoundGuardTab { Home, Chat, People, Settings }

@Composable
fun SoundGuardBottomNav(
    selectedTab: SoundGuardTab,
    onTabSelected: (SoundGuardTab) -> Unit,
    peopleTabLabel: String = "Caregivers",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                selected = selectedTab == SoundGuardTab.Home,
                onClick = { onTabSelected(SoundGuardTab.Home) }
            )
            NavItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = "Chat",
                selected = selectedTab == SoundGuardTab.Chat,
                onClick = { onTabSelected(SoundGuardTab.Chat) }
            )
            NavItem(
                icon = Icons.Outlined.People,
                label = peopleTabLabel,
                selected = selectedTab == SoundGuardTab.People,
                onClick = { onTabSelected(SoundGuardTab.People) }
            )
            NavItem(
                icon = Icons.Outlined.Tune,
                label = "Settings",
                selected = selectedTab == SoundGuardTab.Settings,
                onClick = { onTabSelected(SoundGuardTab.Settings) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.ink300
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
