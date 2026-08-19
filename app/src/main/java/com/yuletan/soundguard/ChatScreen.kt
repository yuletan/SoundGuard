package com.yuletan.soundguard

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    partnerName: String,
    partnerPhone: String,
    messages: List<ChatMessage>,
    loading: Boolean,
    isCaregiverView: Boolean,
    snapshotMessage: String?,
    activeIncidentBannerText: String? = null,
    onRequestPhoto: () -> Unit,
    onCall: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    var showPhotoDetail by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            onRefresh()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Go back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(4.dp))
            AvatarCircle(text = partnerName.ifBlank { "User" }, sizeDp = 34)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = partnerName.ifBlank { "Chat" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (partnerPhone.isNotBlank()) partnerPhone else "Active recently",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
            }

            if (isCaregiverView) {
                IconButton(onClick = onRequestPhoto, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Request photo", tint = MaterialTheme.colorScheme.ink700)
                }
            }
            IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.ink700)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        // --- ACTIVE INCIDENT BANNER ---
        if (activeIncidentBannerText != null) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                IncidentBanner(text = activeIncidentBannerText)
            }
        }

        // --- CHAT CONTENT LIST ---
        if (loading && messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "No incidents yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.ink500
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Detected sounds and safety alerts will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.ink500,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                userScrollEnabled = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { "${it::class.simpleName}_${it.timestamp}" }) { message ->
                    when (message) {
                        is ChatMessage.Incident -> IncidentBubble(
                            message = message,
                            isCaregiverView = isCaregiverView,
                            onPhotoClick = { showPhotoDetail = message.snapshotUrl },
                        )
                        is ChatMessage.PhotoRequest -> PhotoRequestBubble(
                            message = message,
                            isCaregiverView = isCaregiverView,
                            onPhotoClick = { showPhotoDetail = message.photoUrl },
                        )
                        is ChatMessage.Acknowledgment -> AcknowledgmentBubble(message = message)
                    }
                }
            }
        }

        // Optional Snapshot Status Message
        if (snapshotMessage != null) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(
                    snapshotMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (snapshotMessage.contains("failed", ignoreCase = true))
                                MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (snapshotMessage.contains("failed", ignoreCase = true))
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- BOTTOM ACTION BAR ---
        if (isCaregiverView) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        Text("Call", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onRequestPhoto,
                        modifier = Modifier.weight(1.3f).height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("Request photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        Text("Call", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showPhotoDetail != null) {
        FullScreenPhotoDialog(url = showPhotoDetail!!, onDismiss = { showPhotoDetail = null })
    }
}

@Composable
private fun FullScreenPhotoDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .systemBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
            var failed by remember(url) { mutableStateOf(false) }
            LaunchedEffect(url) {
                val decoded = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 10_000
                        connection.readTimeout = 15_000
                        connection.instanceFollowRedirects = true
                        connection.inputStream.use(BitmapFactory::decodeStream)
                    }.getOrNull()
                }
                bitmap = decoded
                failed = decoded == null
            }
            when {
                bitmap != null -> Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Verification photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                failed -> Text(
                    text = "Photo no longer available",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun IncidentBubble(
    message: ChatMessage.Incident,
    isCaregiverView: Boolean,
    onPhotoClick: () -> Unit,
) {
    val isSent = isCaregiverView
    val isDark = LocalDarkTheme.current

    val containerColor: Color
    val textColor: Color
    val subTextColor: Color

    if (isSent) {
        containerColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0EE)
        textColor = if (isDark) Color(0xFFF5F5F4) else Color(0xFF111111)
        subTextColor = if (isDark) Color(0xFF9E9E9E) else Color(0xFF767676)
    } else {
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
        textColor = if (isDark) Color(0xFFF5F5F4) else Color(0xFF111111)
        subTextColor = if (isDark) Color(0xFF9E9E9E) else Color(0xFF767676)
    }

    val borderColor = if (!isSent) {
        if (isDark) Color(0xFF333333) else Color(0xFFE0E0DE)
    } else Color.Transparent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(messageBubbleShape(isSent))
                .background(containerColor)
                .then(
                    if (!isSent) Modifier.border(1.dp, borderColor, messageBubbleShape(isSent))
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = formatIncidentLabel(message.status).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                    color = subTextColor,
                )
                if (message.label.contains("simulated", ignoreCase = true) ||
                    message.label.contains("test", ignoreCase = true) ||
                    message.id.contains("sim", ignoreCase = true)
                ) {
                    Box(
                        modifier = Modifier
                            .background(subTextColor.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "TEST",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            color = subTextColor,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatChatBubbleTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = subTextColor,
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = message.label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            Text(
                text = "Confidence ${(message.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = subTextColor,
            )

            if (message.snapshotUrl != null) {
                Spacer(Modifier.height(8.dp))
                RemotePhotoImage(
                    url = message.snapshotUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onPhotoClick),
                )
            }
        }
    }
}

private fun messageBubbleShape(isSent: Boolean): RoundedCornerShape =
    if (isSent) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)

