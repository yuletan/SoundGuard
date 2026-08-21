package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatPreview(
    val partnerId: String,
    val partnerName: String,
    val partnerPhone: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val connectionId: String = "",
    val deactivated: Boolean = false,
)

class ChatRepository(context: Context) {
    private val notificationClient = NotificationClient(context)
    private val incidentClient = IncidentClient(context)
    private val snapshotClient = SnapshotClient(context)
    private val careClient = CareClient(context)

    private val HIDDEN_SEVERITIES = setOf("low")

    private fun isHiddenSeverity(value: String) = value.trim().lowercase() in HIDDEN_SEVERITIES

    suspend fun buildChatMessages(partnerId: String, isCaregiverView: Boolean): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val messages = mutableListOf<ChatMessage>()

            if (isCaregiverView) {
                val notifications = notificationClient.fetchForConnection(partnerId).getOrNull().orEmpty()
                    .filter { !isHiddenSeverity(it.severity) }
                val incidents = incidentClient.fetchIncidentsForBeneficiary(partnerId).getOrNull().orEmpty()
                    .filter { !isHiddenSeverity(it.severity) }

                val primary = if (incidents.isNotEmpty()) {
                    incidents.map { inc ->
                        val ts = parseIsoTimestamp(inc.startedAt) ?: Long.MAX_VALUE
                        ChatMessage.Incident(
                            id = inc.id,
                            label = inc.label.ifBlank { "Alert" },
                            severity = inc.severity,
                            confidence = inc.confidence,
                            status = inc.status,
                            timestamp = ts,
                        )
                    }.filter { it.timestamp != Long.MAX_VALUE }
                } else {
                    notifications.mapNotNull { n ->
                        val ts = parseIsoTimestamp(n.incidentStartedAt)
                            ?: parseIsoTimestamp(n.createdAt)
                            ?: return@mapNotNull null
                        ChatMessage.Incident(
                            id = n.incidentId,
                            label = n.soundLabel.ifBlank { "Alert" },
                            severity = n.severity,
                            confidence = n.confidence,
                            status = n.status,
                            timestamp = ts,
                        )
                    }
                }

                messages.addAll(primary)

                for (n in notifications) {
                    if (n.status == "acknowledged") {
                        val ts = parseIsoTimestamp(n.incidentStartedAt)
                            ?: parseIsoTimestamp(n.createdAt)
                            ?: continue
                        messages.add(
                            ChatMessage.Acknowledgment(timestamp = ts + 1000, byName = "Caregiver")
                        )
                    }
                }

                val incidentIds = (primary.map { (it as ChatMessage.Incident).id } + notifications.map { it.incidentId }).distinct()
                appendSnapshots(messages, incidentIds)
            } else {
                val incidents = incidentClient.fetchOwnIncidents().getOrNull().orEmpty()
                    .filter { !isHiddenSeverity(it.severity) }
                for (incident in incidents) {
                    val ts = parseIsoTimestamp(incident.startedAt) ?: continue
                    messages.add(
                        ChatMessage.Incident(
                            id = incident.id,
                            label = incident.label,
                            severity = incident.severity,
                            confidence = incident.confidence,
                            status = incident.status,
                            timestamp = ts,
                        )
                    )
                }

                appendSnapshots(messages, incidents.map { it.id }.distinct())
            }

