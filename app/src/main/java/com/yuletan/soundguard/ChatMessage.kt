package com.yuletan.soundguard

sealed class ChatMessage {
    abstract val timestamp: Long

    data class Incident(
        val id: String,
        val label: String,
        val severity: String,
        val confidence: Float,
        val status: String,
        override val timestamp: Long,
        val snapshotUrl: String? = null,
    ) : ChatMessage()

    data class PhotoRequest(
        val incidentId: String,
        val status: String,
        override val timestamp: Long,
        val photoUrl: String? = null,
    ) : ChatMessage()

    data class Acknowledgment(
        override val timestamp: Long,
        val byName: String,
    ) : ChatMessage()
}