@Composable
private fun PhotoRequestBubble(
    message: ChatMessage.PhotoRequest,
    isCaregiverView: Boolean,
    onPhotoClick: () -> Unit,
) {
    val isSent = isCaregiverView
    val isDark = LocalDarkTheme.current

    val containerColor: Color
    val textColor: Color
    val subTextColor: Color

    if (isSent) {
        containerColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0EE)
        textColor = if (isDark) Color(0xFFF5F5F4) else Color(0xFF111111)
        subTextColor = if (isDark) Color(0xFF9E9E9E) else Color(0xFF767676)
    } else {
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
        textColor = if (isDark) Color(0xFFF5F5F4) else Color(0xFF111111)
        subTextColor = if (isDark) Color(0xFF9E9E9E) else Color(0xFF767676)
    }

    val borderColor = if (!isSent) {
        if (isDark) Color(0xFF333333) else Color(0xFFE0E0DE)
    } else Color.Transparent

    val expiresAt = message.expiresAt
    var now by remember(expiresAt) { mutableStateOf(System.currentTimeMillis()) }
    if (expiresAt != null && !message.photoUrl.isNullOrBlank() && message.status != "expired") {
        LaunchedEffect(expiresAt) {
            while (true) {
                now = System.currentTimeMillis()
                if (expiresAt <= now) break
                delay(1_000L)
            }
        }
    }
    val remainingMs = if (expiresAt != null) (expiresAt - now).coerceAtLeast(0L) else null
    val isExpired = message.status.equals("expired", ignoreCase = true) ||
        (expiresAt != null && expiresAt <= now) ||
        (message.photoUrl.isNullOrBlank() && message.status.equals("expired", ignoreCase = true))
    val hasPhoto = !message.photoUrl.isNullOrBlank() && !isExpired

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
    ) {
        when {
            isExpired -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .clip(messageBubbleShape(isSent))
                        .background(containerColor)
                        .border(1.dp, subTextColor.copy(alpha = 0.18f), messageBubbleShape(isSent))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(subTextColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.HideImage, contentDescription = null, tint = subTextColor, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = "Photo expired",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "This photo was automatically deleted after 10 minutes for privacy. It was visible to both caregiver and beneficiary until expiry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = subTextColor,
                        lineHeight = 15.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formatChatBubbleTime(message.timestamp) + " \u00B7 deleted",
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor.copy(alpha = 0.7f),
                    )
                }
            }
            hasPhoto -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .clip(messageBubbleShape(isSent))
                        .clickable(onClick = onPhotoClick),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(236.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        RemotePhotoImage(
                            url = message.photoUrl!!,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = if (remainingMs != null && remainingMs > 0) formatEphemeralCountdown(remainingMs) else "deleting…",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                    )
                                }
                                Text(
                                    text = formatChatBubbleTime(message.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        if (remainingMs != null && remainingMs > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF4ADE80), RoundedCornerShape(999.dp)))
                                    Text(
                                        text = "Visible to both \u00B7 auto-deletes",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(containerColor)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Image, contentDescription = null, tint = subTextColor, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Photo \u00B7 tap to view",
                                style = MaterialTheme.typography.labelSmall,
                                color = subTextColor,
                            )
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .clip(messageBubbleShape(isSent))
                        .background(containerColor)
                        .then(
                            if (!isSent) Modifier.border(1.dp, borderColor, messageBubbleShape(isSent))
                            else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Photo request",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                    Text(
                        text = photoRequestStatusText(message.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = subTextColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatChatBubbleTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

private fun formatEphemeralCountdown(remainingMs: Long): String {
    val totalSec = (remainingMs / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) String.format("%d:%02d left", m, s) else String.format("0:%02d left", s)
}

private fun photoRequestStatusText(status: String): String = when (status.lowercase()) {
    "requested", "pending" -> "Waiting for approval"
    "approved" -> "Approved \u2014 capturing photo"
    "declined" -> "Declined"
    "uploaded" -> "Photo uploaded"
    "expired" -> "Expired"
    else -> status.replaceFirstChar { it.uppercase() }
}

@Composable
private fun RemotePhotoImage(url: String, modifier: Modifier) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        val decoded = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = true
                connection.inputStream.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
        bitmap = decoded
        failed = decoded == null
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Verification photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            failed -> Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = "Photo unavailable",
                tint = MaterialTheme.colorScheme.ink500,
                modifier = Modifier.size(24.dp),
            )
            else -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.ink500,
            )
        }
    }
}

@Composable
private fun AcknowledgmentBubble(message: ChatMessage.Acknowledgment) {
    val isDark = LocalDarkTheme.current
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F4)
    val textColor = if (isDark) Color(0xFF9E9E9E) else Color(0xFF767676)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "\u2713 ${message.byName} acknowledged the alert",
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "\u00B7 ${formatChatBubbleTime(message.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
            )
        }
    }
}

private fun formatChatBubbleTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}
