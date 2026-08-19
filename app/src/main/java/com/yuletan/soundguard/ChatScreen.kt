package com.yuletan.soundguard

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                androidx.compose.material3.Icon(Icons.Outlined.ArrowBack, contentDescription = "Go back")
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    partnerName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    partnerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    partnerPhone.ifBlank { "Connected" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isCaregiverView) {
                OutlinedButton(onClick = onRequestPhoto) {
                    androidx.compose.material3.Icon(Icons.Outlined.CameraAlt, contentDescription = "Request photo")
                }
                Spacer(Modifier.width(6.dp))
            }
            OutlinedButton(onClick = onCall) {
                androidx.compose.material3.Icon(Icons.Outlined.Call, contentDescription = "Call")
            }
        }

        HorizontalDivider()

        if (loading && messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No messages yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Incidents and alerts will appear here as chat messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { "${it::class.simpleName}_${it.timestamp}" }) { message ->
                    when (message) {
                        is ChatMessage.Incident -> IncidentBubble(
                            message = message,
                            onPhotoClick = { showPhotoDetail = message.snapshotUrl },
                        )
                        is ChatMessage.PhotoRequest -> PhotoRequestBubble(
                            message = message,
                            onPhotoClick = { showPhotoDetail = message.photoUrl },
                        )
                        is ChatMessage.Acknowledgment -> AcknowledgmentBubble(message = message)
                    }
                }
            }
        }

        if (snapshotMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (snapshotMessage.contains("failed", ignoreCase = true))
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            ) {
                Text(
                    snapshotMessage,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Call", fontSize = 13.sp) }
                }
                if (isCaregiverView) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRequestPhoto,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Request Photo", fontSize = 13.sp) }
                    }
                }
            }
        }
    }

    if (showPhotoDetail != null) {
        AlertDialog(
            onDismissRequest = { showPhotoDetail = null },
            title = { Text("Verification photo") },
            text = {
                showPhotoDetail?.let { url ->
                    val context = LocalContext.current
                    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(url) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                connection.connectTimeout = 10_000
                                connection.readTimeout = 15_000
                                val tmpFile = java.io.File(context.cacheDir, "chat_photo_${System.currentTimeMillis()}.jpg")
                                connection.inputStream.use { input -> tmpFile.outputStream().use { output -> input.copyTo(output) } }
                                bitmap = BitmapFactory.decodeFile(tmpFile.absolutePath)
                                tmpFile.delete()
                            } catch (_: Exception) {}
                        }
                    }
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Verification photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                        )
                    } ?: Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoDetail = null }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun IncidentBubble(
    message: ChatMessage.Incident,
    onPhotoClick: () -> Unit,
) {
    val isHigh = message.severity == "high"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            colors = CardDefaults.cardColors(
                containerColor = if (isHigh) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        message.label,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    StatusChip(statusLabel(message.status), statusTone(message.status))
                }
                Spacer(Modifier.height(4.dp))
                ConfidenceBar(message.confidence, "${message.label} · ${severityLabel(message.severity)}")
                if (message.snapshotUrl != null) {
                    Spacer(Modifier.height(8.dp))
                    val context = LocalContext.current
                    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(message.snapshotUrl) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val connection = java.net.URL(message.snapshotUrl).openConnection() as java.net.HttpURLConnection
                                connection.connectTimeout = 10_000
                                connection.readTimeout = 15_000
                                val tmpFile = java.io.File(context.cacheDir, "bubble_${message.id}.jpg")
                                connection.inputStream.use { input -> tmpFile.outputStream().use { output -> input.copyTo(output) } }
                                bitmap = BitmapFactory.decodeFile(tmpFile.absolutePath)
                                tmpFile.delete()
                            } catch (_: Exception) {}
                        }
                    }
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Verification photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable(onClick = onPhotoClick),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    formatChatBubbleTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status.lowercase(Locale.ROOT)) {
    "waiting_user", "detected" -> "Waiting for response"
    "caregiver_notified" -> "Caregiver notified"
    "escalated" -> "Escalated"
    "caregiver_acknowledged", "acknowledged" -> "Acknowledged"
    "resolved" -> "Resolved"
    "false_alarm" -> "False alarm"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun statusTone(status: String): IncidentStatusTone = when (statusLabel(status)) {
    "Resolved", "Acknowledged" -> IncidentStatusTone.Success
    "Escalated" -> IncidentStatusTone.Danger
    "False alarm" -> IncidentStatusTone.Neutral
    else -> IncidentStatusTone.Warning
}

private fun severityLabel(severity: String): String = severity.replaceFirstChar { it.uppercase() }

@Composable
private fun PhotoRequestBubble(message: ChatMessage.PhotoRequest, onPhotoClick: () -> Unit) {
    val statusText = when (message.status) {
        "requested" -> "Photo requested — waiting for approval"
        "approved" -> "Photo approved — capturing..."
        "declined" -> "Photo request declined"
        "uploaded" -> "Verification photo captured"
        else -> "Photo ${message.status}"
    }
    val statusColor = when (message.status) {
        "declined" -> MaterialTheme.colorScheme.error
        "uploaded", "approved" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val hasPhoto = message.photoUrl != null && (message.status == "uploaded" || message.status == "approved")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (hasPhoto) Arrangement.Start else Arrangement.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = if (hasPhoto) RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp) else RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
                if (hasPhoto) {
                    Spacer(Modifier.height(6.dp))
                    val context = LocalContext.current
                    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(message.photoUrl) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val connection = java.net.URL(message.photoUrl!!).openConnection() as java.net.HttpURLConnection
                                connection.connectTimeout = 10_000
                                connection.readTimeout = 15_000
                                val tmpFile = java.io.File(context.cacheDir, "bubble_photo_${System.currentTimeMillis()}.jpg")
                                connection.inputStream.use { input -> tmpFile.outputStream().use { output -> input.copyTo(output) } }
                                val bmp = BitmapFactory.decodeFile(tmpFile.absolutePath)
                                tmpFile.delete()
                                bitmap = bmp
                            } catch (_: Exception) {}
                        }
                    }
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Verification photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clickable(onClick = onPhotoClick),
                        )
                        TextButton(
                            onClick = onPhotoClick,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("View full photo", style = MaterialTheme.typography.labelSmall)
                        }
                    } ?: CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 8.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    formatChatBubbleTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun AcknowledgmentBubble(message: ChatMessage.Acknowledgment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "✓ ${message.byName} acknowledged the alert",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatChatBubbleTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun formatChatBubbleTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}