            messages.sortedBy { it.timestamp }
        }
    }

    private suspend fun appendSnapshots(messages: MutableList<ChatMessage>, incidentIds: List<String>) {
        for (incidentId in incidentIds) {
            val allSnapshots = snapshotClient.fetchSnapshotsForIncident(incidentId)
                .getOrNull().orEmpty()
            if (allSnapshots.isEmpty()) continue
            val now = System.currentTimeMillis()
            val activeSnapshots = allSnapshots.filter { snapshot ->
                snapshot.uploadStatus != "expired" &&
                    (snapshot.expiresAt?.let { parseIsoTimestamp(it) } ?: Long.MAX_VALUE) > now
            }
            val expiredSnapshots = allSnapshots.filter { snapshot ->
                snapshot.uploadStatus == "expired" ||
                    (snapshot.expiresAt?.let { parseIsoTimestamp(it) } ?: Long.MAX_VALUE) <= now
            }
            val bestSnapshot = pickBestSnapshot(activeSnapshots)
                ?: pickBestSnapshot(expiredSnapshots)
                ?: pickBestSnapshot(allSnapshots)
                ?: continue
            val requestedTs = bestSnapshot.requestedAt?.let { parseIsoTimestamp(it) }
            val expiresTs = bestSnapshot.expiresAt?.let { parseIsoTimestamp(it) }
            val snapshotTs = requestedTs ?: expiresTs?.minus(600_000) ?: System.currentTimeMillis()
            val isExpired = bestSnapshot.uploadStatus == "expired" ||
                (expiresTs != null && expiresTs <= now)
            val displayStatus = when {
                isExpired -> "expired"
                bestSnapshot.uploadStatus == "uploaded" -> "uploaded"
                bestSnapshot.approvalStatus == "approved" -> "approved"
                bestSnapshot.approvalStatus == "declined" -> "declined"
                bestSnapshot.uploadStatus == "failed" -> "failed"
                else -> bestSnapshot.uploadStatus ?: bestSnapshot.approvalStatus ?: "requested"
            }
            messages.add(
                ChatMessage.PhotoRequest(
                    incidentId = incidentId,
                    status = displayStatus,
                    timestamp = snapshotTs,
                    photoUrl = if (isExpired) null else bestSnapshot.signedUrl,
                    expiresAt = expiresTs,
                )
            )
        }
    }

    suspend fun buildChatListForCaregiver(): Result<List<ChatPreview>> = withContext(Dispatchers.IO) {
        runCatching {
            val beneficiaries = careClient.fetchBeneficiariesForCaregiver().getOrNull().orEmpty()
            val previews = mutableListOf<ChatPreview>()

            for (beneficiary in beneficiaries) {
                val notifications = notificationClient.fetchForConnection(beneficiary.beneficiaryId).getOrNull().orEmpty()
                    .filter { !isHiddenSeverity(it.severity) }
                val incidents = incidentClient.fetchIncidentsForBeneficiary(beneficiary.beneficiaryId).getOrNull().orEmpty()
                    .filter { !isHiddenSeverity(it.severity) }

                val lastNotification = notifications.lastOrNull()
                val lastIncident = incidents.maxByOrNull { parseIsoTimestamp(it.startedAt) ?: 0L }
                val notifTs = lastNotification?.let { parseIsoTimestamp(it.createdAt) } ?: 0L
                val incidentTs = lastIncident?.let { parseIsoTimestamp(it.startedAt) } ?: 0L
                val useIncident = incidentTs >= notifTs && lastIncident != null
                val ts = when {
                    useIncident -> incidentTs
                    lastNotification != null -> notifTs
                    else -> System.currentTimeMillis()
                }
                val unread = maxOf(
                    notifications.count { it.status != "acknowledged" },
                    if (useIncident && lastIncident != null) 1 else 0
                )
                val lastMsg = when {
                    useIncident && lastIncident != null -> "${lastIncident.label.ifBlank { "Alert" }} • ${lastIncident.severity}"
                    lastNotification != null -> "${lastNotification.soundLabel.ifBlank { "Alert" }} • ${lastNotification.severity}"
                    else -> "No incidents yet"
                }
                previews.add(
                    ChatPreview(
                        partnerId = beneficiary.beneficiaryId,
                        partnerName = beneficiary.name,
                        partnerPhone = beneficiary.phone,
                        lastMessage = lastMsg,
                        lastTimestamp = ts,
                        unreadCount = unread,
                        connectionId = beneficiary.connectionId,
                        deactivated = beneficiary.deactivated,
                    )
                )
            }

            previews.sortedByDescending { it.lastTimestamp }
        }
    }

    suspend fun buildChatListForBeneficiary(): Result<List<ChatPreview>> = withContext(Dispatchers.IO) {
        runCatching {
            val caregivers = careClient.fetchCaregiversForBeneficiary().getOrNull().orEmpty()
            val previews = mutableListOf<ChatPreview>()

            for (caregiver in caregivers) {
                val notifications = notificationClient.fetchForBeneficiary().getOrNull().orEmpty()
                val caregiverNotifications = notifications.filter { true }
                val lastNotification = caregiverNotifications.lastOrNull()
                val ts = if (lastNotification != null) parseIsoTimestamp(lastNotification.createdAt) ?: System.currentTimeMillis() else System.currentTimeMillis()
                val unread = caregiverNotifications.count { it.status != "acknowledged" }
                val lastMsg = if (lastNotification != null) {
                    "${lastNotification.soundLabel.ifBlank { "Alert" }} • ${lastNotification.severity}"
                } else {
                    "No incidents yet"
                }
                previews.add(
                    ChatPreview(
                        partnerId = caregiver.caregiverId,
                        partnerName = caregiver.name,
                        partnerPhone = caregiver.phone,
                        lastMessage = lastMsg,
                        lastTimestamp = ts,
                        unreadCount = unread,
                        connectionId = caregiver.connectionId,
                        deactivated = caregiver.deactivated,
                    )
                )
            }

            previews.sortedByDescending { it.lastTimestamp }
        }
    }

    private fun pickBestSnapshot(snapshots: List<SnapshotClient.SnapshotWithUrl>): SnapshotClient.SnapshotWithUrl? {
        if (snapshots.isEmpty()) return null
        val statusRank = mapOf(
            "uploaded" to 3,
            "viewed" to 2,
            "approved" to 2,
            "requested" to 1,
            "pending" to 1,
            "declined" to 0,
            "failed" to 0,
            "expired" to 0,
        )
        return snapshots.maxByOrNull { snapshot ->
            val uploadRank = statusRank[snapshot.uploadStatus] ?: 0
            val approvalRank = statusRank[snapshot.approvalStatus] ?: 0
            uploadRank * 10 + approvalRank
        }
    }

    private fun parseIsoTimestamp(iso: String): Long? {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
